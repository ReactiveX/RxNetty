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
import io.netty.channel.ChannelInitializer;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.channel.ObservableConnectionFactory;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.pipeline.PipelineConfiguratorComposite;
import io.reactivex.netty.pipeline.RxRequiredConfigurator;
import rx.Subscriber;

/**
 * A factory to create netty channels for clients.
 *
 * @param <I> The type of the object that is read from the channel created by this factory.
 * @param <O> The type of objects that are written to the channel created by this factory.
 *
 * @author Nitesh Kant
 */
public class ClientChannelFactoryImpl<I, O> implements ClientChannelFactory<I,O> {

    protected final Bootstrap clientBootstrap;
    protected final ObservableConnectionFactory<I, O> connectionFactory;
    protected final RxClient.ServerInfo serverInfo;

    public ClientChannelFactoryImpl(Bootstrap clientBootstrap, ObservableConnectionFactory<I, O> connectionFactory,
                                    RxClient.ServerInfo serverInfo) {
        this.clientBootstrap = clientBootstrap;
        this.connectionFactory = connectionFactory;
        this.serverInfo = serverInfo;
    }

    @Override
    public ChannelFuture connect(final ClientConnectionHandler<I, O> connectionHandler,
                                 PipelineConfigurator<I, O> pipelineConfigurator) {

        final PipelineConfigurator<I, O> configurator = getPipelineConfiguratorForAChannel(connectionHandler,
                                                                                           pipelineConfigurator);
        // make the connection
        clientBootstrap.handler(new ChannelInitializer<Channel>() {
            @Override
            public void initChannel(Channel ch) throws Exception {
                configurator.configureNewPipeline(ch.pipeline());
            }
        });

        final ChannelFuture connectFuture = _connect().addListener(connectionHandler);

        connectionHandler.connectionAttempted(connectFuture);

        return connectFuture;
    }

    @Override
    public ClientConnectionHandler<I, O> newConnectionHandler(Subscriber<? super ObservableConnection<I, O>> subscriber) {
        return new ClientConnectionHandler<I, O>(subscriber);
    }

    protected ChannelFuture _connect() {
        return clientBootstrap.connect(serverInfo.getHost(), serverInfo.getPort());
    }


    protected PipelineConfigurator<I, O> getPipelineConfiguratorForAChannel(ClientConnectionHandler<I, O> connHandler,
                                                                            PipelineConfigurator<I, O> pipelineConfigurator) {
        RxRequiredConfigurator<I, O> requiredConfigurator = new RxRequiredConfigurator<I, O>(connHandler, connectionFactory);
        PipelineConfiguratorComposite<I, O> toReturn;
        if (null != pipelineConfigurator) {
            toReturn = new PipelineConfiguratorComposite<I, O>(pipelineConfigurator, requiredConfigurator);
        } else {
            toReturn = new PipelineConfiguratorComposite<I, O>(requiredConfigurator);
        }
        return toReturn;
    }
}
