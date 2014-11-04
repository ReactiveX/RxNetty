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
import io.netty.channel.ChannelHandlerContext;
import io.reactivex.netty.NoOpChannelHandlerContext;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static io.reactivex.netty.protocol.http.sse.SseTestUtil.assertContentEquals;
import static io.reactivex.netty.protocol.http.sse.SseTestUtil.newServerSentEvent;
import static io.reactivex.netty.protocol.http.sse.SseTestUtil.newSseProtocolString;
import static io.reactivex.netty.protocol.http.sse.SseTestUtil.toByteBuf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Tomasz Bak
 */
public class ServerSentEventDecoderTest {

    private final TestableServerSentEventDecoder decoder = new TestableServerSentEventDecoder();

    private final ChannelHandlerContext ch = new NoOpChannelHandlerContext();

    static class TestableServerSentEventDecoder extends ServerSentEventDecoder {

        @Override
        public void callDecode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
            super.callDecode(ctx, in, out);
        }
    }

    @Test
    public void testOneDataLineDecode() throws Exception {
        String eventType = "add";
        String eventId = "1";
        String data = "data line";

        ServerSentEvent expected = newServerSentEvent(eventType, eventId, data);

        doTest(newSseProtocolString(eventType, eventId, data), expected);
    }

    @Test
    public void testMultipleDataLineDecode() throws Exception {
        String eventType = "add";
        String eventId = "1";
        String data1 = "data line";
        String data2 = "data line";

        ServerSentEvent expected1 = newServerSentEvent(eventType, eventId, data1);
        ServerSentEvent expected2 = newServerSentEvent(eventType, eventId, data2);

        doTest(newSseProtocolString(eventType, eventId, data1, data2), expected1, expected2);
    }

    @Test
    public void testEventWithNoIdDecode() throws Exception {
        String eventType = "add";
        String data = "data line";

        ServerSentEvent expected = newServerSentEvent(eventType, null, data);

        doTest(newSseProtocolString(eventType, null, data), expected);
    }

    @Test
    public void testEventWithNoEventTypeDecode() throws Exception {
        String eventId = "1";
        String data = "data line";

        ServerSentEvent expected = newServerSentEvent(null, eventId, data);

        doTest(newSseProtocolString(null, eventId, data), expected);
    }

    @Test
    public void testEventWithDataOnlyDecode() throws Exception {
        String data = "data line";

        ServerSentEvent expected = newServerSentEvent(null, null, data);

        doTest(newSseProtocolString(null, null, data), expected);
    }

    @Test
    public void testResetEventType() throws Exception {
        String eventType = "add";
        String eventId = "1";
        String data1 = "data line";
        String data2 = "data line";

        ServerSentEvent expected1 = newServerSentEvent(eventType, eventId, data1);
        ServerSentEvent expected2 = newServerSentEvent(null, eventId, data2);

        doTest(newSseProtocolString(eventType, eventId, data1) + newSseProtocolString("", null, data2),
               expected1, expected2);
    }

    @Test
    public void testResetEventId() throws Exception {
        String eventType = "add";
        String eventId = "1";
        String data1 = "data line";
        String data2 = "data line";

        ServerSentEvent expected1 = newServerSentEvent(eventType, eventId, data1);
        ServerSentEvent expected2 = newServerSentEvent(eventType, null, data2);

        doTest(newSseProtocolString(eventType, eventId, data1) + newSseProtocolString(null, "", data2),
               expected1, expected2);
    }

    @Test
    public void testIncompleteEventId() throws Exception {
        List<Object> out = new ArrayList<Object>();
        decoder.callDecode(ch, toByteBuf("id: 111"), out);
        assertEquals("Unexpected number of decoded messages.", 0, out.size());

        ServerSentEvent expected = newServerSentEvent(null, "1111", "data line");

        doTest("1\ndata: data line\n", expected);

    }

    @Test
    public void testIncompleteEventType() throws Exception {
        List<Object> out = new ArrayList<Object>();
        decoder.callDecode(ch, toByteBuf("event: ad"), out);
        assertEquals("Unexpected number of decoded messages.", 0, out.size());

        ServerSentEvent expected = newServerSentEvent("add", null, "data line");

        doTest("d\ndata: data line\n", expected);

    }

    @Test
    public void testIncompleteEventData() throws Exception {
        ServerSentEvent expected = newServerSentEvent("add", null, "data line");

        List<Object> out = new ArrayList<Object>();

        decoder.callDecode(ch, toByteBuf("event: add\n"), out);
        assertEquals("Unexpected number of decoded messages.", 0, out.size());

        decoder.callDecode(ch, toByteBuf("data: d"), out);
        assertEquals("Unexpected number of decoded messages.", 0, out.size());

        doTest("ata line\n", expected);
    }

    @Test
    public void testIncompleteFieldName() throws Exception {
        ServerSentEvent expected = newServerSentEvent("add", null, "data line");

        List<Object> out = new ArrayList<Object>();

        decoder.callDecode(ch, toByteBuf("ev"), out);
        assertEquals("Unexpected number of decoded messages.", 0, out.size());

        decoder.callDecode(ch, toByteBuf("ent: add\n d"), out);
        assertEquals("Unexpected number of decoded messages.", 0, out.size());

        doTest("ata: data line\n", expected);
    }

    @Test
    public void testInvalidFieldNameAndNextEvent() throws Exception {
        ArrayList<Object> out = new ArrayList<Object>();
        decoder.callDecode(ch, toByteBuf("eventt: event type\n"), out);
        assertTrue("Output list not empty.", out.isEmpty());

        decoder.callDecode(ch, toByteBuf("data: dumb \n"), out);
        assertFalse("Event not emitted after invalid field name.", out.isEmpty());
        assertEquals("Unexpected event count after invalid field name.", 1, out.size());

    }

    @Test
    public void testInvalidFieldName() throws Throwable {
        ArrayList<Object> out = new ArrayList<Object>();
        decoder.callDecode(ch, toByteBuf("eventt: dumb \n"), out);
        assertTrue("Event emitted for invalid field name.", out.isEmpty());
    }

    @Test
    public void testFieldNameWithSpace() throws Throwable {
        ArrayList<Object> out = new ArrayList<Object>();
        decoder.callDecode(ch, toByteBuf("eve nt: dumb \n"), new ArrayList<Object>());
        assertTrue("Event emitted for invalid field name.", out.isEmpty());
    }

    @Test
    public void testDataInMultipleChunks() throws Exception {
        ServerSentEvent expected = newServerSentEvent(null, null, "data line");

        List<Object> out = new ArrayList<Object>();

        decoder.callDecode(ch, toByteBuf("da"), out);
        assertEquals("Unexpected number of decoded messages.", 0, out.size());

        decoder.callDecode(ch, toByteBuf("ta: d"), out);
        assertEquals("Unexpected number of decoded messages.", 0, out.size());

        decoder.callDecode(ch, toByteBuf("ata"), out);
        assertEquals("Unexpected number of decoded messages.", 0, out.size());

        decoder.callDecode(ch, toByteBuf(" "), out);
        assertEquals("Unexpected number of decoded messages.", 0, out.size());

        decoder.callDecode(ch, toByteBuf("li"), out);
        assertEquals("Unexpected number of decoded messages.", 0, out.size());

        decoder.callDecode(ch, toByteBuf("ne"), out);
        assertEquals("Unexpected number of decoded messages.", 0, out.size());

        doTest("\n", expected);
    }

    private void doTest(String eventText, ServerSentEvent... expected) {
        List<Object> out = new ArrayList<Object>();
        decoder.callDecode(ch, toByteBuf(eventText), out);

        assertEquals(expected.length, out.size());

        for (int i = 0; i < out.size(); i++) {
            ServerSentEvent event = (ServerSentEvent) out.get(i);
            assertContentEquals("Unexpected SSE data", expected[i].content(), event.content());
            assertContentEquals("Unexpected SSE event type", expected[i].getEventType(),
                                event.getEventType());
            assertContentEquals("Unexpected SSE event id", expected[i].getEventId(), event.getEventId());
        }
    }
}