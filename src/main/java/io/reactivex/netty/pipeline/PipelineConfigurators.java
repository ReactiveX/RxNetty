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
package io.reactivex.netty.pipeline;

import io.reactivex.netty.protocol.http.HttpObjectAggregationConfigurator;
import io.reactivex.netty.protocol.http.client.HttpClientPipelineConfigurator;
import io.reactivex.netty.protocol.http.client.HttpRequest;
import io.reactivex.netty.protocol.http.client.HttpResponse;
import io.reactivex.netty.protocol.http.server.HttpServerPipelineConfigurator;
import io.reactivex.netty.protocol.http.sse.SseOverHttpClientPipelineConfigurator;
import io.reactivex.netty.protocol.http.sse.SseOverHttpServerPipelineConfigurator;
import io.reactivex.netty.protocol.text.SimpleTextProtocolConfigurator;
import io.reactivex.netty.protocol.text.sse.SSEEvent;

import java.nio.charset.Charset;

/**
 * Utility class that provides a variety of {@link PipelineConfigurator} implementations
 */
public final class PipelineConfigurators {

    private PipelineConfigurators() {
    }

    public static PipelineConfigurator<String, String> textOnlyConfigurator() {
        return new StringMessageConfigurator();
    }

    public static PipelineConfigurator<String, String> textOnlyConfigurator(Charset inputCharset, Charset outputCharset) {
        return new StringMessageConfigurator(inputCharset, outputCharset);
    }

    public static PipelineConfigurator<String, String> stringMessageConfigurator() {
        return new SimpleTextProtocolConfigurator();
    }

    public static <I, O> PipelineConfigurator<io.reactivex.netty.protocol.http.server.HttpRequest<I>,
            io.reactivex.netty.protocol.http.server.HttpResponse<O>> httpServerConfigurator() {
        return new PipelineConfiguratorComposite<io.reactivex.netty.protocol.http.server.HttpRequest<I>,
                io.reactivex.netty.protocol.http.server.HttpResponse<O>>(new HttpServerPipelineConfigurator<I, O>(),
                                                                         new HttpObjectAggregationConfigurator());
    }

    public static <I, O> PipelineConfigurator<HttpResponse<O>, HttpRequest<I>> httpClientConfigurator() {
        return new PipelineConfiguratorComposite<HttpResponse<O>, HttpRequest<I>>(new HttpClientPipelineConfigurator<I, O>(),
                                                                                  new HttpObjectAggregationConfigurator());
    }

    public static <I> PipelineConfigurator<HttpResponse<SSEEvent>, HttpRequest<I>> sseClientConfigurator() {
        return new SseOverHttpClientPipelineConfigurator<I>();
    }

    public static <I> PipelineConfigurator<io.reactivex.netty.protocol.http.server.HttpRequest<I>,
                                           io.reactivex.netty.protocol.http.server.HttpResponse<SSEEvent>> sseServerConfigurator() {
        return new SseOverHttpServerPipelineConfigurator<I>();
    }
}
