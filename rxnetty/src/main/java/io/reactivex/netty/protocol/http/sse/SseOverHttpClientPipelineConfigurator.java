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

import io.netty.channel.ChannelPipeline;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.protocol.http.client.HttpClientPipelineConfigurator;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.text.sse.SSEClientPipelineConfigurator;
import io.reactivex.netty.protocol.text.sse.ServerSentEvent;
import io.reactivex.netty.protocol.text.sse.ServerSentEventDecoder;

/**
 * An extension to {@link SSEClientPipelineConfigurator} that enables SSE over HTTP. <br/>
 *
 * @see SSEInboundHandler
 * @see ServerSentEventDecoder
 *
 * @author Nitesh Kant
 *
 * @deprecated Use {@link io.reactivex.netty.protocol.http.sse.SseClientPipelineConfigurator} instead.
 */
@Deprecated
public class SseOverHttpClientPipelineConfigurator<I> implements PipelineConfigurator<HttpClientResponse<ServerSentEvent>, HttpClientRequest<I>> {

    private final HttpClientPipelineConfigurator<I, ?> httpClientPipelineConfigurator;

    public SseOverHttpClientPipelineConfigurator() {
        this(new HttpClientPipelineConfigurator<I, Object>());
    }

    public SseOverHttpClientPipelineConfigurator(HttpClientPipelineConfigurator<I, ?> httpClientPipelineConfigurator) {
        this.httpClientPipelineConfigurator = httpClientPipelineConfigurator;
    }

    @Override
    public void configureNewPipeline(ChannelPipeline pipeline) {
        httpClientPipelineConfigurator.configureNewPipeline(pipeline);
        if (null != pipeline.get(HttpClientPipelineConfigurator.REQUEST_RESPONSE_CONVERTER_HANDLER_NAME)) {
            pipeline.addBefore(HttpClientPipelineConfigurator.REQUEST_RESPONSE_CONVERTER_HANDLER_NAME,
                               SSEInboundHandler.NAME, SSEClientPipelineConfigurator.SSE_INBOUND_HANDLER);
        } else {
            // Assuming that the underlying HTTP configurator knows what its doing. It will mostly fail though.
            pipeline.addLast(SSEInboundHandler.NAME, SSEClientPipelineConfigurator.SSE_INBOUND_HANDLER);
        }
    }
}
