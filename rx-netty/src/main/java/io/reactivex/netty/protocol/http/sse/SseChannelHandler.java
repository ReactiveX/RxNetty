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

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.reactivex.netty.protocol.http.client.ClientRequestResponseConverter;

/**
 * A handler to insert {@link ServerSentEventDecoder} at a proper position in the pipeline according to the protocol. <br/>
 * There are the following cases, this handles:
 *
 * <h1>Http response with chunked encoding</h1>
 * In this case, the {@link ServerSentEventDecoder} is inserted after this {@link SseChannelHandler}
 *
 * <h1>Http response with no chunking</h1>
 * In this case, the {@link ServerSentEventDecoder} is inserted as the first handler in the pipeline. This makes the
 * {@link io.netty.buffer.ByteBuf} at the origin to be converted to {@link ServerSentEvent} and hence any other handler
 * will not look at this message unless it is really interested.
 *
 *
 * <h3>Caveat</h3>
 * In some cases where any message is buffered before this pipeline change is made by this handler (i.e. adding
 * {@link ServerSentEventDecoder} as the first handler), the {@link ServerSentEventDecoder} will not be applied to those
 * messages. For this reason we also add {@link ServerSentEventDecoder} after this handler. In cases, where the first
 * {@link ServerSentEventDecoder} is applied on the incoming data, the next instance of {@link ServerSentEventDecoder}
 * will be redundant.
 *
 */
@ChannelHandler.Sharable
public class SseChannelHandler extends SimpleChannelInboundHandler<Object> {

    public static final String NAME = "sse-inbound-handler";
    public static final String SSE_DECODER_HANDLER_NAME = "rx-sse-decoder";
    public static final String SSE_DECODER_POST_INBOUND_HANDLER = "rx-sse-decoder-post-inbound";

    public SseChannelHandler() {
        super(false); // Never auto-release, management of buffer is done via {@link ObservableAdapter}
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg)
            throws Exception {
        if (msg instanceof HttpResponse) {

            /**
             * Since SSE is an endless stream, we can never reuse a connection and hence as soon as SSE traffic is
             * received, the connection is marked as discardable on close.
             */
            ctx.channel().attr(ClientRequestResponseConverter.DISCARD_CONNECTION).set(true); // SSE traffic should always discard connection on close.

            ChannelPipeline pipeline = ctx.channel().pipeline();
            if (!HttpHeaders.isTransferEncodingChunked((HttpResponse) msg)) {
                pipeline.addFirst(SSE_DECODER_HANDLER_NAME, new ServerSentEventDecoder());
                /*
                 * If there are buffered messages in the previous handler at the time this message is read, we would
                 * not be able to convert the content into an SseEvent. For this reason, we also add the decoder after
                 * this handler, so that we can handle the buffered messages.
                 * See the class level javadoc for more details.
                 */
                pipeline.addAfter(NAME, SSE_DECODER_POST_INBOUND_HANDLER, new ServerSentEventDecoder());
            } else {
                pipeline.addAfter(NAME, SSE_DECODER_HANDLER_NAME, new ServerSentEventDecoder());
            }
            ctx.fireChannelRead(msg);
        } else if (msg instanceof LastHttpContent) {
            LastHttpContent lastHttpContent = (LastHttpContent) msg;

            /**
             * The entire pipeline is set based on the assumption that LastHttpContent signals the end of the stream.
             * Since, here we are only passing the content to the rest of the pipeline, it becomes imperative to
             * also pass LastHttpContent as such.
             * For this reason, we send the LastHttpContent again in the pipeline. For this event sent, the content
             * buffer will already be read and hence will not be read again. This message serves as only containing
             * the trailing headers.
             * However, we need to increment the ref count of the content so that the assumptions down the line of the
             * ByteBuf always being released by the last pipeline handler will not break (as ServerSentEventDecoder releases
             * the ByteBuf after read).
             */
            lastHttpContent.content().retain(); // pseudo retain so that the last handler of the pipeline can release it.

            if (lastHttpContent.content().isReadable()) {
                ctx.fireChannelRead(lastHttpContent.content());
            }

            ctx.fireChannelRead(msg); // Since the content is already consumed above (by the SSEDecoder), this is just
            // as sending just trailing headers. This is critical to mark the end of stream.

        } else if (msg instanceof HttpContent) {
            ctx.fireChannelRead(((HttpContent) msg).content());
        } else {
            ctx.fireChannelRead(msg);
        }
    }
}
