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

package io.reactivex.netty.protocol.http.sse;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.reactivex.netty.NoOpChannelHandlerContext;
import org.junit.Test;

import java.nio.charset.Charset;

import static io.reactivex.netty.protocol.http.sse.SseTestUtil.newServerSentEvent;
import static io.reactivex.netty.protocol.http.sse.SseTestUtil.newSseProtocolString;
import static org.junit.Assert.assertEquals;

/**
 * @author Tomasz Bak
 */
public class ServerSentEventEncoderTest {

    private final ServerSentEventEncoder encoder = new ServerSentEventEncoder();

    private final ChannelHandlerContext ch = new NoOpChannelHandlerContext();

    @Test
    public void testOneDataLineEncode() throws Exception {
        String eventType = "add";
        String eventId = "1";
        String data = "data line";
        ServerSentEvent event = newServerSentEvent(eventType, eventId, data);
        String expectedOutput = newSseProtocolString(eventType, eventId, data);
        doTest(expectedOutput, event);
    }

    @Test
    public void testMultipleDataLineEncode() throws Exception {
        ServerSentEventEncoder splitEncoder = new ServerSentEventEncoder(true);
        String eventType = "add";
        String eventId = "1";
        String data1 = "first line";
        String data2 = "second line";
        String data3 = "third line";
        String data = data1 + '\n' + data2 + '\n' + data3;
        ServerSentEvent event = newServerSentEvent(eventType, eventId, data);
        String expectedOutput = newSseProtocolString(eventType, eventId, data1, data2, data3);
        doTest(splitEncoder, expectedOutput, event);
    }

    @Test
    public void testNoSplitMode() throws Exception {
        String eventType = "add";
        String eventId = "1";
        String data = "first line\nsecond line\nthird line";
        ServerSentEvent event = newServerSentEvent(eventType, eventId, data);
        String expectedOutput = newSseProtocolString(eventType, eventId, data);
        doTest(expectedOutput, event);
    }

    @Test
    public void testEventWithNoIdEncode() throws Exception {
        String eventType = "add";
        String data = "data line";
        ServerSentEvent event = newServerSentEvent(eventType, null, data);
        String expectedOutput = newSseProtocolString(eventType, null, data);
        doTest(expectedOutput, event);
    }

    @Test
    public void testEventWithNoEventTypeEncode() throws Exception {
        String eventId = "1";
        String data = "data line";
        ServerSentEvent event = newServerSentEvent(null, eventId, data);
        String expectedOutput = newSseProtocolString(null, eventId, data);
        doTest(expectedOutput, event);
    }

    @Test
    public void testEventWithDataOnlyEncode() throws Exception {
        String data = "data line";
        ServerSentEvent event = newServerSentEvent(null, null, data);
        String expectedOutput = newSseProtocolString(null, null, data);
        doTest(expectedOutput, event);
    }

    private void doTest(String expectedOutput, ServerSentEvent... toEncode) throws Exception {
        doTest(encoder, expectedOutput, toEncode);
    }

    private void doTest(ServerSentEventEncoder encoder, String expectedOutput,
                        ServerSentEvent... toEncode) throws Exception {
        ByteBuf out = Unpooled.buffer();

        for (ServerSentEvent event: toEncode) {
            encoder.encode(ch, event, out);
        }

        assertEquals("Unexpected encoder output", expectedOutput, out.toString(Charset.defaultCharset()));
    }
}