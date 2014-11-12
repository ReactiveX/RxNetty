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
package io.reactivex.netty.contexts;

import io.reactivex.netty.contexts.http.HttpClientContextConfigurator;
import io.reactivex.netty.contexts.http.HttpServerContextConfigurator;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.sse.ServerSentEvent;

/**
 * A factory class for different {@link PipelineConfigurator} for the context module.
 *
 * @author Nitesh Kant
 */
public final class ContextPipelineConfigurators {

    private ContextPipelineConfigurators() {
    }


    public static <I, O> PipelineConfigurator<HttpServerRequest<I>, HttpServerResponse<O>>
    httpServerConfigurator(RequestIdProvider requestIdProvider, RequestCorrelator correlator,
                           PipelineConfigurator<HttpServerRequest<I>, HttpServerResponse<O>> httpConfigurator) {
        return new HttpServerContextConfigurator<I, O>(requestIdProvider, correlator, httpConfigurator);
    }

    public static <I, O> PipelineConfigurator<HttpClientResponse<O>, HttpClientRequest<I>>
    httpClientConfigurator(RequestIdProvider requestIdProvider, RequestCorrelator correlator,
                           PipelineConfigurator<HttpClientResponse<O>, HttpClientRequest<I>> httpConfigurator) {
        return new HttpClientContextConfigurator<I, O>(requestIdProvider, correlator, httpConfigurator);
    }

    public static <I, O> PipelineConfigurator<HttpServerRequest<I>, HttpServerResponse<O>>
    httpServerConfigurator(RequestIdProvider requestIdProvider, RequestCorrelator correlator) {
        return httpServerConfigurator(requestIdProvider, correlator, PipelineConfigurators.<I, O>httpServerConfigurator());
    }

    public static <I, O> PipelineConfigurator<HttpClientResponse<O>, HttpClientRequest<I>>
    httpClientConfigurator(RequestIdProvider requestIdProvider, RequestCorrelator correlator) {
        return httpClientConfigurator(requestIdProvider, correlator, PipelineConfigurators.<I, O>httpClientConfigurator());
    }

    public static <I> PipelineConfigurator<HttpClientResponse<ServerSentEvent>, HttpClientRequest<I>>
    sseClientConfigurator(RequestIdProvider requestIdProvider, RequestCorrelator correlator) {
        return new HttpClientContextConfigurator<I, ServerSentEvent>(requestIdProvider, correlator,
                                                                     PipelineConfigurators.<I>clientSseConfigurator());
    }

    public static <I> PipelineConfigurator<HttpServerRequest<I>, HttpServerResponse<ServerSentEvent>>
    sseServerConfigurator(RequestIdProvider requestIdProvider, RequestCorrelator correlator) {
        return new HttpServerContextConfigurator<I, ServerSentEvent>(requestIdProvider, correlator,
                                                                     PipelineConfigurators.<I>serveSseConfigurator());
    }
}
