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
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;
import java.util.regex.Pattern;

/**
 * An encoder to convert {@link ServerSentEvent} to a {@link ByteBuf}
 *
 * @author Nitesh Kant
 */
@ChannelHandler.Sharable
public class ServerSentEventEncoder extends MessageToMessageEncoder<ServerSentEvent> {

    private static final Pattern PATTERN_NEW_LINE = Pattern.compile("\n");

    @Override
    protected void encode(ChannelHandlerContext ctx, ServerSentEvent serverSentEvent, List<Object> out) throws Exception {
        StringBuilder eventBuilder = new StringBuilder();
        if (serverSentEvent.getEventType() != null) {
            eventBuilder.append("event: ").append(serverSentEvent.getEventType()).append('\n');
        }
        if (serverSentEvent.getEventData() != null) {
            appendData(serverSentEvent, eventBuilder);
        }
        if (serverSentEvent.getEventId() != null) {
            eventBuilder.append("id: ").append(serverSentEvent.getEventId()).append('\n');
        }
        eventBuilder.append('\n');
        String data = eventBuilder.toString();
        out.add(ctx.alloc().buffer(data.length()).writeBytes(data.getBytes()));
    }

    private void appendData(ServerSentEvent serverSentEvent, StringBuilder eventBuilder) {
        if (serverSentEvent.isSplitMode()) {
            for (String dataLine : PATTERN_NEW_LINE.split(serverSentEvent.getEventData())) {
                eventBuilder.append("data: ").append(dataLine).append('\n');
            }
        } else {
            eventBuilder.append("data: ").append(serverSentEvent.getEventData()).append('\n');
        }
    }
}
