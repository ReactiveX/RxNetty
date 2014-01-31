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
import io.reactivex.netty.client.RxClient;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.protocol.http.HttpClient;
import io.reactivex.netty.protocol.http.HttpClientBuilder;
import io.reactivex.netty.protocol.http.HttpServer;
import io.reactivex.netty.protocol.http.HttpServerBuilder;
import io.reactivex.netty.protocol.text.sse.SSEEvent;
import io.reactivex.netty.server.RxServer;
import io.reactivex.netty.server.ServerBuilder;

public final class RxNetty {

    private RxNetty() {
    }

    public static <I, O> RxServer<I, O> createTcpServer(final int port, PipelineConfigurator<I, O> pipelineConfigurator) {
        return new ServerBuilder<I, O>(port).pipelineConfigurator(pipelineConfigurator).build();
    }

    public static <I, O> RxClient<I, O> createTcpClient(String host, int port, PipelineConfigurator<O, I> handler) {
        return new ClientBuilder<I, O>(host, port).pipelineConfigurator(handler).build();
    }

    public static HttpServer<FullHttpRequest, FullHttpResponse> createHttpServer(int port) {
        return createHttpServer(port, PipelineConfigurators.fullHttpMessageServerConfigurator());
    }

    public static HttpClient<FullHttpRequest, FullHttpResponse> createHttpClient(String host, int port) {
        return createHttpClient(host, port, PipelineConfigurators.fullHttpMessageClientConfigurator());
    }

    public static <I extends HttpObject, O> HttpServer<I, O> createStreamingHttpServer(int port) {
        return new HttpServerBuilder<I, O>(port).build();
    }

    public static <O extends HttpObject> HttpClient<FullHttpRequest, O> createStreamingHttpClient(String host, int port) {
        return new HttpClientBuilder<FullHttpRequest, O>(host, port).build();
    }

    public static <I extends HttpObject, O> HttpServer<I, O> createHttpServer(int port, PipelineConfigurator<I, O> pipelineConfigurator) {
        return new HttpServerBuilder<I, O>(port).pipelineConfigurator(pipelineConfigurator).build();
    }

    public static <I extends HttpRequest, O> HttpClient<I, O> createHttpClient(String host, int port,
                                                                               PipelineConfigurator<O, I> pipelineConfigurator) {
        return new HttpClientBuilder<I, O>(host, port).pipelineConfigurator(pipelineConfigurator).build();
    }

    public static HttpServer<FullHttpRequest, Object> createSseServer(int port) {
        return createHttpServer(port, PipelineConfigurators.sseServerConfigurator());
    }

    public static HttpClient<FullHttpRequest, SSEEvent> createSseClient(String host, int port) {
        return createHttpClient(host, port, PipelineConfigurators.sseClientConfigurator());
    }
}
