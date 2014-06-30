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

package io.reactivex.netty.protocol.text.sse;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.reactivex.netty.NoOpChannelHandlerContext;
import org.junit.Test;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Tomasz Bak
 */
public class ServerSentEventDecoderTest {

    private final TestableServerSentEventDecoder decoder = new TestableServerSentEventDecoder();

    private final ChannelHandlerContext ch = new NoOpChannelHandlerContext();

    private final ByteBufAllocator alloc = new UnpooledByteBufAllocator(false);

    static class TestableServerSentEventDecoder extends ServerSentEventDecoder {
        @Override
        public void callDecode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
            super.callDecode(ctx, in, out);
        }
    }

    @Test
    public void testOneDataLineDecode() throws Exception {
        doTest(
                "event: add\ndata: data line\nid: 1\n\n",
                new ServerSentEvent("1", "add", "data line")
        );
    }

    @Test
    public void testMultipleDataLineDecode() throws Exception {
        doTest(
                "event: add\ndata: data line 1\ndata: data line 2\nid: 1\n\n",
                new ServerSentEvent("1", "add", "data line 1\ndata line 2")
        );
    }

    @Test
    public void testEventWithNoIdDecode() throws Exception {
        doTest(
                "event: add\ndata: test data\n\n",
                new ServerSentEvent(null, "add", "test data")
        );
    }

    @Test
    public void testEventWithNoEventTypeDencode() throws Exception {
        doTest(
                "data: test data\nid: 1\n\n",
                new ServerSentEvent("1", null, "test data")
        );
    }

    @Test
    public void testEventWithDataOnlyDecode() throws Exception {
        doTest(
                "data: test data\n\n",
                new ServerSentEvent(null, null, "test data")
        );
    }


    private void doTest(String eventText, ServerSentEvent expected) {
        List<Object> out = new ArrayList<Object>();
        decoder.callDecode(ch, toByteBuf(eventText), out);

        assertEquals(1, out.size());
        ServerSentEvent event = (ServerSentEvent) out.get(0);
        assertEquals(expected.getEventId(), event.getEventId());
        assertEquals(expected.getEventType(), event.getEventType());
        assertEquals(expected.getEventData(), event.getEventData());
    }

    private ByteBuf toByteBuf(String event) {
        ByteBuf in = alloc.buffer(1024, 1024);
        in.writeBytes(event.getBytes(Charset.defaultCharset()));
        return in;
    }
}