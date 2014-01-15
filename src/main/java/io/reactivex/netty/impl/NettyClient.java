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
package io.reactivex.netty.impl;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import io.reactivex.netty.protocol.tcp.ProtocolHandler;
import rx.subscriptions.Subscriptions;

public class NettyClient<I, O> {

    public static <I, O> Observable<ObservableConnection<I, O>> createClient(final String host, final int port, final EventLoopGroup eventLoops, final ProtocolHandler<I, O> handler) {
        return Observable.create(new OnSubscribeFunc<ObservableConnection<I, O>>() {

            @Override
            public Subscription onSubscribe(final Observer<? super ObservableConnection<I, O>> observer) {
                try {
                    Bootstrap b = new Bootstrap();
                    b.group(eventLoops)
                            .channel(NioSocketChannel.class)
                            // TODO allow ChannelOptions to be passed in
                            .option(ChannelOption.TCP_NODELAY, true)
                            .handler(new ChannelInitializer<SocketChannel>() {
                                @Override
                                public void initChannel(SocketChannel ch) throws Exception {
                                    handler.configure(ch.pipeline());

                                    // add the handler that will emit responses to the observer
                                    ch.pipeline()
                                            .addLast(new ConnectionHandler<I, O>(observer));
                                }
                            });

                    // make the connection
                    final ChannelFuture f = b.connect(host, port).sync();

                    // return a subscription that can shut down the connection
                    return new Subscription() {

                        @Override
                        public void unsubscribe() {
                            try {
                                f.channel().close().sync();
                            } catch (InterruptedException e) {
                                observer.onError(new RuntimeException("Failed to unsubscribe", e));
                            }
                        }
                    };
                } catch (Throwable e) {
                    observer.onError(e);
                    return Subscriptions.empty();
                }
            }
        });
    }
}