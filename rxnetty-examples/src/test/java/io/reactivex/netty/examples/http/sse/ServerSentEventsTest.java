/*
 * Copyright 2016 Netflix, Inc.
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
 *
 */

package io.reactivex.netty.examples.http.sse;

import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.reactivex.netty.examples.ExamplesEnvironment;
import io.reactivex.netty.examples.http.streaming.StreamingClient;
import io.reactivex.netty.protocol.http.internal.HttpMessageFormatter;
import org.junit.Test;

import java.util.Queue;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.reactivex.netty.examples.ExamplesTestUtil.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class ServerSentEventsTest extends ExamplesEnvironment {

    @Test(timeout = 60000)
    public void testSse() throws Exception {
        final Queue<String> output = setupClientLogger(StreamingClient.class);

        HelloSseClient.main(null);

        HttpResponse expectedHeader = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        expectedHeader.headers().add(TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
        expectedHeader.headers().add(CONTENT_TYPE, "text/event-stream");
        String expectedHeaderString = HttpMessageFormatter.formatResponse(expectedHeader.protocolVersion(),
                                                                          expectedHeader.status(),
                                                                          expectedHeader.headers().iteratorCharSequence());

        String[] expectedContent = new String[10];
        for (int i = 0; i < 10; i++) {
            expectedContent[i] = "data: Interval => " + i + '\n';
        }

        assertThat("Unexpected number of messages echoed", output, hasSize(expectedContent.length + 1));

        assertThat("Unexpected header", output.poll()/*Remove the header.*/, is(expectedHeaderString));

        assertThat("Unexpected content", output, contains(expectedContent));
    }
}
