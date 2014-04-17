/*
 * Copyright 2014 Netflix, Inc.
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
package io.reactivex.netty.client;

import io.netty.bootstrap.Bootstrap;
import io.reactivex.netty.channel.ObservableConnectionFactory;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.channel.UnpooledConnectionFactory;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.pipeline.PipelineConfiguratorComposite;
import io.reactivex.netty.pipeline.ReadTimeoutPipelineConfigurator;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The base class for all connection oriented clients inside RxNetty.
 * 
 * @param <I>
 *            The request object type for this client.
 * @param <O>
 *            The response object type for this client.
 */
public class RxClientImpl<I, O> implements RxClient<I, O> {

    protected final ServerInfo serverInfo;
    protected final Bootstrap clientBootstrap;

    /**
     * This should NOT be used directly. {@link ClientChannelFactoryImpl#getPipelineConfiguratorForAChannel(ClientConnectionHandler, PipelineConfigurator)}
     * is the correct way of getting the pipeline configurator.
     */
    private final PipelineConfigurator<O, I> incompleteConfigurator;
    protected final PipelineConfigurator<O, I> originalPipelineConfigurator;
    protected final ClientChannelFactory<O, I> channelFactory;
    protected final ClientConfig clientConfig;
    protected ConnectionPool<O, I> pool;
    private final AtomicBoolean isShutdown = new AtomicBoolean();

    public RxClientImpl(ServerInfo serverInfo, Bootstrap clientBootstrap, ClientConfig clientConfig) {
        this(serverInfo, clientBootstrap, null, clientConfig);
    }

    public RxClientImpl(ServerInfo serverInfo, Bootstrap clientBootstrap, PipelineConfigurator<O, I> pipelineConfigurator,
                        ClientConfig clientConfig) {
        this(serverInfo, clientBootstrap, pipelineConfigurator, clientConfig, null);
    }
    
    public RxClientImpl(ServerInfo serverInfo, Bootstrap clientBootstrap, PipelineConfigurator<O, I> pipelineConfigurator,
                        ClientConfig clientConfig, ConnectionPool<O, I> pool) {
        if (null == clientBootstrap) {
            throw new NullPointerException("Client bootstrap can not be null.");
        }
        if (null == serverInfo) {
            throw new NullPointerException("Server info can not be null.");
        }
        if (null == clientConfig) {
            throw new NullPointerException("Client config can not be null.");
        }
        this.clientConfig = clientConfig;
        this.serverInfo = serverInfo;
        this.clientBootstrap = clientBootstrap;
        this.pool = pool;
        if (null != pool) {
            channelFactory = _newChannelFactory(this.serverInfo, this.clientBootstrap, this.pool);
        } else {
            channelFactory = _newChannelFactory(this.serverInfo, this.clientBootstrap,
                                                new UnpooledConnectionFactory<O, I>());
        }

        if (pool instanceof ConnectionPoolImpl) {
            ((ConnectionPoolImpl<O, I>) pool).setChannelFactory(channelFactory);
        }
        originalPipelineConfigurator = pipelineConfigurator;

        if (clientConfig.isReadTimeoutSet()) {
            ReadTimeoutPipelineConfigurator readTimeoutConfigurator =
                    new ReadTimeoutPipelineConfigurator(clientConfig.getReadTimeoutInMillis(), TimeUnit.MILLISECONDS);
            if (null != pipelineConfigurator) {
                pipelineConfigurator = new PipelineConfiguratorComposite<O, I>(pipelineConfigurator,
                                                                               readTimeoutConfigurator);
            } else {
                pipelineConfigurator = new PipelineConfiguratorComposite<O, I>(readTimeoutConfigurator);
            }
        }

        incompleteConfigurator = pipelineConfigurator;
    }

    /**
     * A lazy connect to the {@link ServerInfo} for this client. Every subscription to the returned {@link Observable} will create a fresh connection.
     *
     * @return Observable for the connect. Every new subscription will create a fresh connection.
     */
    @Override
    public Observable<ObservableConnection<O, I>> connect() {
        if (isShutdown.get()) {
            return Observable.error(new IllegalStateException("Client is already shutdown."));
        }

        if (null != pool) {
            return pool.acquire(incompleteConfigurator);
        }

        return Observable.create(new OnSubscribe<ObservableConnection<O, I>>() {
            @Override
            public void call(final Subscriber<? super ObservableConnection<O, I>> subscriber) {
                try {
                    channelFactory.connect(subscriber, incompleteConfigurator);
                } catch (Throwable throwable) {
                    subscriber.onError(throwable);
                }
            }
        });
    }

    @Override
    public void shutdown() {
        if (!isShutdown.compareAndSet(false, true)) {
            return;
        }

        try {
            if (null != pool) {
                pool.shutdown();
            }
        } finally {
            clientBootstrap.group().shutdownGracefully();
        }
    }

    protected ClientChannelFactory<O, I> _newChannelFactory(ServerInfo serverInfo, Bootstrap clientBootstrap,
                                                            ObservableConnectionFactory<O, I> connectionFactory) {
        return new ClientChannelFactoryImpl<O, I>(clientBootstrap, connectionFactory, serverInfo);
    }

    @Override
    public Observable<StateChangeEvent> stateChangeObservable() {
        if (null == pool) {
            return Observable.empty();
        }
        return pool.stateChangeObservable();
    }

    @Override
    public PoolStats getStats() {
        if (null == pool) {
            return null;
        }
        return pool.getStats();
    }
}
