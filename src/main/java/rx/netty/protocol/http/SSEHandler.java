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
package rx.netty.protocol.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;

public class SSEHandler extends SimpleChannelInboundHandler<HttpObject> {

    public static final String NAME = "sse-handler";
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg)
            throws Exception {
        if (msg instanceof HttpResponse) {
            ChannelPipeline pipeline = ctx.channel().pipeline();
            pipeline.remove("http-codec");
            pipeline.remove(NAME);
            pipeline.replace("http-response-decoder", "http-sse-handler", new ServerSentEventDecoder());
        }
    }
}
