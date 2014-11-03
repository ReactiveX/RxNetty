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

import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;

/**
 * @author Nitesh Kant
 */
public final class SseTestUtil {

    private SseTestUtil() {
    }

    public static ServerSentEvent newServerSentEvent(String eventType, String eventId, String data) {
        ByteBuf eventTypeBuffer = null != eventType
                                  ? Unpooled.buffer().writeBytes(eventType.getBytes(Charset.forName("UTF-8")))
                                  : null;
        ByteBuf eventIdBuffer = null != eventId
                                ? Unpooled.buffer().writeBytes(eventId.getBytes(Charset.forName("UTF-8")))
                                : null;

        ByteBuf dataBuffer = Unpooled.buffer().writeBytes(data.getBytes(Charset.forName("UTF-8")));

        return ServerSentEvent.withEventIdAndType(eventIdBuffer, eventTypeBuffer, dataBuffer);
    }

    public static String newSseProtocolString(String eventType, String eventId, String... dataElements) {
        StringBuilder eventStream = new StringBuilder();

        if (null != eventType) {
            eventStream.append("event: ").append(eventType).append('\n');
        }

        if (null != eventId) {
            eventStream.append("id: ").append(eventId).append('\n');
        }

        for (String aData : dataElements) {
            eventStream.append("data: ").append(aData).append('\n');
        }
        return eventStream.toString();
    }

    public static void assertContentEquals(String message, ByteBuf expected, ByteBuf actual) {
        assertEquals(message,
                     null == expected ? null : expected.toString(Charset.defaultCharset()),
                     null == actual ? null : actual.toString(Charset.defaultCharset()));
    }

    public static ByteBuf toByteBuf(String event) {
        ByteBuf in = Unpooled.buffer(1024);
        in.writeBytes(event.getBytes(Charset.defaultCharset()));
        return in;
    }
}
