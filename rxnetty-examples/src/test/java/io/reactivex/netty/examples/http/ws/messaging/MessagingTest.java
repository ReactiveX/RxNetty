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

package io.reactivex.netty.examples.http.ws.messaging;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.reactivex.netty.examples.ExamplesEnvironment;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.Queue;

import static io.reactivex.netty.examples.ExamplesTestUtil.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class MessagingTest extends ExamplesEnvironment {

    @Test(timeout = 60000)
    public void testMessaging() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        final Queue<String> output = setupClientLogger(MessagingClient.class);

        MessagingClient.main(null);

        String[] content = new String[10];
        for (int i = 0; i < 10; i++) {
            content[i] = "Received acknowledgment for message id => " + i;
        }

        assertThat("Unexpected number of messages echoed", output, hasSize(content.length + 1));

        final String headerString = output.poll();
        assertThat("Unexpected HTTP initial line of response.", headerString,
                   containsString("HTTP/1.1 101 Switching Protocols"));
        assertThat("WebSocket accept header not found in response.", headerString,
                   containsString(HttpHeaderNames.SEC_WEBSOCKET_ACCEPT + ":"));
        assertThat("Unexpected connection header.", headerString,
                   containsString(HttpHeaderNames.CONNECTION + ": " + HttpHeaderValues.UPGRADE));

        assertThat("Unexpected content", output, contains(content));
    }
}
