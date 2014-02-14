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

/**
 * An encoder to convert {@link SSEEvent} to a {@link ByteBuf}
 *
 * @author Nitesh Kant
 */
@ChannelHandler.Sharable
public class ServerSentEventEncoder extends MessageToMessageEncoder<SSEEvent> {

    @Override
    protected void encode(ChannelHandlerContext ctx, SSEEvent sseEvent, List<Object> out) throws Exception {
        StringBuilder eventBuilder = new StringBuilder();
        eventBuilder.append(sseEvent.getEventName());
        eventBuilder.append(": ");
        eventBuilder.append(sseEvent.getEventData());
        eventBuilder.append("\n\n");
        String data = eventBuilder.toString();
        out.add(ctx.alloc().buffer(data.length()).writeBytes(data.getBytes()));
    }
}
