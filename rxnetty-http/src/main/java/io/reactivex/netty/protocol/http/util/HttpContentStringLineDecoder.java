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

package io.reactivex.netty.protocol.http.util;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.LastHttpContent;
import io.reactivex.netty.util.LineReader;
import io.reactivex.netty.util.StringLineDecoder;

import java.util.List;

/**
 * A handler just like {@link StringLineDecoder} but works on {@link HttpContent}. This handler will decode the HTTP
 * content as lines, separated by a new line.
 */
public class HttpContentStringLineDecoder extends MessageToMessageDecoder<HttpContent> {

    private final LineReader reader = new LineReader();

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        reader.dispose();
        super.handlerRemoved(ctx);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, HttpContent msg, List<Object> out) throws Exception {
        if (msg instanceof LastHttpContent) {
            reader.decodeLast(msg.content(), out, ctx.alloc());
            out.add(DefaultLastHttpContent.EMPTY_LAST_CONTENT);
        } else {
            reader.decode(msg.content(), out, ctx.alloc());
        }
    }
}
