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
package rx.netty.impl;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.concurrent.TimeUnit;

import rx.netty.protocol.tcp.ProtocolHandler;
import rx.util.functions.Action1;

public class NettyServer<I, O> {

    public static <I, O> NettyServer<I, O> create(final int port,
            EventLoopGroup acceptorEventLoops,
            EventLoopGroup workerEventLoops,
            ProtocolHandler<I, O> handler) {
        return new NettyServer<I, O>(port, acceptorEventLoops, workerEventLoops, handler);
    }

    private final int port;
    private final EventLoopGroup acceptorEventLoops;
    private final EventLoopGroup workerEventLoops;
    private final ProtocolHandler<I, O> handler;
    // protected by synchronized start/shutdown access
    private ChannelFuture f;

    private NettyServer(final int port,
            EventLoopGroup acceptorEventLoops,
            EventLoopGroup workerEventLoops,
            ProtocolHandler<I, O> handler) {

        this.port = port;
        this.acceptorEventLoops = acceptorEventLoops;
        this.workerEventLoops = workerEventLoops;
        this.handler = handler;
    }

    public NettyServerInstance onConnect(Action1<ObservableConnection<I, O>> connection) {
        return new NettyServerInstance(connection);
    }

    public class NettyServerInstance {

        final Action1<ObservableConnection<I, O>> onConnect;

        public NettyServerInstance(Action1<ObservableConnection<I, O>> onConnect) {
            this.onConnect = onConnect;
        }

        public synchronized void start() {
            if (f != null) {
                throw new IllegalStateException("Server already started.");
            }
            ServerBootstrap b = new ServerBootstrap().group(acceptorEventLoops, workerEventLoops)
                    .channel(NioServerSocketChannel.class)
                    // TODO allow ChannelOptions to be passed in
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .handler(new ChannelDuplexHandler())
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            handler.configure(ch.pipeline());

                            // add the handler that will emit responses to the observer
                            ch.pipeline().addLast(new ConnectionHandler<I, O>(onConnect));
                        }
                    });

            // start the server
            try {
                f = b.bind(port).sync();
            } catch (InterruptedException e) {
                Thread.interrupted();
                throw new RuntimeException("Interrupted while starting server.", e);
            }
        }
        
        public synchronized void startAndAwait() throws InterruptedException {
            start();
            acceptorEventLoops.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        }

        public synchronized void shutdown() {
            try {
                f.channel().close().sync();
            } catch (InterruptedException e) {
                Thread.interrupted();
                throw new RuntimeException("Interrupted while stopping server.", e);
            }
        }
    }

}
