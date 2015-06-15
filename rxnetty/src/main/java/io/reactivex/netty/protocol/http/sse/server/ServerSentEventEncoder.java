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
package io.reactivex.netty.protocol.http.sse.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.util.ByteProcessor;
import io.reactivex.netty.protocol.http.sse.ServerSentEvent;

import static io.netty.handler.codec.http.HttpHeaderNames.*;

/**
 * An encoder to handle {@link io.reactivex.netty.protocol.http.sse.ServerSentEvent} encoding for an HTTP server.
 *
 * This encoder will encode any {@link io.reactivex.netty.protocol.http.sse.ServerSentEvent} to {@link ByteBuf} and also set the appropriate HTTP Response
 * headers required for <a href="http://www.w3.org/TR/eventsource/">SSE</a>
 */
@ChannelHandler.Sharable
public class ServerSentEventEncoder extends ChannelOutboundHandlerAdapter {

    private static final byte[] EVENT_PREFIX_BYTES = "event: ".getBytes();
    private static final byte[] NEW_LINE_AS_BYTES = "\n".getBytes();
    private static final byte[] ID_PREFIX_AS_BYTES = "id: ".getBytes();
    private static final byte[] DATA_PREFIX_AS_BYTES = "data: ".getBytes();
    private final boolean splitSseData;

    public ServerSentEventEncoder() {
        this(false);
    }

    /**
     * Splits the SSE data on new line and create multiple "data" events if {@code splitSseData} is {@code true}
     *
     * @param splitSseData {@code true} if the SSE data is to be splitted on new line to create multiple "data" events.
     */
    public ServerSentEventEncoder(boolean splitSseData) {
        this.splitSseData = splitSseData;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {

        Object msgToWriteFurther = msg;

        if (msg instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) msg;
            /*Set the content-type for SSE*/
            response.headers().set(CONTENT_TYPE, "text/event-stream");
        } else if (msg instanceof ServerSentEvent) {

            final ServerSentEvent serverSentEvent = (ServerSentEvent) msg;

            final ByteBuf out = ctx.alloc().buffer();
            msgToWriteFurther = out;

            if (serverSentEvent.hasEventType()) { // Write event type, if available
                out.writeBytes(EVENT_PREFIX_BYTES);
                out.writeBytes(serverSentEvent.getEventType());
                out.writeBytes(NEW_LINE_AS_BYTES);
            }

            if (serverSentEvent.hasEventId()) { // Write event id, if available
                out.writeBytes(ID_PREFIX_AS_BYTES);
                out.writeBytes(serverSentEvent.getEventId());
                out.writeBytes(NEW_LINE_AS_BYTES);
            }

            final ByteBuf content;
            if (serverSentEvent.hasDataAsString()) {
                /*Allocate ByteBuf only in the eventloop*/
                content = ctx.alloc().buffer().writeBytes(serverSentEvent.contentAsString().getBytes());
            } else {
                content = serverSentEvent.content();
            }

            if (splitSseData) {
                while (content.isReadable()) { // Scan the buffer and split on new line into multiple data lines.
                    final int readerIndexAtStart = content.readerIndex();
                    int newLineIndex = content.forEachByte(new ByteProcessor() {
                        @Override
                        public boolean process(byte value) throws Exception {
                            return (char) value != '\n';
                        }
                    });
                    if (-1 == newLineIndex) { // No new line, write the buffer as is.
                        out.writeBytes(DATA_PREFIX_AS_BYTES);
                        out.writeBytes(content);
                        out.writeBytes(NEW_LINE_AS_BYTES);
                    } else { // Write the buffer till the new line and then iterate this loop
                        out.writeBytes(DATA_PREFIX_AS_BYTES);
                        out.writeBytes(content, newLineIndex - readerIndexAtStart);
                        content.readerIndex(content.readerIndex() + 1);
                        out.writeBytes(NEW_LINE_AS_BYTES);
                    }
                }
            } else { // write the buffer with data prefix and new line post fix.
                out.writeBytes(DATA_PREFIX_AS_BYTES);
                out.writeBytes(content);
                out.writeBytes(NEW_LINE_AS_BYTES);
            }
        }

        ctx.write(msgToWriteFurther, promise);
    }
}
