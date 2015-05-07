/*
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.reactivex.netty.protocol.http.clientNew;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.junit.Rule;
import org.junit.Test;
import rx.Observable;
import rx.functions.Func1;
import rx.observers.TestSubscriber;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class HttpClientTest {

    @Rule
    public final HttpClientRule clientRule = new HttpClientRule();

    @Test(timeout = 60000)
    public void testCloseOnResponseComplete() throws Exception {
        HttpResponse nettyResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        HttpClientRequest<ByteBuf, ByteBuf> request = clientRule.getHttpClient().createGet("/");
        TestSubscriber<Void> testSubscriber = new TestSubscriber<>();

        request.flatMap(new Func1<HttpClientResponse<ByteBuf>, Observable<Void>>() {
            @Override
            public Observable<Void> call(HttpClientResponse<ByteBuf> clientResponse) {
                return clientResponse.discardContent();
            }
        }).subscribe(testSubscriber);

        clientRule.feedResponseAndComplete(nettyResponse);
        testSubscriber.requestMore(1);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();

        assertThat("Channel not closed after response completion.", clientRule.getChannel().isOpen(), is(false));
    }

}
