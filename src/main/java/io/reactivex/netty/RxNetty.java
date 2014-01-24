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
package io.reactivex.netty;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.reactivex.netty.client.ClientBuilder;
import io.reactivex.netty.client.HttpClientBuilder;
import io.reactivex.netty.client.RxClient;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.protocol.http.HttpClient;
import io.reactivex.netty.protocol.http.HttpClientPipelineConfigurator;
import io.reactivex.netty.protocol.http.HttpObjectAggregationConfigurator;
import io.reactivex.netty.protocol.http.HttpServer;
import io.reactivex.netty.protocol.http.HttpServerBuilder;
import io.reactivex.netty.protocol.http.HttpServerPipelineConfigurator;
import io.reactivex.netty.protocol.http.sse.SseOverHttpClientPipelineConfigurator;
import io.reactivex.netty.protocol.http.sse.SseOverHttpServerPipelineConfigurator;
import io.reactivex.netty.protocol.text.sse.SSEEvent;
import io.reactivex.netty.server.RxServer;
import io.reactivex.netty.server.ServerBuilder;

public final class RxNetty {

    private RxNetty() {
    }

    public static <I extends HttpRequest> HttpClient<I, FullHttpResponse> createAggregatedHttpObjectClient(String host, int port) {
        return createHttpClient(host, port,
                                new HttpObjectAggregationConfigurator<FullHttpResponse, I>(new HttpClientPipelineConfigurator<I, HttpObject>()));
    }

    public static <I extends HttpRequest> HttpClient<I, SSEEvent> createHttpSseClient(String host, int port) {
        return createHttpSseClient(host, port, new HttpClientPipelineConfigurator<I, HttpObject>());
    }

    public static <I extends HttpRequest> HttpClient<I, SSEEvent> createHttpSseClient(String host, int port,
                                                                                      HttpClientPipelineConfigurator<I, ? extends HttpObject> httpClientPipelineConfigurator) {
        return createHttpClient(host, port, new SseOverHttpClientPipelineConfigurator<I>(httpClientPipelineConfigurator));
    }

    public static <I extends HttpRequest, O> HttpClient<I, O> createHttpClient(String host, int port) {
        return new HttpClientBuilder<I, O>(host, port).build();
    }

    public static <I extends HttpRequest, O> HttpClient<I, O> createHttpClient(String host, int port,
                                                                               PipelineConfigurator<O, I> pipelineConfigurator) {
        return new HttpClientBuilder<I, O>(host, port).pipelineConfigurator(pipelineConfigurator).build();
    }

    public static HttpServer<FullHttpRequest, FullHttpResponse> createAggregatedHttpObjectServer(int port) {
        return createHttpServer(port,
                                new HttpObjectAggregationConfigurator<FullHttpRequest, FullHttpResponse>(new HttpServerPipelineConfigurator<HttpObject, FullHttpResponse>()));
    }

    public static <I extends HttpObject> HttpServer<I, Object>
                    createHttpSseServer(int port, PipelineConfigurator<I, ?> pipelineConfigurator) {
        return createHttpServer(port, new SseOverHttpServerPipelineConfigurator<I>(pipelineConfigurator));
    }

    public static <I extends HttpObject> HttpServer<I, Object>
                            createHttpSseServer(int port,
                                                HttpServerPipelineConfigurator<I, ?> httpServerPipelineConfigurator) {
        return createHttpServer(port, new SseOverHttpServerPipelineConfigurator<I>(httpServerPipelineConfigurator));
    }

    public static <I extends HttpObject, O> HttpServer<I, O> createHttpServer(int port) {
        return new HttpServerBuilder<I, O>(port).build();
    }

    public static <I extends HttpObject, O> HttpServer<I, O> createHttpServer(int port, PipelineConfigurator<I, O> pipelineConfigurator) {
        return new HttpServerBuilder<I, O>(port).pipelineConfigurator(pipelineConfigurator).build();
    }

    public static <I, O> RxServer<I, O> createTcpServer(final int port, PipelineConfigurator<I, O> pipelineConfigurator) {
        return new ServerBuilder<I, O>(port)
                .pipelineConfigurator(pipelineConfigurator)
                .build();
    }

    public static <I, O> RxClient<I, O> createTcpClient(String host, int port, PipelineConfigurator<O, I> handler) {
        return new ClientBuilder<I, O>(host, port).pipelineConfigurator(handler).build();
    }
}
