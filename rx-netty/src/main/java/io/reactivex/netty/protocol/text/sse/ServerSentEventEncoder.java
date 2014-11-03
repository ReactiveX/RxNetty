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
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;
import java.util.regex.Pattern;

/**
 * An encoder to convert {@link ServerSentEvent} to a {@link ByteBuf}
 *
 * @author Nitesh Kant
 *
 * @deprecated Use {@link io.reactivex.netty.protocol.http.sse.ServerSentEventEncoder} instead.
 */
@Deprecated
@ChannelHandler.Sharable
public class ServerSentEventEncoder extends MessageToMessageEncoder<ServerSentEvent> {

    private static final Pattern PATTERN_NEW_LINE = Pattern.compile("\n");
    private static final byte[] EVENT_PREFIX_BYTES = "event: ".getBytes();
    private static final byte[] NEW_LINE_AS_BYTES = "\n".getBytes();
    private static final byte[] ID_PREFIX_AS_BYTES = "id: ".getBytes();
    private static final byte[] DATA_PREFIX_AS_BYTES = "data: ".getBytes();

    @Override
    protected void encode(ChannelHandlerContext ctx, ServerSentEvent serverSentEvent, List<Object> out) throws Exception {
        ByteBuf outBuffer = ctx.alloc().buffer();
        out.add(outBuffer);

        if (serverSentEvent.getEventType() != null) {
            outBuffer.writeBytes(EVENT_PREFIX_BYTES)
                     .writeBytes(serverSentEvent.getEventType().getBytes())
                     .writeBytes(NEW_LINE_AS_BYTES);
        }

        if (serverSentEvent.getEventData() != null) {
            appendData(serverSentEvent, outBuffer);
        }

        if (serverSentEvent.getEventId() != null) {
            outBuffer.writeBytes(ID_PREFIX_AS_BYTES)
                     .writeBytes(serverSentEvent.getEventId().getBytes())
                     .writeBytes(NEW_LINE_AS_BYTES);
        }

        outBuffer.writeBytes(NEW_LINE_AS_BYTES);
    }

    private static void appendData(ServerSentEvent serverSentEvent, ByteBuf outBuffer) {
        if (serverSentEvent.isSplitMode()) {
            for (String dataLine : PATTERN_NEW_LINE.split(serverSentEvent.getEventData())) {
                outBuffer.writeBytes(DATA_PREFIX_AS_BYTES)
                         .writeBytes(dataLine.getBytes())
                         .writeBytes(NEW_LINE_AS_BYTES);
            }
        } else {
            outBuffer.writeBytes(DATA_PREFIX_AS_BYTES)
                     .writeBytes(serverSentEvent.getEventData().getBytes())
                     .writeBytes(NEW_LINE_AS_BYTES);
        }
    }
}
