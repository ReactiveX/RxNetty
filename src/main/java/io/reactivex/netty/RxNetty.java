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
import io.reactivex.netty.http.HttpClient;
import io.reactivex.netty.http.HttpClientPipelineConfigurator;
import io.reactivex.netty.http.HttpObjectAggregationConfigurator;
import io.reactivex.netty.http.HttpServer;
import io.reactivex.netty.http.HttpServerBuilder;
import io.reactivex.netty.http.HttpServerPipelineConfigurator;
import io.reactivex.netty.http.sse.SseOverHttpClientPipelineConfigurator;
import io.reactivex.netty.http.sse.SseOverHttpServerPipelineConfigurator;
import io.reactivex.netty.http.sse.codec.SSEEvent;
import io.reactivex.netty.spi.NettyPipelineConfigurator;
import rx.Observable;

public final class RxNetty {

    private RxNetty() {
    }

    public static HttpClient<FullHttpRequest, FullHttpResponse> createAggregatedHttpObjectClient(String host, int port) {
        return createHttpClient(host, port, new HttpObjectAggregationConfigurator(new HttpClientPipelineConfigurator()));
    }

    public static <I extends HttpRequest> HttpClient<I, SSEEvent> createHttpSseClient(String host, int port) {
        return createHttpSseClient(host, port, new HttpClientPipelineConfigurator());
    }

    public static <I extends HttpRequest> HttpClient<I, SSEEvent> createHttpSseClient(String host, int port,
                                                                                      HttpClientPipelineConfigurator httpClientPipelineConfigurator) {
        return createHttpClient(host, port, new SseOverHttpClientPipelineConfigurator(httpClientPipelineConfigurator));
    }

    public static <I extends HttpRequest, O> HttpClient<I, O> createHttpClient(String host, int port) {
        return new HttpClientBuilder<I, O>(host, port).build();
    }

    public static <I extends HttpRequest, O> HttpClient<I, O> createHttpClient(String host, int port,
                                                                               NettyPipelineConfigurator pipelineConfigurator) {
        return new HttpClientBuilder<I, O>(host, port).pipelineConfigurator(pipelineConfigurator).build();
    }

    public static HttpServer<FullHttpRequest, FullHttpResponse> createAggregatedHttpObjectServer(int port) {
        return createHttpServer(port, new HttpObjectAggregationConfigurator(new HttpServerPipelineConfigurator()));
    }

    public static <I extends HttpObject> HttpServer<I, Object> createHttpSseServer(int port,
                                                                                     NettyPipelineConfigurator pipelineConfigurator) {
        return createHttpServer(port, new SseOverHttpServerPipelineConfigurator(pipelineConfigurator));
    }

    public static <I extends HttpObject> HttpServer<I, Object> createHttpSseServer(int port,
                                                                                     HttpServerPipelineConfigurator httpServerPipelineConfigurator) {
        return createHttpServer(port, new SseOverHttpServerPipelineConfigurator(httpServerPipelineConfigurator));
    }

    public static <I extends HttpObject, O> HttpServer<I, O> createHttpServer(int port) {
        return new HttpServerBuilder<I, O>(port).build();
    }

    public static <I extends HttpObject, O> HttpServer<I, O> createHttpServer(int port, NettyPipelineConfigurator pipelineConfigurator) {
        return new HttpServerBuilder<I, O>(port).pipelineConfigurator(pipelineConfigurator).build();
    }

    public static <I, O> NettyServer<I, O> createTcpServer(final int port, NettyPipelineConfigurator pipelineConfigurator) {
        return new ServerBuilder<I, O>(port)
                .pipelineConfigurator(pipelineConfigurator)
                .build();
    }

    public static <I, O> Observable<ObservableConnection<O, I>> createTcpClient(String host, int port, NettyPipelineConfigurator handler) {
        return new ClientBuilder<I, O>(host, port)
                .pipelineConfigurator(handler)
                .build().connect();
    }
}
