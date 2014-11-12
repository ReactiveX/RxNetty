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

package io.reactivex.netty.protocol.udp.server;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.reactivex.netty.channel.ConnectionHandler;
import io.reactivex.netty.metrics.MetricEventsListener;
import io.reactivex.netty.metrics.MetricEventsListenerFactory;
import io.reactivex.netty.pipeline.ssl.SSLEngineFactory;
import io.reactivex.netty.server.AbstractServerBuilder;
import io.reactivex.netty.server.ServerMetricsEvent;

/**
 * @author Nitesh Kant
 */
public class UdpServerBuilder<I, O> extends AbstractServerBuilder<I, O, Bootstrap, Channel, UdpServerBuilder<I, O>,
        UdpServer<I, O>> {

    public UdpServerBuilder(int port, ConnectionHandler<I, O> connectionHandler) {
        this(port, connectionHandler, new Bootstrap());
    }

    public UdpServerBuilder(int port, ConnectionHandler<I, O> connectionHandler, Bootstrap bootstrap) {
        super(port, bootstrap, connectionHandler);
    }

    @Override
    protected Class<? extends Channel> defaultServerChannelClass() {
        return NioDatagramChannel.class;
    }

    @Override
    public UdpServerBuilder<I, O> defaultChannelOptions() {
        channelOption(ChannelOption.SO_BROADCAST, true);
        return super.defaultChannelOptions();
    }

    @Override
    public UdpServerBuilder<I, O> withSslEngineFactory(SSLEngineFactory sslEngineFactory) {
        throw new IllegalArgumentException("SSL protocol is not applicable to UDP ");
    }

    @Override
    protected UdpServer<I, O> createServer() {
        if (null != pipelineConfigurator) {
            return new UdpServer<I, O>(serverBootstrap, port, pipelineConfigurator, connectionHandler, eventExecutorGroup);
        } else {
            return new UdpServer<I, O>(serverBootstrap, port, connectionHandler, eventExecutorGroup);
        }
    }

    @Override
    protected MetricEventsListener<ServerMetricsEvent<?>>
    newMetricsListener(MetricEventsListenerFactory factory, UdpServer<I, O> server) {
        return factory.forUdpServer(server);
    }
}
