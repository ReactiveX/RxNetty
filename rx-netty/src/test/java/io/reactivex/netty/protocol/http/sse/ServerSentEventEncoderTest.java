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
import io.reactivex.netty.NoOpChannelHandlerContext;
import org.junit.Test;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Tomasz Bak
 */
public class ServerSentEventEncoderTest {

    private final ServerSentEventEncoder encoder = new ServerSentEventEncoder();

    private final NoOpChannelHandlerContext ctx = new NoOpChannelHandlerContext();

    @Test
    public void testOneDataLineEncode() throws Exception {
        doTest(
                new ServerSentEvent("1", "add", "test data"),
                "event: add\ndata: test data\nid: 1\n\n"
        );
    }

    @Test
    public void testMultipleDataLineEncode() throws Exception {
        doTest(
                new ServerSentEvent("1", "add", "first line\nsecond line\nthird line"),
                "event: add\ndata: first line\ndata: second line\ndata: third line\nid: 1\n\n"
        );
    }

    @Test
    public void testNoSplitMode() throws Exception {
        doTest(
                new ServerSentEvent("1", "add", "first line\nsecond line\nthird line", false),
                "event: add\ndata: first line\nsecond line\nthird line\nid: 1\n\n"
        );
    }

    @Test
    public void testEventWithNoIdEncode() throws Exception {
        doTest(
                new ServerSentEvent(null, "add", "test data"),
                "event: add\ndata: test data\n\n"
        );
    }

    @Test
    public void testEventWithNoEventTypeEncode() throws Exception {
        doTest(
                new ServerSentEvent("1", null, "test data"),
                "data: test data\nid: 1\n\n"
        );
    }

    @Test
    public void testEventWithDataOnlyEncode() throws Exception {
        doTest(
                new ServerSentEvent(null, null, "test data"),
                "data: test data\n\n"
        );
    }

    private void doTest(ServerSentEvent event, String expectedText) throws Exception {
        List<Object> byteBufOut = new ArrayList<Object>();
        encoder.encode(ctx, event, byteBufOut);

        assertEquals(1, byteBufOut.size());

        String eventText = ((ByteBuf) byteBufOut.get(0)).toString(Charset.defaultCharset());
        assertEquals(expectedText, eventText);
    }
}