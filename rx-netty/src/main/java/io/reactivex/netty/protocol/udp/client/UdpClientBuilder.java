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
package io.reactivex.netty.protocol.udp.client;

import io.netty.bootstrap.Bootstrap;
import io.reactivex.netty.client.AbstractClientBuilder;
import io.reactivex.netty.client.ClientChannelFactoryImpl;
import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.client.ConnectionPoolBuilder;
import io.reactivex.netty.client.RxClient;
import io.reactivex.netty.client.RxClientImpl;
import io.reactivex.netty.metrics.MetricEventsListener;
import io.reactivex.netty.metrics.MetricEventsListenerFactory;
import io.reactivex.netty.pipeline.ssl.SSLEngineFactory;

import java.net.InetSocketAddress;

/**
 * A builder to build an instance of {@link RxClientImpl}
 *
 * @author Nitesh Kant
 */
public class UdpClientBuilder<I, O> extends AbstractClientBuilder<I,O, UdpClientBuilder<I, O>, RxClient<I, O>> {

    public UdpClientBuilder(String host, int port) {
        this(host, port, new Bootstrap());
    }

    public UdpClientBuilder(String host, int port, Bootstrap bootstrap) {
        super(bootstrap, host, port, new UdpClientConnectionFactory<O, I>(new InetSocketAddress(host, port)),
              new ClientChannelFactoryImpl<O, I>(bootstrap));
        defaultUdpOptions();
    }

    @Override
    protected RxClient<I, O> createClient() {
        return new UdpClient<I, O>(getOrCreateName(), serverInfo, bootstrap, pipelineConfigurator, clientConfig,
                                   channelFactory, eventsSubject);
    }

    @Override
    public UdpClientBuilder<I, O> withSslEngineFactory(SSLEngineFactory sslEngineFactory) {
        throw new IllegalArgumentException("SSL protocol is not applicable to UDP ");
    }

    @Override
    protected ConnectionPoolBuilder<O, I> getPoolBuilder(boolean createNew) {
        ConnectionPoolBuilder<O, I> builder = super.getPoolBuilder(false);
        if (null == builder && createNew) {
            throw new IllegalStateException("Connection pools are not allowed for UDP clients.");
        }

        if (null != builder) {
            throw new IllegalStateException("Connection pools are not allowed for UDP clients.");
        }

        return builder;
    }

    @Override
    protected String generatedNamePrefix() {
        return "UdpClient-";
    }

    @Override
    protected MetricEventsListener<? extends ClientMetricsEvent<?>>
    newMetricsListener(MetricEventsListenerFactory factory, RxClient<I, O> client) {
        return factory.forUdpClient((UdpClient<I, O>) client);
    }
}
