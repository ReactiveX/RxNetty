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

/**
 * {@link PipelineConfigurator} implementation for <a href="http://www.w3.org/TR/eventsource/">Server Sent Events</a> to
 * be used for SSE clients.
 *
 * @author Nitesh Kant
 */
public class SseClientPipelineConfigurator<I>
        implements PipelineConfigurator<HttpClientResponse<ServerSentEvent>, HttpClientRequest<I>> {

    public static final SseChannelHandler SSE_CHANNEL_HANDLER = new SseChannelHandler();

    private final HttpClientPipelineConfigurator<I, ?> httpClientPipelineConfigurator;

    public SseClientPipelineConfigurator() {
        this(new HttpClientPipelineConfigurator<I, Object>());
    }

    public SseClientPipelineConfigurator(HttpClientPipelineConfigurator<I, ?> httpClientPipelineConfigurator) {
        this.httpClientPipelineConfigurator = httpClientPipelineConfigurator;
    }

    @Override
    public void configureNewPipeline(ChannelPipeline pipeline) {
        httpClientPipelineConfigurator.configureNewPipeline(pipeline);
        if (null != pipeline.get(HttpClientPipelineConfigurator.REQUEST_RESPONSE_CONVERTER_HANDLER_NAME)) {
            pipeline.addBefore(HttpClientPipelineConfigurator.REQUEST_RESPONSE_CONVERTER_HANDLER_NAME,
                               SseChannelHandler.NAME, SSE_CHANNEL_HANDLER);
        } else {
            // Assuming that the underlying HTTP configurator knows what its doing. It will mostly fail though.
            pipeline.addLast(SseChannelHandler.NAME, SSE_CHANNEL_HANDLER);
        }
    }
}
