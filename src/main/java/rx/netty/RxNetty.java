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
package rx.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

import java.net.URI;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Observable;
import rx.netty.impl.NettyClient;
import rx.netty.impl.NettyServer;
import rx.netty.impl.ObservableConnection;
import rx.netty.protocol.http.HttpProtocolHandler;
import rx.netty.protocol.http.HttpProtocolHandlerAdapter;
import rx.netty.protocol.http.ObservableHttpClient;
import rx.netty.protocol.http.ObservableHttpResponse;
import rx.netty.protocol.http.ValidatedFullHttpRequest;
import rx.netty.protocol.tcp.ProtocolHandler;
import rx.netty.protocol.tcp.ProtocolHandlers;

public class RxNetty {

    public static <T> Observable<ObservableHttpResponse<T>> createHttpRequest(String uri) {
        return createHttpRequest(uri, new HttpProtocolHandlerAdapter<T>(), DEFAULT_HTTP_CLIENT.CLIENT);
    }

    public static <T> Observable<ObservableHttpResponse<T>> createHttpRequest(URI uri) {
        return createHttpRequest(uri, new HttpProtocolHandlerAdapter<T>(), DEFAULT_HTTP_CLIENT.CLIENT);
    }

    public static <T> Observable<ObservableHttpResponse<T>> createHttpRequest(String uri, HttpProtocolHandler<T> protocolHandler) {
        return createHttpRequest(uri, protocolHandler, DEFAULT_HTTP_CLIENT.CLIENT);
    }

    public static <T> Observable<ObservableHttpResponse<T>> createHttpRequest(URI uri, HttpProtocolHandler<T> protocolHandler) {
        return createHttpRequest(uri, protocolHandler, DEFAULT_HTTP_CLIENT.CLIENT);
    }

    public static <T> Observable<ObservableHttpResponse<T>> createHttpRequest(String uri, HttpProtocolHandler<T> protocolHandler, ObservableHttpClient client) {
        return client.execute(ValidatedFullHttpRequest.get(uri), protocolHandler);
    }

    public static <T> Observable<ObservableHttpResponse<T>> createHttpRequest(URI uri, HttpProtocolHandler<T> protocolHandler, ObservableHttpClient client) {
        return client.execute(ValidatedFullHttpRequest.get(uri), protocolHandler);
    }

    public static NettyServer<String, String> createTcpServer(final int port, final EventLoopGroup acceptorEventLoops, final EventLoopGroup workerEventLoops) {
        return createTcpServer(port, acceptorEventLoops, workerEventLoops, ProtocolHandlers.stringCodec());
    }

    public static NettyServer<String, String> createTcpServer(int port) {
        return createTcpServer(port, DEFAULT_EVENT_LOOPS.ACCEPTOR, DEFAULT_EVENT_LOOPS.WORKER);
    }

    public static <I, O> NettyServer<I, O> createTcpServer(int port, ProtocolHandler<I, O> handler) {
        return createTcpServer(port, DEFAULT_EVENT_LOOPS.ACCEPTOR, DEFAULT_EVENT_LOOPS.WORKER, handler);
    }

    public static <I, O> NettyServer<I, O> createTcpServer(final int port, final EventLoopGroup acceptorEventLoops, final EventLoopGroup workerEventLoops, ProtocolHandler<I, O> handler) {
        return NettyServer.create(port, acceptorEventLoops, workerEventLoops, handler);
    }

    public static Observable<ObservableConnection<ByteBuf, String>> createTcpClient(final String host, final int port, final EventLoopGroup eventLoops) {
        return NettyClient.createClient(host, port, eventLoops, ProtocolHandlers.commandOnlyHandler());
    }

    public static <I, O> Observable<ObservableConnection<I, O>> createTcpClient(String host, int port, ProtocolHandler<I, O> handler) {
        return NettyClient.createClient(host, port, DEFAULT_EVENT_LOOPS.WORKER, handler);
    }

    private static class DEFAULT_EVENT_LOOPS {
        private static ThreadFactory THREAD_FACTORY = new ThreadFactory() {

            private final AtomicInteger counter = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "rx-netty-nio-event-loop-" + counter.incrementAndGet());
                t.setDaemon(true);
                return t;
            }

        };
        private static NioEventLoopGroup ACCEPTOR = new NioEventLoopGroup(2, THREAD_FACTORY);
        private static NioEventLoopGroup WORKER = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors(), THREAD_FACTORY);
    }

    private static class DEFAULT_HTTP_CLIENT {
        private static ObservableHttpClient CLIENT = ObservableHttpClient.newBuilder().build(DEFAULT_EVENT_LOOPS.WORKER);
    }
}
