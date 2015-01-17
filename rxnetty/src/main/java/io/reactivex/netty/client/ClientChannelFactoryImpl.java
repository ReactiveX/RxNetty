/*
 * Copyright 2015 Netflix, Inc.
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
import io.netty.channel.ChannelPipeline;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.metrics.Clock;
import io.reactivex.netty.metrics.MetricEventsSubject;
import io.reactivex.netty.pipeline.RxRequiredConfigurator;
import io.reactivex.netty.pipeline.ssl.SslCompletionHandler;
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
            public void operationComplete(final ChannelFuture future) throws Exception {
                try {
                    if (!future.isSuccess()) {
                        _onConnectFailed(future.cause(), subscriber, startTimeMillis);
                    } else {
                        ChannelPipeline pipeline = future.channel().pipeline();
                        ChannelHandler lifecycleHandler = pipeline.get(RxRequiredConfigurator.CONN_LIFECYCLE_HANDLER_NAME);
                        if (null == lifecycleHandler) {
                            _newConnection(connectionFactory, future.channel(), subscriber, startTimeMillis);
                        } else {
                            @SuppressWarnings("unchecked")
                            ConnectionLifecycleHandler<I, O> handler = (ConnectionLifecycleHandler<I, O>) lifecycleHandler;
                            SslCompletionHandler sslHandler = pipeline.get(SslCompletionHandler.class);
                            if (null == sslHandler) {
                                ObservableConnection<I, O> conn = _newConnection(connectionFactory, future.channel(),
                                                                                 subscriber, startTimeMillis);
                                handler.setConnection(conn);
                            } else {
                                sslHandler.sslCompletionStatus().subscribe(new Subscriber<Void>() {
                                    @Override
                                    public void onCompleted() {
                                        _newConnection(connectionFactory, future.channel(), subscriber,
                                                       startTimeMillis);
                                    }

                                    @Override
                                    public void onError(Throwable e) {
                                        _onConnectFailed(e, subscriber, startTimeMillis);
                                    }

                                    @Override
                                    public void onNext(Void aVoid) {
                                        // No Op.
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

    private ObservableConnection<I, O> _newConnection(ClientConnectionFactory<I, O, ? extends ObservableConnection<I, O>> connectionFactory,
                                                      Channel channel,
                                                      Subscriber<? super ObservableConnection<I, O>> subscriber,
                                                      long startTimeMillis) {
        final ObservableConnection<I, O> newConnection = connectionFactory.newConnection(channel);
        eventsSubject.onEvent(ClientMetricsEvent.CONNECT_SUCCESS, Clock.onEndMillis(startTimeMillis));
        onNewConnection(newConnection, subscriber);
        return newConnection;
    }

    private void _onConnectFailed(Throwable cause, Subscriber<? super ObservableConnection<I, O>> subscriber,
                                  long startTimeMillis) {
        eventsSubject.onEvent(ClientMetricsEvent.CONNECT_FAILED, Clock.onEndMillis(startTimeMillis), cause);
        subscriber.onError(cause);
    }
}
