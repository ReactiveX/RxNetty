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
package io.reactivex.netty.protocol.http.sse;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.reactivex.netty.protocol.http.sse.codec.SSEEvent;
import io.reactivex.netty.protocol.http.sse.codec.ServerSentEventDecoder;

/**
 * A handler to insert {@link ServerSentEventDecoder} at a proper position in the pipeline according to the protocol. <br/>
 * There are the following cases, this handles:
 *
 * <h1>Http response with chunked encoding</h1>
 * In this case, the {@link ServerSentEventDecoder} is inserted after this {@link SSEInboundHandler}
 *
 * <h1>Http response with no chunking</h1>
 * In this case, the {@link ServerSentEventDecoder} is inserted as the first handler in the pipeline. This makes the
 * {@link ByteBuf} at the origin to be converted to {@link SSEEvent} and hence any other handler will not look at this
 * message unless it is really interested.
 *
 * <h1>No HTTP protocol</h1>
 * In this case, the handler does not do anything, assuming that there is no special handling required.
 *
 */
@ChannelHandler.Sharable
public class SSEInboundHandler extends SimpleChannelInboundHandler<Object> {

    public static final String NAME = "sse-handler";
    public static final String SSE_DECODER_HANDLER_NAME = "sse-decoder";

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg)
            throws Exception {
        if (msg instanceof HttpResponse) {
            ChannelPipeline pipeline = ctx.channel().pipeline();
            if (!HttpHeaders.isTransferEncodingChunked((HttpResponse) msg)) {
                pipeline.addFirst(SSE_DECODER_HANDLER_NAME, new ServerSentEventDecoder());
            } else {
                pipeline.addAfter(NAME, SSE_DECODER_HANDLER_NAME, new ServerSentEventDecoder());
            }
            ctx.fireChannelRead(msg);
        } else if (msg instanceof HttpContent) {
            ctx.fireChannelRead(((HttpContent) msg).content());
        } else {
            ctx.fireChannelRead(msg);
        }
    }
}
