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

package io.reactivex.netty.protocol.http.sse;

import io.netty.channel.ChannelHandlerContext;
import io.reactivex.netty.NoOpChannelHandlerContext;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static io.reactivex.netty.protocol.http.sse.SseTestUtil.*;
import static org.junit.Assert.*;

public class ServerSentEventDecoderTest {

    private final ServerSentEventDecoder decoder = new ServerSentEventDecoder();

    private final ChannelHandlerContext ch = new NoOpChannelHandlerContext();

    @Test(timeout = 60000)
    public void testOneDataLineDecode() throws Exception {
        String eventType = "add";
        String eventId = "1";
        String data = "data line";

        ServerSentEvent expected = newServerSentEvent(eventType, eventId, data);

        doTest(newSseProtocolString(eventType, eventId, data), expected);
    }

    @Test(timeout = 60000)
    public void testMultipleDataLineDecode() throws Exception {
        String eventType = "add";
        String eventId = "1";
        String data1 = "data line";
        String data2 = "data line";

        ServerSentEvent expected1 = newServerSentEvent(eventType, eventId, data1);
        ServerSentEvent expected2 = newServerSentEvent(eventType, eventId, data2);

        doTest(newSseProtocolString(eventType, eventId, data1, data2), expected1, expected2);
    }

    @Test(timeout = 60000)
    public void testEventWithNoIdDecode() throws Exception {
        String eventType = "add";
        String data = "data line";

        ServerSentEvent expected = newServerSentEvent(eventType, null, data);

        doTest(newSseProtocolString(eventType, null, data), expected);
    }

    @Test(timeout = 60000)
    public void testEventWithNoEventTypeDecode() throws Exception {
        String eventId = "1";
        String data = "data line";

        ServerSentEvent expected = newServerSentEvent(null, eventId, data);

        doTest(newSseProtocolString(null, eventId, data), expected);
    }

    @Test(timeout = 60000)
    public void testEventWithDataOnlyDecode() throws Exception {
        String data = "data line";

        ServerSentEvent expected = newServerSentEvent(null, null, data);

        doTest(newSseProtocolString(null, null, data), expected);
    }

    @Test(timeout = 60000)
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

    @Test(timeout = 60000)
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

    @Test(timeout = 60000)
    public void testIncompleteEventId() throws Exception {
        List<Object> out = new ArrayList<Object>();
        decoder.decode(ch, toHttpContent("id: 111"), out);
        assertEquals("Unexpected number of decoded messages.", 0, out.size());

        ServerSentEvent expected = newServerSentEvent(null, "1111", "data line");

        doTest("1\ndata: data line\n", expected);

    }

    @Test(timeout = 60000)
    public void testIncompleteEventType() throws Exception {
        List<Object> out = new ArrayList<Object>();
        decoder.decode(ch, toHttpContent("event: ad"), out);
        assertEquals("Unexpected number of decoded messages.", 0, out.size());

        ServerSentEvent expected = newServerSentEvent("add", null, "data line");

        doTest("d\ndata: data line\n", expected);

    }

    @Test(timeout = 60000)
    public void testIncompleteEventData() throws Exception {
        ServerSentEvent expected = newServerSentEvent("add", null, "data line");

        List<Object> out = new ArrayList<Object>();

        decoder.decode(ch, toHttpContent("event: add\n"), out);
        assertEquals("Unexpected number of decoded messages.", 0, out.size());

        decoder.decode(ch, toHttpContent("data: d"), out);
        assertEquals("Unexpected number of decoded messages.", 0, out.size());

        doTest("ata line\n", expected);
    }

    @Test(timeout = 60000)
    public void testIncompleteFieldName() throws Exception {
        ServerSentEvent expected = newServerSentEvent("add", null, "data line");

        List<Object> out = new ArrayList<Object>();

        decoder.decode(ch, toHttpContent("ev"), out);
        assertEquals("Unexpected number of decoded messages.", 0, out.size());

        decoder.decode(ch, toHttpContent("ent: add\n d"), out);
        assertEquals("Unexpected number of decoded messages.", 0, out.size());

        doTest("ata: data line\n", expected);
    }

    @Test(timeout = 60000)
    public void testInvalidFieldNameAndNextEvent() throws Exception {
        ArrayList<Object> out = new ArrayList<Object>();
        decoder.decode(ch, toHttpContent("eventt: event type\n"), out);
        assertTrue("Output list not empty.", out.isEmpty());

        decoder.decode(ch, toHttpContent("data: dumb \n"), out);
        assertFalse("Event not emitted after invalid field name.", out.isEmpty());
        assertEquals("Unexpected event count after invalid field name.", 1, out.size());

    }

    @Test(timeout = 60000)
    public void testInvalidFieldName() throws Throwable {
        ArrayList<Object> out = new ArrayList<Object>();
        decoder.decode(ch, toHttpContent("eventt: dumb \n"), out);
        assertTrue("Event emitted for invalid field name.", out.isEmpty());
    }

    @Test(timeout = 60000)
    public void testFieldNameWithSpace() throws Throwable {
        ArrayList<Object> out = new ArrayList<Object>();
        decoder.decode(ch, toHttpContent("eve nt: dumb \n"), new ArrayList<Object>());
        assertTrue("Event emitted for invalid field name.", out.isEmpty());
    }

    @Test(timeout = 60000)
    public void testDataInMultipleChunks() throws Exception {
        ServerSentEvent expected = newServerSentEvent(null, null, "data line");

        List<Object> out = new ArrayList<Object>();

        decoder.decode(ch, toHttpContent("da"), out);
        assertEquals("Unexpected number of decoded messages.", 0, out.size());

        decoder.decode(ch, toHttpContent("ta: d"), out);
        assertEquals("Unexpected number of decoded messages.", 0, out.size());

        decoder.decode(ch, toHttpContent("ata"), out);
        assertEquals("Unexpected number of decoded messages.", 0, out.size());

        decoder.decode(ch, toHttpContent(" "), out);
        assertEquals("Unexpected number of decoded messages.", 0, out.size());

        decoder.decode(ch, toHttpContent("li"), out);
        assertEquals("Unexpected number of decoded messages.", 0, out.size());

        decoder.decode(ch, toHttpContent("ne"), out);
        assertEquals("Unexpected number of decoded messages.", 0, out.size());

        doTest("\n", expected);
    }

    private void doTest(String eventText, ServerSentEvent... expected) throws Exception {
        List<Object> out = new ArrayList<Object>();
        decoder.decode(ch, toHttpContent(eventText), out);

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