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
package rx.netty.experimental;

import io.netty.buffer.ByteBuf;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Observable;
import rx.experimental.remote.RemoteObservableClient;
import rx.netty.experimental.impl.NettyClient;
import rx.netty.experimental.impl.NettyServer;
import rx.netty.experimental.impl.TcpConnection;
import rx.netty.experimental.protocol.ProtocolHandler;
import rx.netty.experimental.protocol.ProtocolHandlers;

public class RxNetty {

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

    public static Observable<TcpConnection<String, String>> createTcpServer(final int port, final EventLoopGroup acceptorEventLoops, final EventLoopGroup workerEventLoops) {
        return NettyServer.createServer(port, acceptorEventLoops, workerEventLoops, ProtocolHandlers.stringCodec());
    }

    public static Observable<TcpConnection<String, String>> createTcpServer(int port) {
        return createTcpServer(port, DEFAULT_EVENT_LOOPS.ACCEPTOR, DEFAULT_EVENT_LOOPS.WORKER);
    }

    public static <I, O> Observable<TcpConnection<I, O>> createTcpServer(int port, ProtocolHandler<I, O> handler) {
        return createTcpServer(port, DEFAULT_EVENT_LOOPS.ACCEPTOR, DEFAULT_EVENT_LOOPS.WORKER, handler);
    }

    public static <I, O> Observable<TcpConnection<I, O>> createTcpServer(
        final int port,
        final EventLoopGroup acceptorEventLoops,
        final EventLoopGroup workerEventLoops,
        ProtocolHandler<I, O> handler) {

        return NettyServer.createServer(port, acceptorEventLoops, workerEventLoops, handler);
    }

    public static RemoteObservableClient<TcpConnection<ByteBuf, String>> createTcpClient(final String host, final int port, final EventLoopGroup eventLoops) {
        return NettyClient.createClient(host, port, eventLoops, ProtocolHandlers.commandOnlyHandler());
    }

    public static RemoteObservableClient<TcpConnection<ByteBuf, String>> createTcpClient(String host, int port) {
        return RxNetty.createTcpClient(host, port, DEFAULT_EVENT_LOOPS.WORKER);

    }

    public static <I, O> RemoteObservableClient<TcpConnection<I, O>> createTcpClient(String host, int port, ProtocolHandler<I, O> handler) {
        return NettyClient.createClient(host, port, DEFAULT_EVENT_LOOPS.WORKER, handler);
    }
}
