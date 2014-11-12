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
package io.reactivex.netty.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;

/**
 * @author Nitesh Kant
 */
public class ByteArrayPipelineConfigurator implements PipelineConfigurator<byte[], byte[]> {

    @Override
    public void configureNewPipeline(ChannelPipeline pipeline) {
        pipeline.addLast(new ChannelDuplexHandler() {

            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                boolean handled = false;

                if (ByteBuf.class.isAssignableFrom(msg.getClass())) {
                    ByteBuf byteBuf = (ByteBuf) msg;
                    if (byteBuf.isReadable()) {
                        int readableBytes = byteBuf.readableBytes();
                        byte[] msgToPass = new byte[readableBytes];
                        byteBuf.readBytes(msgToPass);
                        handled = true;
                        ctx.fireChannelRead(msgToPass);
                    }
                }
                if (!handled) {
                    super.channelRead(ctx, msg);
                }
            }

            @Override
            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                if (msg instanceof byte[]) {
                    byte[] msgAsBytes = (byte[]) msg;
                    ByteBuf byteBuf = ctx.alloc().buffer(msgAsBytes.length).writeBytes(msgAsBytes);
                    super.write(ctx, byteBuf, promise);
                } else {
                    super.write(ctx, msg, promise); // pass-through
                }
            }
        });
    }
}
