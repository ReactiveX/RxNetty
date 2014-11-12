/*
 * Copyright 2014 Netflix, Inc.
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
package io.reactivex.netty.protocol.http.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.reactivex.netty.protocol.http.UnicastContentSubject;
import org.junit.Assert;
import org.junit.Test;
import rx.Observable;

import java.util.concurrent.TimeUnit;

/**
 * @author Nitesh Kant
 */
public class FlatResponseOperatorTest {

    @Test
    public void testContent() throws Exception {
        DefaultHttpResponse nettyResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1,
                                                                    HttpResponseStatus.NO_CONTENT);
        UnicastContentSubject<ByteBuf> contentSubject = UnicastContentSubject.createWithoutNoSubscriptionTimeout();
        contentSubject.onNext(Unpooled.buffer());
        contentSubject.onCompleted();

        ResponseHolder<ByteBuf> holder = Observable.just(new HttpClientResponse<ByteBuf>(nettyResponse, contentSubject))
                                                   .lift(FlatResponseOperator.<ByteBuf>flatResponse())
                                                   .toBlocking().toFuture().get(1, TimeUnit.MINUTES);

        Assert.assertEquals("Unexpected http response status", HttpResponseStatus.NO_CONTENT,
                            holder.getResponse().getStatus());
        Assert.assertTrue("Response holder does not have content.", holder.hasContent());

    }

    @Test
    public void testNoContent() throws Exception {
        DefaultHttpResponse nettyResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1,
                                                                    HttpResponseStatus.NO_CONTENT);
        UnicastContentSubject<ByteBuf> contentSubject = UnicastContentSubject.createWithoutNoSubscriptionTimeout();
        contentSubject.onCompleted();

        ResponseHolder<ByteBuf> holder = Observable.just(new HttpClientResponse<ByteBuf>(nettyResponse, contentSubject))
                                                   .lift(FlatResponseOperator.<ByteBuf>flatResponse())
                                                   .toBlocking().toFuture().get(1, TimeUnit.MINUTES);
        Assert.assertEquals("Unexpected http response status", HttpResponseStatus.NO_CONTENT,
                            holder.getResponse().getStatus());
        Assert.assertFalse("Response holder has content.", holder.hasContent());

    }
}
