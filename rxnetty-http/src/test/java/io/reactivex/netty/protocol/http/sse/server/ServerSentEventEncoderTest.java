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
 *
 */

package io.reactivex.netty.protocol.http.sse.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.reactivex.netty.protocol.http.sse.ServerSentEvent;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.nio.charset.Charset;

import static io.reactivex.netty.protocol.http.sse.SseTestUtil.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class ServerSentEventEncoderTest {

    @Rule
    public final EncoderRule rule = new EncoderRule();

    @Test(timeout = 60000)
    public void testOneDataLineEncode() throws Exception {
        String eventType = "add";
        String eventId = "1";
        String data = "data line";
        ServerSentEvent event = newServerSentEvent(eventType, eventId, data);
        String expectedOutput = newSseProtocolString(eventType, eventId, data);
        rule.test(expectedOutput, event);
    }

    @Test(timeout = 60000)
    public void testMultipleDataLineEncode() throws Exception {
        ServerSentEventEncoder splitEncoder = new ServerSentEventEncoder(true);
        EmbeddedChannel channel = new EmbeddedChannel(splitEncoder);

        String eventType = "add";
        String eventId = "1";
        String data1 = "first line";
        String data2 = "second line";
        String data3 = "third line";
        String data = data1 + '\n' + data2 + '\n' + data3;
        ServerSentEvent event = newServerSentEvent(eventType, eventId, data);
        String expectedOutput = newSseProtocolString(eventType, eventId, data1, data2, data3);
        rule.test(channel, expectedOutput, event);
    }

    @Test(timeout = 60000)
    public void testNoSplitMode() throws Exception {
        String eventType = "add";
        String eventId = "1";
        String data = "first line\nsecond line\nthird line";
        ServerSentEvent event = newServerSentEvent(eventType, eventId, data);
        String expectedOutput = newSseProtocolString(eventType, eventId, data);
        rule.test(expectedOutput, event);
    }

    @Test(timeout = 60000)
    public void testEventWithNoIdEncode() throws Exception {
        String eventType = "add";
        String data = "data line";
        ServerSentEvent event = newServerSentEvent(eventType, null, data);
        String expectedOutput = newSseProtocolString(eventType, null, data);
        rule.test(expectedOutput, event);
    }

    @Test(timeout = 60000)
    public void testEventWithNoEventTypeEncode() throws Exception {
        String eventId = "1";
        String data = "data line";
        ServerSentEvent event = newServerSentEvent(null, eventId, data);
        String expectedOutput = newSseProtocolString(null, eventId, data);
        rule.test(expectedOutput, event);
    }

    @Test(timeout = 60000)
    public void testEventWithDataOnlyEncode() throws Exception {
        String data = "data line";
        ServerSentEvent event = newServerSentEvent(null, null, data);
        String expectedOutput = newSseProtocolString(null, null, data);
        rule.test(expectedOutput, event);
    }

    public static class EncoderRule extends ExternalResource {

        private ServerSentEventEncoder encoder;
        private EmbeddedChannel channel;

        @Override
        public Statement apply(final Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    encoder = new ServerSentEventEncoder();
                    channel = new EmbeddedChannel(encoder);
                    base.evaluate();
                }
            };
        }

        public void test(String expectedOutput, ServerSentEvent... toEncode) {
            test(channel, expectedOutput, toEncode);
        }

        public void test(EmbeddedChannel channel, String expectedOutput, ServerSentEvent... toEncode) {

            for (ServerSentEvent event : toEncode) {
                channel.writeAndFlush(event);
            }

            final ByteBuf allOut = Unpooled.buffer();
            ByteBuf anOut;
            while ((anOut = channel.readOutbound()) != null) {
                allOut.writeBytes(anOut);
            }

            assertThat("Unexpected encoder output", allOut.toString(Charset.defaultCharset()), equalTo(expectedOutput));
        }
    }
}