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

import io.netty.buffer.ByteBuf;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.reactivex.netty.http.HttpClient;
import io.reactivex.netty.http.HttpNettyPipelineConfigurator;
import io.reactivex.netty.http.ObservableHttpResponse;
import io.reactivex.netty.spi.NettyPipelineConfigurator;
import rx.Observable;

import java.net.URI;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class RxNetty {

    private RxNetty() {
    }

    public static <T> Observable<ObservableHttpResponse<T>> createHttpRequest(String uri) {
        return null;
    }

    public static <T> Observable<ObservableHttpResponse<T>> createHttpRequest(URI uri) {
        //return createHttpRequest(uri, new HttpNettyPipelineConfiguratorAdapter<T>(), DEFAULT_HTTP_CLIENT.CLIENT);
        return null;
    }

    public static <T> Observable<ObservableHttpResponse<T>> createHttpRequest(String uri, HttpNettyPipelineConfigurator<T> protocolHandler) {
        //return createHttpRequest(uri, protocolHandler, DEFAULT_HTTP_CLIENT.CLIENT);
        return null;
    }

    public static <T> Observable<ObservableHttpResponse<T>> createHttpRequest(URI uri, HttpNettyPipelineConfigurator<T> protocolHandler) {
        //return createHttpRequest(uri, protocolHandler, DEFAULT_HTTP_CLIENT.CLIENT);
        return null;
    }

    public static <T> Observable<ObservableHttpResponse<T>> createHttpRequest(String uri, HttpNettyPipelineConfigurator<T> protocolHandler, HttpClient client) {
        //return client.execute(ValidatedFullHttpRequest.get(uri), protocolHandler);
        return null;
    }

    public static <T> Observable<ObservableHttpResponse<T>> createHttpRequest(URI uri, HttpNettyPipelineConfigurator<T> protocolHandler, HttpClient client) {
        //return client.execute(ValidatedFullHttpRequest.get(uri), protocolHandler);
        return null;
    }

    public static NettyServer<String, String> createTcpServer(final int port, final EventLoopGroup acceptorEventLoops, final EventLoopGroup workerEventLoops) {
        return createTcpServer(port, acceptorEventLoops, workerEventLoops, ProtocolHandlers.stringCodec());
    }

    public static NettyServer<String, String> createTcpServer(int port) {
        return createTcpServer(port, DEFAULT_EVENT_LOOPS.ACCEPTOR, DEFAULT_EVENT_LOOPS.WORKER);
    }

    public static <I, O> NettyServer<I, O> createTcpServer(int port, NettyPipelineConfigurator handler) {
        return createTcpServer(port, DEFAULT_EVENT_LOOPS.ACCEPTOR, DEFAULT_EVENT_LOOPS.WORKER, handler);
    }

    public static <I, O> NettyServer<I, O> createTcpServer(final int port, final EventLoopGroup acceptorEventLoops,
                                                           final EventLoopGroup workerEventLoops,
                                                           NettyPipelineConfigurator pipelineConfigurator) {
        return new ServerBuilder<I, O>(port)
                .eventLoops(acceptorEventLoops, workerEventLoops)
                .pipelineConfigurator(pipelineConfigurator)
                .build();
    }

    public static <I, O> Observable<ObservableConnection<O, I>> createTcpClient(String host, int port, NettyPipelineConfigurator handler) {
        return new ClientBuilder<I, O>(host, port)
                .pipelineConfigurator(handler)
                .build().connect();
    }

    private static final class DEFAULT_EVENT_LOOPS {
        private static final ThreadFactory THREAD_FACTORY = new ThreadFactory() {

            private final AtomicInteger counter = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "rx-netty-nio-event-loop-" + counter.incrementAndGet());
                t.setDaemon(true);
                return t;
            }

        };
        private static final NioEventLoopGroup ACCEPTOR = new NioEventLoopGroup(2, THREAD_FACTORY);
        private static final NioEventLoopGroup WORKER = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors(), THREAD_FACTORY);
    }
}
