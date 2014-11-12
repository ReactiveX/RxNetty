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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.protocol.http.server.HttpServerPipelineConfigurator;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.text.sse.SSEServerPipelineConfigurator;
import io.reactivex.netty.protocol.text.sse.ServerSentEvent;
import io.reactivex.netty.protocol.text.sse.ServerSentEventEncoder;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.reactivex.netty.protocol.text.sse.SSEServerPipelineConfigurator.SERVER_SENT_EVENT_ENCODER;
import static io.reactivex.netty.protocol.text.sse.SSEServerPipelineConfigurator.SSE_ENCODER_HANDLER_NAME;

/**
 * An extension to {@link SSEServerPipelineConfigurator} that enables SSE over HTTP. <br/>
 *
 * @see ServerSentEventEncoder
 *
 * @author Nitesh Kant
 *
 * @deprecated Use {@link io.reactivex.netty.protocol.http.sse.SseServerPipelineConfigurator} instead.
 */
@Deprecated
public class SseOverHttpServerPipelineConfigurator<I>
        implements PipelineConfigurator<HttpServerRequest<I>, HttpServerResponse<ServerSentEvent>> {

    public static final String SSE_RESPONSE_HEADERS_COMPLETER = "sse-response-headers-completer";

    private final HttpServerPipelineConfigurator<I, ?> serverPipelineConfigurator;

    public SseOverHttpServerPipelineConfigurator() {
        this(new HttpServerPipelineConfigurator<I, Object>());
    }

    public SseOverHttpServerPipelineConfigurator(HttpServerPipelineConfigurator<I, ?> serverPipelineConfigurator) {
        this.serverPipelineConfigurator = serverPipelineConfigurator;
    }

    @Override
    public void configureNewPipeline(ChannelPipeline pipeline) {
        serverPipelineConfigurator.configureNewPipeline(pipeline);
        pipeline.addLast(SSE_ENCODER_HANDLER_NAME, SERVER_SENT_EVENT_ENCODER);
        pipeline.addLast(SSE_RESPONSE_HEADERS_COMPLETER, new ChannelOutboundHandlerAdapter() {
            @Override
            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                if (HttpServerResponse.class.isAssignableFrom(msg.getClass())) {
                    @SuppressWarnings("rawtypes")
                    HttpServerResponse rxResponse = (HttpServerResponse) msg;
                    String contentTypeHeader = rxResponse.getHeaders().get(CONTENT_TYPE);
                    if (null == contentTypeHeader) {
                        rxResponse.getHeaders().set(CONTENT_TYPE, "text/event-stream");
                    }
                }
                super.write(ctx, msg, promise);
            }
        });
    }
}
