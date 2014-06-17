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
package io.reactivex.netty.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.handler.ssl.SslHandler;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.pipeline.RxRequiredConfigurator;
import rx.Subscriber;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

/**
 * A factory to create netty channels for clients.
 *
 * @param <I> The type of the object that is read from the channel created by this factory.
 * @param <O> The type of objects that are written to the channel created by this factory.
 * @author Nitesh Kant
 */
public class ClientChannelFactoryImpl<I, O> implements ClientChannelFactory<I, O> {

    protected final Bootstrap clientBootstrap;

    public ClientChannelFactoryImpl(Bootstrap clientBootstrap) {
        this.clientBootstrap = clientBootstrap;
    }

    @Override
    public ChannelFuture connect(final Subscriber<? super ObservableConnection<I, O>> subscriber,
                                 RxClient.ServerInfo serverInfo,
                                 final ClientConnectionFactory<I, O, ? extends ObservableConnection<I, O>> connectionFactory) {
        final ChannelFuture connectFuture = clientBootstrap.connect(serverInfo.getHost(), serverInfo.getPort());

        subscriber.add(Subscriptions.create(new Action0() {
            @Override
            public void call() {
                if (!connectFuture.isDone()) {
                    connectFuture.cancel(true); // Unsubscribe here means, no more connection is required. A close on connection is explicit.
                }
            }
        }));

        connectFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (!future.isSuccess()) {
                    subscriber.onError(future.cause());
                } else {
                    ChannelPipeline pipeline = future.channel().pipeline();
                    ChannelHandlerContext ctx = pipeline.lastContext(); // The connection uses the context for write which should always start from the tail.
                    final ObservableConnection<I, O> newConnection = connectionFactory.newConnection(ctx);
                    ChannelHandler lifecycleHandler = pipeline.get(RxRequiredConfigurator.CONN_LIFECYCLE_HANDLER_NAME);
                    if (null == lifecycleHandler) {
                        onNewConnection(newConnection, subscriber);
                    } else {
                        @SuppressWarnings("unchecked")
                        ConnectionLifecycleHandler<I, O> handler = (ConnectionLifecycleHandler<I, O>) lifecycleHandler;
                        ChannelHandler sslHandler = pipeline.get(SslHandler.class);
                        if (null == sslHandler) {
                            handler.setConnection(newConnection);
                            onNewConnection(newConnection, subscriber);
                        } else {
                            handler.setConnectionAction(new Action0() {
                                @Override
                                public void call() {
                                    onNewConnection(newConnection, subscriber);
                                }
                            });
                        }
                    }
                }
            }
        });
        return connectFuture;
    }

    @Override
    public void onNewConnection(ObservableConnection<I, O> newConnection,
                                Subscriber<? super ObservableConnection<I, O>> subscriber) {
        subscriber.onNext(newConnection);
        subscriber.onCompleted(); // The observer is no longer looking for any more connections.
    }
}
