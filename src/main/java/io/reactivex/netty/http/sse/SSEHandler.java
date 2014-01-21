/**
 * Copyright 2013 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.reactivex.netty.http.sse;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpResponse;
import io.reactivex.netty.http.sse.codec.ServerSentEventDecoder;

import java.util.List;

public class SSEHandler extends SimpleChannelInboundHandler<Object> {

    public static final String NAME = "sse-handler";
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg)
            throws Exception {
        ByteBuf buf = null;
        if (msg instanceof HttpResponse) {
            ChannelPipeline pipeline = ctx.channel().pipeline();
            pipeline.addAfter(NAME, "http-sse-handler", new ServerSentEventDecoder());
            List<String> encoding = ((HttpResponse) msg).headers().getAll("Transfer-encoding");
            boolean chunked = false;
            if (encoding != null) {
                for (String value: encoding) {
                    if ("chunked".equalsIgnoreCase(value)) {
                        chunked = true;
                        break;
                    }
                }
            }
            if (!chunked) {
                // if chunked encoding is not used on server, remove HttpCodec 
                // and HTTP state tracker to optimize performance. Otherwise, 
                // reserve the HttpCodec and HTTP state tracker to handle HTTP chunks 
                // and ensure end of stream is signaled to the observers
                pipeline.remove("http-response-decoder");
                pipeline.remove("http-codec");
            }
        } else if (msg instanceof HttpContent) {
            buf = ((HttpContent) msg).content();
        } else if (msg instanceof ByteBuf) {
            buf = (ByteBuf) msg;
        }
        if (buf != null) {
            buf.retain();
            ctx.fireChannelRead(buf);
        }
    }
}
