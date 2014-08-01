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
import io.netty.channel.ChannelInitializer;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.metrics.MetricEventsListener;
import io.reactivex.netty.metrics.MetricEventsSubject;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.Subscription;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The base class for all connection oriented clients inside RxNetty.
 * 
 * @param <I> The request object type for this client.
 * @param <O> The response object type for this client.
 */
public class RxClientImpl<I, O> implements RxClient<I, O> {

    protected final String name;
    protected final ServerInfo serverInfo;
    protected final Bootstrap clientBootstrap;
    protected final PipelineConfigurator<O, I> pipelineConfigurator;
    protected final ClientChannelFactory<O, I> channelFactory;
    protected final ClientConnectionFactory<O, I, ? extends ObservableConnection<O, I>> connectionFactory;
    protected final ClientConfig clientConfig;
    protected final MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject;
    protected final ConnectionPool<O, I> pool;
    private final AtomicBoolean isShutdown = new AtomicBoolean();

    public RxClientImpl(String name, ServerInfo serverInfo, Bootstrap clientBootstrap,
                        PipelineConfigurator<O, I> pipelineConfigurator,
                        ClientConfig clientConfig, ClientChannelFactory<O, I> channelFactory,
                        ClientConnectionFactory<O, I, ? extends ObservableConnection<O, I>> connectionFactory,
                        MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject) {
        if (null == name) {
            throw new NullPointerException("Name can not be null.");
        }
        if (null == clientBootstrap) {
            throw new NullPointerException("Client bootstrap can not be null.");
        }
        if (null == serverInfo) {
            throw new NullPointerException("Server info can not be null.");
        }
        if (null == clientConfig) {
            throw new NullPointerException("Client config can not be null.");
        }
        if (null == connectionFactory) {
            throw new NullPointerException("Connection factory can not be null.");
        }
        if (null == channelFactory) {
            throw new NullPointerException("Channel factory can not be null.");
        }
        this.name = name;
        pool = null;
        this.eventsSubject = eventsSubject;
        this.clientConfig = clientConfig;
        this.serverInfo = serverInfo;
        this.clientBootstrap = clientBootstrap;
        this.connectionFactory = connectionFactory;
        this.connectionFactory.useMetricEventsSubject(eventsSubject);
        this.channelFactory = channelFactory;
        this.channelFactory.useMetricEventsSubject(eventsSubject);
        this.pipelineConfigurator = pipelineConfigurator;
        final PipelineConfigurator<O, I> configurator = adaptPipelineConfigurator(pipelineConfigurator, clientConfig,
                                                                                  eventsSubject);
        this.clientBootstrap.handler(new ChannelInitializer<Channel>() {
            @Override
            public void initChannel(Channel ch) throws Exception {
                configurator.configureNewPipeline(ch.pipeline());
            }
        });
    }

    public RxClientImpl(String name, ServerInfo serverInfo, Bootstrap clientBootstrap,
                        PipelineConfigurator<O, I> pipelineConfigurator,
                        ClientConfig clientConfig, ConnectionPoolBuilder<O, I> poolBuilder,
                        MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject) {
        if (null == name) {
            throw new NullPointerException("Name can not be null.");
        }
        if (null == clientBootstrap) {
            throw new NullPointerException("Client bootstrap can not be null.");
        }
        if (null == serverInfo) {
            throw new NullPointerException("Server info can not be null.");
        }
        if (null == clientConfig) {
            throw new NullPointerException("Client config can not be null.");
        }
        if (null == poolBuilder) {
            throw new NullPointerException("Pool builder can not be null.");
        }
        this.name = name;
        this.eventsSubject = eventsSubject;
        this.clientConfig = clientConfig;
        this.serverInfo = serverInfo;
        this.clientBootstrap = clientBootstrap;
        this.pipelineConfigurator = pipelineConfigurator;
        final PipelineConfigurator<O, I> configurator = adaptPipelineConfigurator(pipelineConfigurator, clientConfig,
                                                                                  eventsSubject);
        this.clientBootstrap.handler(new ChannelInitializer<Channel>() {
            @Override
            public void initChannel(Channel ch) throws Exception {
                configurator.configureNewPipeline(ch.pipeline());
            }
        });
        pool = poolBuilder.build();
        channelFactory = poolBuilder.getChannelFactory();
        connectionFactory = poolBuilder.getConnectionFactory();
    }

    /**
     * A lazy connect to the {@link RxClient.ServerInfo} for this client. Every subscription to the returned {@link Observable}
     * will create a fresh connection.
     *
     * @return Observable for the connect. Every new subscription will create a fresh connection.
     */
    @Override
    public Observable<ObservableConnection<O, I>> connect() {
        if (isShutdown.get()) {
            return Observable.error(new IllegalStateException("Client is already shutdown."));
        }

        Observable<ObservableConnection<O, I>> toReturn;
        if (null != pool) {
            toReturn = pool.acquire();
        } else {
            toReturn = Observable.create(new OnSubscribe<ObservableConnection<O, I>>() {
                @Override
                public void call(final Subscriber<? super ObservableConnection<O, I>> subscriber) {
                    try {
                        channelFactory.connect(subscriber, serverInfo, connectionFactory);
                    } catch (Throwable throwable) {
                        subscriber.onError(throwable);
                    }
                }
            });
        }

        return toReturn.take(1); // We only need one connection, even if the underlying source emits multiple.
    }

    @Override
    public void shutdown() {
        if (!isShutdown.compareAndSet(false, true)) {
            return;
        }

        if (null != pool) {
            pool.shutdown();
        }
    }

    @Override
    public String name() {
        return name;
    }

    protected PipelineConfigurator<O, I> adaptPipelineConfigurator(PipelineConfigurator<O, I> pipelineConfigurator,
                                                                   ClientConfig clientConfig,
                                                                   MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject) {
        return PipelineConfigurators.createClientConfigurator(pipelineConfigurator, clientConfig, eventsSubject);
    }

    @Override
    public Subscription subscribe(MetricEventsListener<? extends ClientMetricsEvent<?>> listener) {
        return eventsSubject.subscribe(listener);
    }
}
