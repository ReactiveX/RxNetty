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

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.client.RxClient;
import io.reactivex.netty.contexts.http.HttpContextClientChannelFactory;
import io.reactivex.netty.contexts.http.HttpRequestIdProvider;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientBuilder;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.HttpServerBuilder;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import io.reactivex.netty.server.RxServer;

/**
 * A factory class to create {@link RxClient} and {@link RxServer} objects which are context aware. <br/>
 * In case, the factory method provided here does not work for a usecase, the provided factory methods for creating the
 * builders must be used. <br/>
 *
 * @author Nitesh Kant
 */
public final class RxContexts {

    public static final ThreadLocalRequestCorrelator DEFAULT_CORRELATOR = new ThreadLocalRequestCorrelator();

    private static String defaultRequestIdContextKeyName = "X-RXNETTY-REQUEST-ID";

    private RxContexts() {
    }

    public static <I, O> HttpServerBuilder<I, O> newHttpServerBuilder(int port, RequestHandler<I, O> requestHandler,
                                                                      RequestCorrelator correlator) {
        HttpRequestIdProvider provider = new HttpRequestIdProvider(defaultRequestIdContextKeyName, correlator);
        return newHttpServerBuilder(port, requestHandler, provider, correlator);
    }

    public static <I, O> HttpServerBuilder<I, O> newHttpServerBuilder(int port, RequestHandler<I, O> requestHandler,
            RequestCorrelator correlator, PipelineConfigurator<HttpServerRequest<I>, HttpServerResponse<O>> configurator) {
        HttpRequestIdProvider provider = new HttpRequestIdProvider(defaultRequestIdContextKeyName, correlator);
        return newHttpServerBuilder(port, requestHandler, provider, correlator, configurator);
    }

    public static <I, O> HttpServerBuilder<I, O> newHttpServerBuilder(int port, RequestHandler<I, O> requestHandler,
                                                                      String requestIdHeaderName,
                                                                      RequestCorrelator correlator) {
        HttpRequestIdProvider provider = new HttpRequestIdProvider(requestIdHeaderName, correlator);
        return newHttpServerBuilder(port, requestHandler, provider, correlator);
    }

    public static <I, O> HttpClientBuilder<I, O> newHttpClientBuilder(String host, int port,
                                                                      RequestCorrelator correlator) {
        HttpRequestIdProvider provider = new HttpRequestIdProvider(defaultRequestIdContextKeyName, correlator);
        return newHttpClientBuilder(host, port, provider, correlator);
    }
    
    public static <I, O> HttpClientBuilder<I, O> newHttpClientBuilder(String host, int port,
            RequestCorrelator correlator, PipelineConfigurator<HttpClientResponse<O>, HttpClientRequest<I>> httpConfigurator) {
        HttpRequestIdProvider provider = new HttpRequestIdProvider(defaultRequestIdContextKeyName, correlator);
        return newHttpClientBuilder(host, port, provider, correlator, httpConfigurator);
    }


    public static <I, O> HttpClientBuilder<I, O> newHttpClientBuilder(String host, int port, String requestIdHeaderName,
                                                                      RequestCorrelator correlator) {
        HttpRequestIdProvider provider = new HttpRequestIdProvider(requestIdHeaderName, correlator);
        return newHttpClientBuilder(host, port, provider, correlator);
    }

    public static <I, O> HttpServerBuilder<I, O> newHttpServerBuilder(int port, RequestHandler<I, O> requestHandler,
                                                                      RequestIdProvider provider,
                                                                      RequestCorrelator correlator, 
                                                                      PipelineConfigurator<HttpServerRequest<I>, HttpServerResponse<O>> configurator) {
        return RxNetty.newHttpServerBuilder(port, requestHandler)
                      .pipelineConfigurator(ContextPipelineConfigurators.<I, O>httpServerConfigurator(provider,
                                                                                                      correlator, configurator));
    }

    public static <I, O> HttpServerBuilder<I, O> newHttpServerBuilder(int port, RequestHandler<I, O> requestHandler,
            RequestIdProvider provider,
            RequestCorrelator correlator) {
        return RxNetty.newHttpServerBuilder(port, requestHandler)
                .pipelineConfigurator(ContextPipelineConfigurators.<I, O>httpServerConfigurator(provider,
                        correlator));
    }

    public static <I, O> HttpClientBuilder<I, O> newHttpClientBuilder(String host, int port,
                                                                      RequestIdProvider provider,
                                                                      RequestCorrelator correlator) {
        HttpClientBuilder<I, O> builder = RxNetty.newHttpClientBuilder(host, port);
        return builder.pipelineConfigurator(ContextPipelineConfigurators.<I, O>httpClientConfigurator(provider,
                                                                                                      correlator))
                      .withChannelFactory(new HttpContextClientChannelFactory<I, O>(builder.getBootstrap(),
                                                                                    correlator,
                                                                                    builder.getEventsSubject()));
    }
    
    public static <I, O> HttpClientBuilder<I, O> newHttpClientBuilder(String host, int port,
            RequestIdProvider provider, RequestCorrelator correlator, 
            PipelineConfigurator<HttpClientResponse<O>, HttpClientRequest<I>> httpConfigurator) {
        HttpClientBuilder<I, O> builder = RxNetty.newHttpClientBuilder(host, port);
        return builder.pipelineConfigurator(ContextPipelineConfigurators.httpClientConfigurator(provider,
                                                                                                correlator,
                                                                                                httpConfigurator))
                .withChannelFactory(new HttpContextClientChannelFactory<I, O>(builder.getBootstrap(), correlator,
                                                                              builder.getEventsSubject()));
    }


    public static HttpServer<ByteBuf, ByteBuf> createHttpServer(int port, RequestHandler<ByteBuf, ByteBuf> requestHandler) {
        return newHttpServerBuilder(port, requestHandler, defaultRequestIdContextKeyName,
                                    DEFAULT_CORRELATOR).build();
    }

    public static HttpServer<ByteBuf, ByteBuf> createHttpServer(int port, RequestHandler<ByteBuf, ByteBuf> requestHandler,
                                                                String requestIdHeaderName) {
        return newHttpServerBuilder(port, requestHandler, requestIdHeaderName, DEFAULT_CORRELATOR).build();
    }

    public static HttpClient<ByteBuf, ByteBuf> createHttpClient(String host, int port) {
        return RxContexts.<ByteBuf, ByteBuf>newHttpClientBuilder(host, port, defaultRequestIdContextKeyName,
                                                                 DEFAULT_CORRELATOR).build();
    }

    public static HttpClient<ByteBuf, ByteBuf> createHttpClient(String host, int port, String requestIdHeaderName) {
        return RxContexts.<ByteBuf, ByteBuf>newHttpClientBuilder(host, port, requestIdHeaderName,
                                                                 DEFAULT_CORRELATOR).build();
    }

    public static <I, O> HttpServer<I, O> createHttpServer(int port, RequestHandler<I, O> requestHandler,
                                                           PipelineConfigurator<HttpServerRequest<I>, HttpServerResponse<O>> configurator) {
        HttpRequestIdProvider provider = new HttpRequestIdProvider(defaultRequestIdContextKeyName, DEFAULT_CORRELATOR);
        return newHttpServerBuilder(port, requestHandler, defaultRequestIdContextKeyName, DEFAULT_CORRELATOR)
                .pipelineConfigurator(ContextPipelineConfigurators
                                              .httpServerConfigurator(provider, DEFAULT_CORRELATOR, configurator)).build();
    }

    public static <I, O> HttpServer<I, O> createHttpServer(int port, RequestHandler<I, O> requestHandler,
                                                           String requestIdHeaderName,
                                                           PipelineConfigurator<HttpServerRequest<I>, HttpServerResponse<O>> configurator) {
        HttpRequestIdProvider provider = new HttpRequestIdProvider(requestIdHeaderName, DEFAULT_CORRELATOR);
        return newHttpServerBuilder(port, requestHandler, requestIdHeaderName, DEFAULT_CORRELATOR)
                .pipelineConfigurator(ContextPipelineConfigurators
                                              .httpServerConfigurator(provider, DEFAULT_CORRELATOR, configurator)).build();
    }

    public static <I, O> HttpClient<I, O> createHttpClient(String host, int port,
                                                           PipelineConfigurator<HttpClientResponse<O>, HttpClientRequest<I>> configurator) {
        HttpRequestIdProvider provider = new HttpRequestIdProvider(defaultRequestIdContextKeyName, DEFAULT_CORRELATOR);
        return RxContexts.<I, O>newHttpClientBuilder(host, port, defaultRequestIdContextKeyName,
                                                     DEFAULT_CORRELATOR)
                         .pipelineConfigurator(ContextPipelineConfigurators.httpClientConfigurator(provider,
                                                                                                   DEFAULT_CORRELATOR,
                                                                                                   configurator))
                         .build();
    }

    public static <I, O> HttpClient<I, O> createHttpClient(String host, int port, String requestIdHeaderName,
                                                           PipelineConfigurator<HttpClientResponse<O>, HttpClientRequest<I>> configurator) {
        HttpRequestIdProvider provider = new HttpRequestIdProvider(requestIdHeaderName, DEFAULT_CORRELATOR);
        return RxContexts.<I, O>newHttpClientBuilder(host, port, requestIdHeaderName, DEFAULT_CORRELATOR)
                         .pipelineConfigurator(ContextPipelineConfigurators.httpClientConfigurator(provider,
                                                                                                   DEFAULT_CORRELATOR,
                                                                                                   configurator))
                         .build();
    }

    /**
     * Default Context key name used for extracting the request Id. This is the default and will be useful to set a
     * system wide requestId key name so that it is consistent between all clients and the server created through
     * this factory class.
     *
     * @param name The name of the context key to be used as default.
     */
    public static void useRequestIdContextKey(String name) {
        defaultRequestIdContextKeyName = name;
    }
}
