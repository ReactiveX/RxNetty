/*
 * Copyright 2016 Netflix, Inc.
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
 *
 */
package io.reactivex.netty.channel;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.util.ReferenceCountUtil;
import io.reactivex.netty.client.ClientConnectionToChannelBridge.ConnectionReuseEvent;

import java.util.List;

/**
 * A {@link ChannelHandler} that transforms objects written to this channel. <p>
 *
 * Any {@code String} or {@code byte[]} written to the channel are converted to {@code ByteBuf} if no other
 * {@link AllocatingTransformer} is added that accepts these types.
 *
 * If the last added {@link AllocatingTransformer} accepts the written message, then invoke all added transformers and
 * skip the primitive conversions.
 */
public class WriteTransformer extends MessageToMessageCodec<Object, Object> {

    private final WriteTransformations transformations = new WriteTransformations();

    @Override
    public boolean acceptInboundMessage(Object msg) throws Exception {
        return false;
    }

    @Override
    public boolean acceptOutboundMessage(Object msg) throws Exception {
        return true;// Always return true and let the encode do the checking as opposed to be done at both places.
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {
        if (!transformations.transform(msg, ctx.alloc(), out)) {
            /*
             * M2MCodec will release the passed message after encode but we are adding the same object to out.
             * So, the message needs to be retained and subsequently released by the next consumer in the pipeline.
             */
            out.add(ReferenceCountUtil.retain(msg));
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {
        // Never decode (acceptInbound) always returns false.
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof AppendTransformerEvent) {
            @SuppressWarnings("rawtypes")
            AppendTransformerEvent ate = (AppendTransformerEvent) evt;
            transformations.appendTransformer(ate.getTransformer());
        } else if(evt instanceof ConnectionReuseEvent) {
            transformations.resetTransformations();
        }

        super.userEventTriggered(ctx, evt);
    }
}
