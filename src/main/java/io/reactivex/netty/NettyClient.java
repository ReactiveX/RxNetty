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

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.reactivex.netty.spi.NettyPipelineConfigurator;
import io.reactivex.netty.spi.PipelineConfiguratorComposite;
import io.reactivex.netty.spi.RxNettyRequiredConfigurator;
import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

public class NettyClient<I, O> {

    private final ServerInfo serverInfo;
    private final Bootstrap clientBootstrap;
    /**
     * This should NOT be used directly. {@link #getPipelineConfiguratorForAChannel(Observer)} is the correct way of
     * getting the pipeline configurator.
     */
    private final NettyPipelineConfigurator incompleteConfigurator;

    public NettyClient(ServerInfo serverInfo, Bootstrap clientBootstrap, NettyPipelineConfigurator pipelineConfigurator) {
        this.serverInfo = serverInfo;
        this.clientBootstrap = clientBootstrap;
        incompleteConfigurator = pipelineConfigurator;
    }

    /**
     * A lazy connect to the {@link ServerInfo} for this client. Every subscription to the returned {@link Observable}
     * will create a fresh connection.
     *
     * @return Observable for the connect. Every new subscription will create a fresh connection.
     */
    public Observable<ObservableConnection<O, I>> connect() {
        return Observable.create(new OnSubscribeFunc<ObservableConnection<O, I>>() {

            @Override
            public Subscription onSubscribe(final Observer<? super ObservableConnection<O, I>> observer) {
                try {
                    clientBootstrap.handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            NettyPipelineConfigurator configurator = getPipelineConfiguratorForAChannel(observer);
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

    protected NettyPipelineConfigurator getPipelineConfiguratorForAChannel(final Observer<? super ObservableConnection<O, I>> observer) {
        RxNettyRequiredConfigurator<O, I> requiredConfigurator = new RxNettyRequiredConfigurator<O, I>(observer);
        return new PipelineConfiguratorComposite(incompleteConfigurator, requiredConfigurator);
    }

    public static class ServerInfo {

        private final String host;
        private final int port;

        public ServerInfo(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }
    }
}