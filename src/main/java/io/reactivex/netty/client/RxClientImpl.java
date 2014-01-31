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
package io.reactivex.netty.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.reactivex.netty.ObservableConnection;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.pipeline.PipelineConfiguratorComposite;
import io.reactivex.netty.pipeline.ReadTimeoutPipelineConfigurator;
import io.reactivex.netty.pipeline.RxRequiredConfigurator;
import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

import java.util.concurrent.TimeUnit;

/**
 * The base class for all connection oriented clients inside RxNetty.
 *
 * @param <I> The request object type for this client.
 * @param <O> The response object type for this client.
 */
public class RxClientImpl<I, O> implements RxClient<I,O> {

    private final ServerInfo serverInfo;
    private final Bootstrap clientBootstrap;
    /**
     * This should NOT be used directly. {@link #getPipelineConfiguratorForAChannel(Observer)} is the correct way of
     * getting the pipeline configurator.
     */
    private final PipelineConfigurator<O, I> incompleteConfigurator;
    protected final ClientConfig clientConfig;

    public RxClientImpl(ServerInfo serverInfo, Bootstrap clientBootstrap,
                        PipelineConfigurator<O, I> pipelineConfigurator, ClientConfig clientConfig) {
        this.clientConfig = clientConfig;
        this.serverInfo = serverInfo;
        this.clientBootstrap = clientBootstrap;
        if (clientConfig.isReadTimeoutSet()) {
            ReadTimeoutPipelineConfigurator readTimeoutConfigurator =
                    new ReadTimeoutPipelineConfigurator(clientConfig.getReadTimeoutInMillis(), TimeUnit.MILLISECONDS);
            pipelineConfigurator = new PipelineConfiguratorComposite<O, I>(pipelineConfigurator,
                                                                           readTimeoutConfigurator);
        }
        incompleteConfigurator = pipelineConfigurator;
    }

    /**
     * A lazy connect to the {@link ServerInfo} for this client. Every subscription to the returned {@link Observable}
     * will create a fresh connection.
     *
     * @return Observable for the connect. Every new subscription will create a fresh connection.
     */
    @Override
    public Observable<ObservableConnection<O, I>> connect() {
        return Observable.create(new OnSubscribeFunc<ObservableConnection<O, I>>() {

            @Override
            public Subscription onSubscribe(final Observer<? super ObservableConnection<O, I>> observer) {
                try {
                    clientBootstrap.handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            PipelineConfigurator<I, O> configurator = getPipelineConfiguratorForAChannel(observer);
                            configurator.configureNewPipeline(ch.pipeline());
                        }
                    });

                    // make the connection
                    final ChannelFuture f = clientBootstrap.connect(serverInfo.getHost(), serverInfo.getPort()).sync();

                    // return a subscription that can shut down the connection
                    return new Subscription() {

                        @Override
                        public void unsubscribe() {
                            f.channel().close(); // Async close, no need to wait for close or give any callback for failures.
                        }
                    };
                } catch (Throwable e) {
                    observer.onError(e);
                    return Subscriptions.empty();
                }
            }
        });
    }

    protected PipelineConfigurator<I, O> getPipelineConfiguratorForAChannel(final Observer<? super ObservableConnection<O, I>> observer) {
        RxRequiredConfigurator<O, I> requiredConfigurator = new RxRequiredConfigurator<O, I>(observer);
        return new PipelineConfiguratorComposite<I, O>(incompleteConfigurator, requiredConfigurator);
    }

}