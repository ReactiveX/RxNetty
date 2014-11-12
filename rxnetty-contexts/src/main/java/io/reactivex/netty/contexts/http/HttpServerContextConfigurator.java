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
package io.reactivex.netty.contexts.http;

import io.netty.channel.ChannelPipeline;
import io.reactivex.netty.contexts.RequestCorrelator;
import io.reactivex.netty.contexts.RequestIdProvider;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;

/**
 * An implementation of {@link PipelineConfigurator} to configure an {@link HttpServer} with the context handling.
 *
 * @author Nitesh Kant
 */
public class HttpServerContextConfigurator<I, O> implements
        PipelineConfigurator<HttpServerRequest<I>, HttpServerResponse<O>> {

    public static final String CTX_HANDLER_NAME = "http-server-context-handler";

    private final RequestCorrelator correlator;
    private final RequestIdProvider requestIdProvider;
    private final PipelineConfigurator<HttpServerRequest<I>, HttpServerResponse<O>> httpConfigurator;

    public HttpServerContextConfigurator(RequestIdProvider requestIdProvider, RequestCorrelator correlator,
                                         PipelineConfigurator<HttpServerRequest<I>, HttpServerResponse<O>> httpConfigurator) {
        this.requestIdProvider = requestIdProvider;
        this.correlator = correlator;
        this.httpConfigurator = httpConfigurator;
    }

    @Override
    public void configureNewPipeline(ChannelPipeline pipeline) {
        httpConfigurator.configureNewPipeline(pipeline);
        pipeline.addLast(CTX_HANDLER_NAME, new HttpServerContextHandler(requestIdProvider, correlator));
    }
}
