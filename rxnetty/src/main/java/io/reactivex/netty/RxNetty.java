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
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.channel.ConnectionHandler;
import io.reactivex.netty.channel.ContentTransformer;
import io.reactivex.netty.channel.RxEventLoopProvider;
import io.reactivex.netty.channel.SingleNioLoopProvider;
import io.reactivex.netty.client.ClientBuilder;
import io.reactivex.netty.client.RxClient;
import io.reactivex.netty.metrics.MetricEventsListenerFactory;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.protocol.http.client.CompositeHttpClient;
import io.reactivex.netty.protocol.http.client.CompositeHttpClientBuilder;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientBuilder;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.HttpServerBuilder;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import io.reactivex.netty.protocol.http.websocket.WebSocketClientBuilder;
import io.reactivex.netty.protocol.http.websocket.WebSocketServerBuilder;
import io.reactivex.netty.protocol.udp.client.UdpClientBuilder;
import io.reactivex.netty.protocol.udp.server.UdpServer;
import io.reactivex.netty.protocol.udp.server.UdpServerBuilder;
import io.reactivex.netty.server.RxServer;
import io.reactivex.netty.server.ServerBuilder;
import rx.Observable;

import java.net.URI;
import java.net.URISyntaxException;

import static io.reactivex.netty.client.MaxConnectionsBasedStrategy.DEFAULT_MAX_CONNECTIONS;

public final class RxNetty {

    private static volatile RxEventLoopProvider rxEventLoopProvider =
            new SingleNioLoopProvider(1, Runtime.getRuntime().availableProcessors());

    private static final CompositeHttpClient<ByteBuf, ByteBuf> globalClient =
            new CompositeHttpClientBuilder<ByteBuf, ByteBuf>().withMaxConnections(DEFAULT_MAX_CONNECTIONS).build();

    private static MetricEventsListenerFactory metricEventsListenerFactory;

    private static volatile boolean usingNativeTransport;

    private RxNetty() {
    }

    public static <I, O> UdpServerBuilder<I, O> newUdpServerBuilder(int port, ConnectionHandler<I, O> connectionHandler) {
        UdpServerBuilder<I, O> builder = new UdpServerBuilder<I, O>(port, connectionHandler)
                .enableWireLogging(LogLevel.DEBUG);
        if (null != metricEventsListenerFactory) {
            builder.withMetricEventsListenerFactory(metricEventsListenerFactory);
        }
        return builder;
    }

    public static <I, O> UdpClientBuilder<I, O> newUdpClientBuilder(String host, int port) {
        UdpClientBuilder<I, O> builder =
                new UdpClientBuilder<I, O>(host, port).channel(NioDatagramChannel.class)
                                                      .enableWireLogging(LogLevel.DEBUG)
                                                      .eventloop(getRxEventLoopProvider().globalClientEventLoop());
        if (null != metricEventsListenerFactory) {
            builder.withMetricEventsListenerFactory(metricEventsListenerFactory);
        }
        return builder;
    }

    public static <I, O> UdpServer<I, O> createUdpServer(final int port, PipelineConfigurator<I, O> pipelineConfigurator,
                                                         ConnectionHandler<I, O> connectionHandler) {
        return newUdpServerBuilder(port, connectionHandler).pipelineConfigurator(pipelineConfigurator).build();
    }

    public static <I, O> RxClient<I, O> createUdpClient(String host, int port,
                                                        PipelineConfigurator<O, I> pipelineConfigurator) {
        return RxNetty.<I, O>newUdpClientBuilder(host, port).pipelineConfigurator(pipelineConfigurator).build();
    }

    public static UdpServer<DatagramPacket, DatagramPacket> createUdpServer(final int port,
                                                                            ConnectionHandler<DatagramPacket, DatagramPacket> connectionHandler) {
        return new UdpServerBuilder<DatagramPacket, DatagramPacket>(port, connectionHandler).build();
    }

    public static RxClient<DatagramPacket, DatagramPacket> createUdpClient(String host, int port) {
        return RxNetty.<DatagramPacket, DatagramPacket>newUdpClientBuilder(host, port).build();
    }

    public static <I, O> ServerBuilder<I, O> newTcpServerBuilder(int port, ConnectionHandler<I, O> connectionHandler) {
        ServerBuilder<I, O> builder =
                new ServerBuilder<I, O>(port, connectionHandler).enableWireLogging(LogLevel.DEBUG);
        if (null != metricEventsListenerFactory) {
            builder.withMetricEventsListenerFactory(metricEventsListenerFactory);
        }
        return builder;
    }

    public static <I, O> RxServer<I, O> createTcpServer(final int port, PipelineConfigurator<I, O> pipelineConfigurator,
                                                        ConnectionHandler<I, O> connectionHandler) {
        return newTcpServerBuilder(port, connectionHandler).pipelineConfigurator(pipelineConfigurator).build();
    }

    public static <I, O> ClientBuilder<I, O> newTcpClientBuilder(String host, int port) {
        ClientBuilder<I, O> builder = new ClientBuilder<I, O>(host, port).enableWireLogging(LogLevel.DEBUG);
        if (null != metricEventsListenerFactory) {
            builder.withMetricEventsListenerFactory(metricEventsListenerFactory);
        }
        return builder;
    }

    public static <I, O> RxClient<I, O> createTcpClient(String host, int port, PipelineConfigurator<O, I> configurator) {
        return RxNetty.<I, O>newTcpClientBuilder(host, port).pipelineConfigurator(configurator).build();
    }

    public static RxServer<ByteBuf, ByteBuf> createTcpServer(final int port,
                                                             ConnectionHandler<ByteBuf, ByteBuf> connectionHandler) {
        return new ServerBuilder<ByteBuf, ByteBuf>(port, connectionHandler).build();
    }

    public static RxClient<ByteBuf, ByteBuf> createTcpClient(String host, int port) {
        return RxNetty.<ByteBuf, ByteBuf>newTcpClientBuilder(host, port).build();
    }

    public static <I, O> HttpServerBuilder<I, O> newHttpServerBuilder(int port, RequestHandler<I, O> requestHandler) {
        HttpServerBuilder<I, O> builder =
                new HttpServerBuilder<I, O>(port, requestHandler).enableWireLogging(LogLevel.DEBUG);
        if (null != metricEventsListenerFactory) {
            builder.withMetricEventsListenerFactory(metricEventsListenerFactory);
        }
        return builder;
    }

    public static <I, O> HttpClientBuilder<I, O> newHttpClientBuilder(String host, int port) {
        HttpClientBuilder<I, O> builder =
                new HttpClientBuilder<I, O>(host, port).withMaxConnections(DEFAULT_MAX_CONNECTIONS)
                                                       .enableWireLogging(LogLevel.DEBUG);
        if (null != metricEventsListenerFactory) {
            builder.withMetricEventsListenerFactory(metricEventsListenerFactory);
        }
        return builder;
    }

    public static HttpServer<ByteBuf, ByteBuf> createHttpServer(int port, RequestHandler<ByteBuf, ByteBuf> requestHandler) {
        return newHttpServerBuilder(port, requestHandler).build();
    }

    public static HttpClient<ByteBuf, ByteBuf> createHttpClient(String host, int port) {
        return RxNetty.<ByteBuf, ByteBuf>newHttpClientBuilder(host, port).build();
    }

    public static <I, O> HttpServer<I, O> createHttpServer(int port,
                                                           RequestHandler<I, O> requestHandler,
                                                           PipelineConfigurator<HttpServerRequest<I>, HttpServerResponse<O>> configurator) {
        return newHttpServerBuilder(port, requestHandler).pipelineConfigurator(configurator).build();
    }

    public static <I, O> HttpClient<I, O> createHttpClient(String host, int port,
                                                           PipelineConfigurator<HttpClientResponse<O>,
                                                           HttpClientRequest<I>> configurator) {
        return RxNetty.<I, O>newHttpClientBuilder(host, port).pipelineConfigurator(configurator).build();
    }

    public static Observable<HttpClientResponse<ByteBuf>> createHttpRequest(HttpClientRequest<ByteBuf> request) {
        RxClient.ServerInfo serverInfo;
        try {
            serverInfo = getServerInfoFromRequest(request);
        } catch (URISyntaxException e) {
            return Observable.error(e);
        }
        return globalClient.submit(serverInfo, request);
    }

    public static Observable<HttpClientResponse<ByteBuf>> createHttpRequest(HttpClientRequest<ByteBuf> request,
                                                                             HttpClient.HttpClientConfig config) {
        RxClient.ServerInfo serverInfo;
        try {
            serverInfo = getServerInfoFromRequest(request);
        } catch (URISyntaxException e) {
            return Observable.error(e);
        }
        return globalClient.submit(serverInfo, request, config);
    }

    public static Observable<HttpClientResponse<ByteBuf>> createHttpGet(String uri) {
        return createHttpRequest(HttpClientRequest.createGet(uri));
    }

    public static Observable<HttpClientResponse<ByteBuf>> createHttpPost(String uri, Observable<ByteBuf> content) {
        return createHttpRequest(HttpClientRequest.createPost(uri).withContentSource(content));
    }

    public static Observable<HttpClientResponse<ByteBuf>> createHttpPut(String uri, Observable<ByteBuf> content) {
        return createHttpRequest(HttpClientRequest.createPut(uri).withContentSource(content));
    }

    public static <T> Observable<HttpClientResponse<ByteBuf>> createHttpPost(String uri, Observable<T> content,
                                                                             ContentTransformer<T> transformer) {
        return createHttpRequest(HttpClientRequest.createPost(uri).withRawContentSource(content, transformer));
    }

    public static <T> Observable<HttpClientResponse<ByteBuf>> createHttpPut(String uri, Observable<T> content,
                                                                            ContentTransformer<T> transformer) {
        return createHttpRequest(HttpClientRequest.createPut(uri).withRawContentSource(content, transformer));
    }

    public static Observable<HttpClientResponse<ByteBuf>> createHttpDelete(String uri) {
        return createHttpRequest(HttpClientRequest.createDelete(uri));
    }

    public static <I extends WebSocketFrame, O extends WebSocketFrame> WebSocketClientBuilder<I, O> newWebSocketClientBuilder(String host, int port) {
        return new WebSocketClientBuilder<I, O>(host, port).enableWireLogging(LogLevel.DEBUG);
    }

    public static <I extends WebSocketFrame, O extends WebSocketFrame> WebSocketServerBuilder<I, O>
    newWebSocketServerBuilder(int port, ConnectionHandler<I, O> connectionHandler) {
        return new WebSocketServerBuilder<I, O>(port, connectionHandler).enableWireLogging(LogLevel.DEBUG);
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

    public static void useMetricListenersFactory(MetricEventsListenerFactory factory) {
        metricEventsListenerFactory = factory;
    }

    public static RxEventLoopProvider getRxEventLoopProvider() {
        return rxEventLoopProvider;
    }

    /**
     * A global flag to start using netty's <a href="https://github.com/netty/netty/wiki/Native-transports">native protocol</a>
     * if applicable for a client or server.
     *
     * <b>This does not evaluate whether the native transport is available for the OS or not.</b>
     *
     * So, this method should be called conditionally when the caller is sure that the OS supports the native protocol.
     *
     * Alternatively, this can be done selectively per client and server instance by doing the following:
     *
     * <h2>Http Server</h2>
     <pre>
       RxNetty.newHttpServerBuilder(8888, new RequestHandler<Object, Object>() {

            public Observable<Void> handle(HttpServerRequest<Object> request, HttpServerResponse<Object> response) {
            return null;
            }
       }).channel(EpollServerSocketChannel.class)
         .eventLoop(new EpollEventLoopGroup());
     </pre>
     *
     * <h2>Http Client</h2>
     *
     <pre>
     RxNetty.newHttpClientBuilder("localhost", 8888)
            .channel(EpollSocketChannel.class)
            .eventloop(new EpollEventLoopGroup());
     </pre>
     */
    public static void useNativeTransportIfApplicable() {
        usingNativeTransport = true;
    }

    /**
     * A global flag to disable the effects of calling {@link #useNativeTransportIfApplicable()}
     */
    public static void disableNativeTransport() {
        usingNativeTransport = false;
    }

    public static boolean isUsingNativeTransport() {
        return usingNativeTransport;
    }

    private static RxClient.ServerInfo getServerInfoFromRequest(HttpClientRequest<ByteBuf> request)
            throws URISyntaxException {
        URI uri = new URI(request.getUri());
        final String host = uri.getHost();
        if (null != host) {
            int port = uri.getPort();
            if (port < 0) {
                String scheme = uri.getScheme();
                if (null != scheme) {
                    if ("http".equals(scheme)) {
                        port = 80;
                    } else if ("https".equals(scheme)) {
                        port = 443;
                    }
                }
            }
            return new RxClient.ServerInfo(host, port);
        }
        return globalClient.getDefaultServer();
    }
}
