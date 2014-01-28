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
package io.reactivex.netty.pipeline;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpObject;
import io.reactivex.netty.protocol.http.HttpClientPipelineConfigurator;
import io.reactivex.netty.protocol.http.HttpObjectAggregationConfigurator;
import io.reactivex.netty.protocol.http.HttpServerPipelineConfigurator;
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

    public static PipelineConfigurator<FullHttpResponse, FullHttpRequest> fullHttpMessageClientConfigurator() {
        return new HttpObjectAggregationConfigurator<FullHttpResponse, FullHttpRequest>(
                new HttpClientPipelineConfigurator<FullHttpRequest, FullHttpResponse>());
    }

    public static PipelineConfigurator<FullHttpRequest, FullHttpResponse> fullHttpMessageServerConfigurator() {
        return new HttpObjectAggregationConfigurator<FullHttpRequest, FullHttpResponse>(
                new HttpServerPipelineConfigurator<FullHttpRequest, FullHttpResponse>());
    }

    public static PipelineConfigurator<SSEEvent, FullHttpRequest> sseClientConfigurator() {
        return new SseOverHttpClientPipelineConfigurator<FullHttpRequest>(
                new HttpClientPipelineConfigurator<FullHttpRequest, HttpObject>());
    }

    public static PipelineConfigurator<FullHttpRequest, Object> sseServerConfigurator() {
        final HttpObjectAggregationConfigurator<FullHttpRequest, Object> configurator =
                new HttpObjectAggregationConfigurator<FullHttpRequest, Object>(new HttpServerPipelineConfigurator<HttpObject, Object>());
        return new SseOverHttpServerPipelineConfigurator<FullHttpRequest>(configurator);
    }
}
