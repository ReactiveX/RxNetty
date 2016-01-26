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
package io.reactivex.netty.util;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * A decoder that breaks an incoming {@link ByteBuf}s into a list of strings delimited by a new line.
 */
public class StringLineDecoder extends ByteToMessageDecoder {

    private final LineReader lineReader = new LineReader();

    @Override
    protected void handlerRemoved0(ChannelHandlerContext ctx) throws Exception {
        lineReader.dispose();
        super.handlerRemoved0(ctx);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        lineReader.decode(in, out, ctx.alloc());
    }
}
