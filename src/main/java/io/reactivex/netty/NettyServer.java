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
package io.reactivex.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.netty.spi.NettyPipelineConfigurator;
import io.reactivex.netty.spi.PipelineConfiguratorComposite;
import io.reactivex.netty.spi.RxNettyRequiredConfigurator;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.util.functions.Action1;

import static rx.Observable.OnSubscribeFunc;

public class NettyServer<I, O> {

    private ChannelFuture bindFuture;

    private enum ServerState {Created, Starting, Started, Shutdown}

    private final ServerBootstrap bootstrap;
    private final int port;
    /**
     * This should NOT be used directly. {@link #getPipelineConfiguratorForAChannel(Observer)} is the correct way of
     * getting the pipeline configurator.
     */
    private final NettyPipelineConfigurator incompleteConfigurator;
    private final AtomicReference<ServerState> serverStateRef;

    public NettyServer(ServerBootstrap bootstrap, int port, NettyPipelineConfigurator pipelineConfigurator) {
        this.bootstrap = bootstrap;
        this.port = port;
        incompleteConfigurator = pipelineConfigurator;
        serverStateRef = new AtomicReference<ServerState>(ServerState.Created);
    }

    /**
     * Starts this server now. The returned {@link Observable} is a cached observable which can be used to get a handle
     * of the {@link ObservableConnection} or just to shutdown this server.
     *
     * @return Observable to use the {@link ObservableConnection} or to shutdown.
     */
    public Observable<ObservableConnection<I, O>> startNow() {

        Observable<ObservableConnection<I, O>> cachedStartObservable = Observable.create(new OnSubscribeFunc<ObservableConnection<I, O>>() {

            @Override
            public Subscription onSubscribe(final Observer<? super ObservableConnection<I, O>> connectObserver) {

                if (!serverStateRef.compareAndSet(ServerState.Created, ServerState.Starting)) {
                    throw new IllegalStateException("Server already started");
                }

                bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        NettyPipelineConfigurator configurator = getPipelineConfiguratorForAChannel(connectObserver);
                        configurator.configureNewPipeline(ch.pipeline());
                    }
                });

                bindFuture = bootstrap.bind(port);

                serverStateRef.set(ServerState.Started); // It will come here only if this was the thread that transitioned to Starting

                // return a subscription that can shut down the server
                return new Subscription() {

                    @Override
                    public void unsubscribe() {
                        if (!serverStateRef.compareAndSet(ServerState.Started, ServerState.Shutdown)) {
                            connectObserver.onError(new IllegalStateException("The server is already shutdown."));
                        } else {
                            try {
                                bindFuture.channel().close().sync();
                            } catch (InterruptedException e) {
                                connectObserver.onError(new RuntimeException("Failed to shutdown the server.", e));
                            }
                        }
                    }
                };
            }
        }).cache();

        cachedStartObservable.subscribe(new Action1<Object>() {
                                            @Override
                                            public void call(Object o) {
                                                // No Op, since we cache the observable, any subscription to the
                                                // returned observable will return the same result.
                                            }
                                        }, new Action1<Throwable>() {
                                            @Override
                                            public void call(Throwable throwable) {
                                                // No Op, since we cache the observable, any subscription to the
                                                // returned observable will return the same result.
                                            }
                                        }
        );
        return cachedStartObservable;
    }

    @SuppressWarnings("fallthrough")
    public void waitTillShutdown() throws InterruptedException {
        ServerState serverState = serverStateRef.get();
        switch (serverState) {
            case Created:
            case Starting:
                throw new IllegalStateException("Server not started yet.");
            case Started:
                bindFuture.sync();
                bindFuture.channel().closeFuture().await();
                break;
            case Shutdown:
                // Nothing to do as it is already shutdown.
                break;
        }
    }

    protected NettyPipelineConfigurator getPipelineConfiguratorForAChannel(final Observer<? super ObservableConnection<I, O>> observer) {
        RxNettyRequiredConfigurator<I, O> requiredConfigurator = new RxNettyRequiredConfigurator<I, O>(observer);
        return new PipelineConfiguratorComposite(incompleteConfigurator, requiredConfigurator);
    }
}
