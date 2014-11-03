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
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.util.List;

/**
 * An decoder for server-sent event. It does not record retry or last event ID. Otherwise, it
 * follows the same interpretation logic as documented here: <a href="http://www.whatwg.org/specs/web-apps/current-work/multipage/comms.html#event-stream-interpretation">Event Stream
 * Interpretation</a>
 *
 * @deprecated Use {@link io.reactivex.netty.protocol.http.sse.ServerSentEventDecoder} instead.
 */
@Deprecated
public class ServerSentEventDecoder extends ReplayingDecoder<ServerSentEventDecoder.State> {
    private final MessageBuffer eventBuffer;

    public ServerSentEventDecoder() {
        super(State.NEW_LINE);

        eventBuffer = new MessageBuffer();
    }

    private static boolean isLineDelimiter(char c) {
        return c == '\r' || c == '\n';
    }

    private static class MessageBuffer {
        private boolean eventTypePresent;
        private final StringBuilder eventType = new StringBuilder();
        private final StringBuilder eventData = new StringBuilder();
        private boolean eventIdPresent;
        private final StringBuilder eventId = new StringBuilder();

        private MessageBuffer() {
            reset();
        }

        public MessageBuffer setEventType(String eventType) {
            this.eventType.setLength(0);
            this.eventType.append(eventType);
            eventTypePresent = true;

            return this;
        }

        public MessageBuffer appendEventData(String eventData) {
            if(this.eventData.length() > 0) {
                this.eventData.append('\n');
            }
            this.eventData.append(eventData);

            return this;
        }

        public MessageBuffer setEventId(String id) {
            eventId.setLength(0);
            eventId.append(id);
            eventIdPresent = true;

            return this;
        }

        public ServerSentEvent toMessage() {
            ServerSentEvent message = new ServerSentEvent(
                    eventIdPresent ? eventId.toString() : null,
                    eventTypePresent ? eventType.toString() : null,
                    eventData.toString()
                    );

            reset();

            return message;
        }

        public void reset() {
            eventIdPresent = false;
            eventId.setLength(0);
            eventTypePresent = false;
            eventType.setLength(0);
            eventData.setLength(0);
        }
    }

    private boolean eventStarted;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        switch (state()) {
        case NEW_LINE:
            String line = readFullLine(in);

            int colonIndex = line.indexOf(':');

            if (colonIndex <= 0) {
                // Comment or line without field name, ignore it.
                break;
            }

            String type = line.substring(0, colonIndex).trim();
            if ("data".equals(type)) {
                eventStarted = true;
                eventBuffer.appendEventData(line.substring(colonIndex + 1).trim());
            }

            if ("event".equals(type)) {
                eventBuffer.setEventType(line.substring(colonIndex + 1).trim());
            }

            if ("id".equals(type)) {
                eventBuffer.setEventId(line.substring(colonIndex + 1).trim());
            }

            break;
        case END_OF_LINE:
            int skipped = skipLineDelimiters(in);
            if (skipped > 0 && eventStarted) {
                out.add(eventBuffer.toMessage());
                eventBuffer.reset();
                eventStarted = false;
            }
            break;
        }
    }

    private String readFullLine(ByteBuf in) {
        StringBuilder line = new StringBuilder();

        for (;;) {
            char c = (char) in.readByte();
            if (isLineDelimiter(c)) {
                // If colon is at the very end of a line, having an empty string will help us avoid IndexOutOfBoundException
                // without checking the colon is at the end of the line.
                line.append("");

                checkpoint(State.END_OF_LINE);
                break;
            }

            line.append(c);
        }
        return line.toString();
    }

    private int skipLineDelimiters(ByteBuf in) {
        int skipped = 0;
        while (in.writerIndex() - in.readerIndex() > 0) {
            // the above check is needed to ensure that last event is delivered
            // otherwise, an exception (Netty's Signal object) will be thrown
            // from ReplayingDecoderBuffer.readByte()
            char c = (char) in.readByte();
            if (isLineDelimiter(c)) {
                skipped += 1;
                continue;
            }

            // Leave the reader index at the first letter of the next line, if any
            in.readerIndex(in.readerIndex() - 1);
            checkpoint(State.NEW_LINE);
            break;
        }

        return skipped;
    }

    public enum State {
        NEW_LINE,
        END_OF_LINE
    }
}