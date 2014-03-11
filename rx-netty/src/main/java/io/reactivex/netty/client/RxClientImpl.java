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
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.reactivex.netty.channel.ConnectionHandler;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.pipeline.PipelineConfiguratorComposite;
import io.reactivex.netty.pipeline.ReadTimeoutPipelineConfigurator;
import io.reactivex.netty.pipeline.RxRequiredConfigurator;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

import java.util.concurrent.TimeUnit;

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
     * This should NOT be used directly. {@link #getPipelineConfiguratorForAChannel(ClientConnectionHandler, PipelineConfigurator)} is the correct way of getting the pipeline configurator.
     */
    private final PipelineConfigurator<O, I> incompleteConfigurator;
    protected final ClientConfig clientConfig;
    private ChannelPool pool;

    public RxClientImpl(ServerInfo serverInfo, Bootstrap clientBootstrap, ClientConfig clientConfig) {
        this(serverInfo, clientBootstrap, null, clientConfig);
    }

    public RxClientImpl(ServerInfo serverInfo, Bootstrap clientBootstrap,
            PipelineConfigurator<O, I> pipelineConfigurator, ClientConfig clientConfig) {
        this(serverInfo, clientBootstrap, pipelineConfigurator, clientConfig, null);
    }
    
    public RxClientImpl(ServerInfo serverInfo, Bootstrap clientBootstrap,
            PipelineConfigurator<O, I> pipelineConfigurator, ClientConfig clientConfig, ChannelPool pool) {
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
        this.pool = pool;
    }

    /**
     * A lazy connect to the {@link ServerInfo} for this client. Every subscription to the returned {@link Observable} will create a fresh connection.
     * 
     * @return Observable for the connect. Every new subscription will create a fresh connection.
     */
    @Override
    public Observable<ObservableConnection<O, I>> connect() {
        return Observable.create(new OnSubscribe<ObservableConnection<O, I>>() {

            @Override
            public void call(final Subscriber<? super ObservableConnection<O, I>> subscriber) {
                try {
                    final ClientConnectionHandler clientConnectionHandler = new ClientConnectionHandler(subscriber);
                    if (pool != null) {
                        Observable<Channel> channelObservable = pool.requestChannel(serverInfo.getHost(), serverInfo.getPort(), clientBootstrap);
                        final Subscription channelSubscription = channelObservable.subscribe(new Observer<Channel>() {

                            @Override
                            public void onCompleted() {
                            }

                            @Override
                            public void onError(Throwable e) {
                                subscriber.onError(e);
                            }

                            @Override
                            public void onNext(Channel t) {
                                PipelineConfigurator<I, O> configurator = getPipelineConfiguratorForAChannel(clientConnectionHandler,
                                        incompleteConfigurator);
                                configurator.configureNewPipeline(t.pipeline());
                                t.attr(ObservableConnection.POOL_ATTR).set(pool);
                            }
                        });
                        subscriber.add(Subscriptions.create(new Action0() {
                            @Override
                            public void call() {
                                channelSubscription.unsubscribe();
                            }
                        }));
                    } else {
                        clientBootstrap.handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            public void initChannel(SocketChannel ch) throws Exception {
                                PipelineConfigurator<I, O> configurator = getPipelineConfiguratorForAChannel(clientConnectionHandler,
                                        incompleteConfigurator);
                                configurator.configureNewPipeline(ch.pipeline());
                            }
                        });

                        // make the connection
                        final ChannelFuture connectFuture =
                                clientBootstrap.connect(serverInfo.getHost(), serverInfo.getPort())
                                .addListener(clientConnectionHandler);

                        subscriber.add(Subscriptions.create(new Action0() {
                            @Override
                            public void call() {
                                if (!connectFuture.isDone()) {
                                    connectFuture.cancel(true); // Unsubscribe here means, no more connection is required. A close on connection is explicit.
                                }
                            }
                        }));
                    }
                } catch (Throwable e) {
                    subscriber.onError(e);
                }
            }
        });
    }

    protected PipelineConfigurator<I, O> getPipelineConfiguratorForAChannel(ClientConnectionHandler clientConnectionHandler,
            PipelineConfigurator<O, I> pipelineConfigurator) {
        RxRequiredConfigurator<O, I> requiredConfigurator = new RxRequiredConfigurator<O, I>(clientConnectionHandler);
        PipelineConfiguratorComposite<I, O> toReturn;
        if (null != pipelineConfigurator) {
            toReturn = new PipelineConfiguratorComposite<I, O>(pipelineConfigurator, requiredConfigurator);
        } else {
            toReturn = new PipelineConfiguratorComposite<I, O>(requiredConfigurator);
        }
        return toReturn;
    }

    protected class ClientConnectionHandler implements ConnectionHandler<O, I>, ChannelFutureListener {

        private final Observer<? super ObservableConnection<O, I>> connectionObserver;

        private ClientConnectionHandler(Observer<? super ObservableConnection<O, I>> connectionObserver) {
            this.connectionObserver = connectionObserver;
        }

        @Override
        public Observable<Void> handle(final ObservableConnection<O, I> newConnection) {
            return Observable.create(new OnSubscribe<Void>() {
                @Override
                public void call(Subscriber<? super Void> voidSub) {
                    connectionObserver.onNext(newConnection);
                    connectionObserver.onCompleted(); // The observer is no longer looking for any more connections.
                }
            });
        }

        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            if (!future.isSuccess()) {
                connectionObserver.onError(future.cause());
            } // onComplete() needs to be send after onNext(), calling it here will cause a race-condition between next & complete.
        }
    }
}