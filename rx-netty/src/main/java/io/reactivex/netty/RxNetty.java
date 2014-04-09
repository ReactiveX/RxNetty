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
package io.reactivex.netty;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.channel.ConnectionHandler;
import io.reactivex.netty.channel.SingleNioLoopProvider;
import io.reactivex.netty.channel.RxEventLoopProvider;
import io.reactivex.netty.client.ClientBuilder;
import io.reactivex.netty.client.RxClient;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientBuilder;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.HttpServerBuilder;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import io.reactivex.netty.server.RxServer;
import io.reactivex.netty.server.ServerBuilder;

public final class RxNetty {

    private static volatile RxEventLoopProvider rxEventLoopProvider = new SingleNioLoopProvider();

    private RxNetty() {
    }

    public static <I, O> RxServer<I, O> createTcpServer(final int port, PipelineConfigurator<I, O> pipelineConfigurator,
                                                        ConnectionHandler<I, O> connectionHandler) {
        return new ServerBuilder<I, O>(port, connectionHandler).pipelineConfigurator(pipelineConfigurator).build();
    }

    public static <I, O> RxClient<I, O> createTcpClient(String host, int port, PipelineConfigurator<O, I> configurator) {
        return new ClientBuilder<I, O>(host, port).pipelineConfigurator(configurator).build();
    }

    public static RxServer<ByteBuf, ByteBuf> createTcpServer(final int port,
                                                             ConnectionHandler<ByteBuf, ByteBuf> connectionHandler) {
        return new ServerBuilder<ByteBuf, ByteBuf>(port, connectionHandler).build();
    }

    public static RxClient<ByteBuf, ByteBuf> createTcpClient(String host, int port) {
        return new ClientBuilder<ByteBuf, ByteBuf>(host, port).build();
    }

    public static HttpServer<ByteBuf, ByteBuf> createHttpServer(int port, RequestHandler<ByteBuf, ByteBuf> requestHandler) {
        return new HttpServerBuilder<ByteBuf, ByteBuf>(port, requestHandler).build();
    }

    public static HttpClient<ByteBuf, ByteBuf> createHttpClient(String host, int port) {
        return new HttpClientBuilder<ByteBuf, ByteBuf>(host, port).build();
    }

    public static <I, O> HttpServer<I, O> createHttpServer(int port,
                                                           RequestHandler<I, O> requestHandler,
                                                           PipelineConfigurator<HttpServerRequest<I>, HttpServerResponse<O>> configurator) {
        return new HttpServerBuilder<I, O>(port, requestHandler).pipelineConfigurator(configurator).build();
    }

    public static <I, O> HttpClient<I, O> createHttpClient(String host, int port,
                                                           PipelineConfigurator<HttpClientResponse<O>,
                                                                                HttpClientRequest<I>> configurator) {
        return new HttpClientBuilder<I, O>(host, port).pipelineConfigurator(configurator).build();
    }

    /**
     * An implementation of {@link RxEventLoopProvider} to be used by all clients and servers created after this call.
     *
     * @param provider New provider to use.
     *
     * @return Existing provider.
     */
    public static RxEventLoopProvider useEventLoopProvider(RxEventLoopProvider provider) {
        RxEventLoopProvider oldProvider = rxEventLoopProvider;
        rxEventLoopProvider = provider;
        return oldProvider;
    }

    public static RxEventLoopProvider getRxEventLoopProvider() {
        return rxEventLoopProvider;
    }
}
