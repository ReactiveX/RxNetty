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
import rx.Observer;
import rx.Subscription;
import rx.experimental.remote.RemoteObservableServer;
import rx.experimental.remote.RemoteObservableServer.RemoteServerOnSubscribeFunc;
import rx.netty.protocol.tcp.ProtocolHandler;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;

public class NettyServer<I, O> {

    public static <I, O> RemoteObservableServer<ObservableConnection<I, O>> createServer(
            final int port,
            final EventLoopGroup acceptorEventLoops,
            final EventLoopGroup workerEventLoops,
            final ProtocolHandler<I, O> handler) {

        return RemoteObservableServer.create(new RemoteServerOnSubscribeFunc<ObservableConnection<I, O>>() {

            @Override
            public Subscription onSubscribe(final Observer<? super ObservableConnection<I, O>> observer) {
                try {
                    ServerBootstrap b = new ServerBootstrap();
                    b.group(acceptorEventLoops, workerEventLoops)
                            .channel(NioServerSocketChannel.class)
                            // TODO allow ChannelOptions to be passed in
                            .option(ChannelOption.SO_BACKLOG, 100)
                            .handler(new ChannelDuplexHandler())
                            .childHandler(new ChannelInitializer<SocketChannel>() {
                                @Override
                                public void initChannel(SocketChannel ch) throws Exception {
                                    handler.configure(ch.pipeline());

                                    // add the handler that will emit responses to the observer
                                    ch.pipeline().addLast(new HandlerObserver<I, O>(observer));
                                }
                            });

                    // start the server
                    final ChannelFuture f = b.bind(port).sync();

                    // return a subscription that can shut down the server
                    return Subscriptions.create(new Action0() {

                        @Override
                        public void call() {
                            try {
                                f.channel().close().sync();
                            } catch (InterruptedException e) {
                                throw new RuntimeException("Failed to unsubscribe");
                            }
                        }

                    });
                } catch (Throwable e) {
                    observer.onError(e);
                    return Subscriptions.empty();
                }
            }
        });
    }
}
