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
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.metrics.Clock;
import io.reactivex.netty.metrics.MetricEventsSubject;
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
    private MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject;

    public ClientChannelFactoryImpl(Bootstrap clientBootstrap, MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject) {
        this.clientBootstrap = clientBootstrap;
        this.eventsSubject = eventsSubject;
    }

    public ClientChannelFactoryImpl(Bootstrap clientBootstrap) {
        this(clientBootstrap, new MetricEventsSubject<ClientMetricsEvent<?>>());
    }

    @Override
    public ChannelFuture connect(final Subscriber<? super ObservableConnection<I, O>> subscriber,
                                 RxClient.ServerInfo serverInfo,
                                 final ClientConnectionFactory<I, O,? extends ObservableConnection<I, O>> connectionFactory) {
        final long startTimeMillis = Clock.newStartTimeMillis();
        eventsSubject.onEvent(ClientMetricsEvent.CONNECT_START);
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
                try {
                    if (!future.isSuccess()) {
                        eventsSubject.onEvent(ClientMetricsEvent.CONNECT_FAILED, Clock.onEndMillis(startTimeMillis),
                                              future.cause());
                        subscriber.onError(future.cause());
                    } else {
                        eventsSubject.onEvent(ClientMetricsEvent.CONNECT_SUCCESS, Clock.onEndMillis(startTimeMillis));
                        ChannelPipeline pipeline = future.channel().pipeline();
                        ChannelHandlerContext ctx = pipeline.lastContext(); // The connection uses the context for write which should always start from the tail.
                        final ObservableConnection<I, O> newConnection = connectionFactory.newConnection(ctx);
                        ChannelHandler lifecycleHandler = pipeline.get(RxRequiredConfigurator.CONN_LIFECYCLE_HANDLER_NAME);
                        if (null == lifecycleHandler) {
                            onNewConnection(newConnection, subscriber);
                        } else {
                            @SuppressWarnings("unchecked")
                            ConnectionLifecycleHandler<I, O> handler = (ConnectionLifecycleHandler<I, O>) lifecycleHandler;
                            SslHandler sslHandler = pipeline.get(SslHandler.class);
                            if (null == sslHandler) {
                                handler.setConnection(newConnection);
                                onNewConnection(newConnection, subscriber);
                            } else {
                                sslHandler.handshakeFuture().addListener(new GenericFutureListener<Future<? super Channel>>() {
                                    @Override
                                    public void operationComplete(Future<? super Channel> future) throws Exception {
                                        onNewConnection(newConnection, subscriber);
                                    }
                                });
                            }
                        }
                    }
                } catch (Throwable throwable) {
                    subscriber.onError(throwable);
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

    @Override
    public void useMetricEventsSubject(MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject) {
        this.eventsSubject = eventsSubject;
    }
}
