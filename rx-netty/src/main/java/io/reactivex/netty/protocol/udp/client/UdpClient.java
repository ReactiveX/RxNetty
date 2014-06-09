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
import io.reactivex.netty.client.ClientChannelFactory;
import io.reactivex.netty.client.ClientConnectionFactory;
import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.client.RxClient;
import io.reactivex.netty.client.RxClientImpl;
import io.reactivex.netty.metrics.MetricEventsSubject;
import io.reactivex.netty.pipeline.PipelineConfigurator;

import java.net.InetSocketAddress;

/**
 * An implementation of {@link RxClient} for UDP/IP
 *
 * @author Nitesh Kant
 */
public class UdpClient<I, O> extends RxClientImpl<I, O> {

    public UdpClient(String name, ServerInfo serverInfo, Bootstrap bootstrap,
                     PipelineConfigurator<O, I> pipelineConfigurator,
                     ClientConfig clientConfig, ClientChannelFactory<O, I> clientChannelFactory,
                     MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject) {
        this(name, serverInfo, bootstrap, pipelineConfigurator, clientConfig, clientChannelFactory,
             new UdpClientConnectionFactory<O, I>(new InetSocketAddress(serverInfo.getHost(), serverInfo.getPort())),
             eventsSubject);
    }

    public UdpClient(String name, ServerInfo serverInfo, Bootstrap bootstrap,
                     PipelineConfigurator<O, I> pipelineConfigurator,
                     ClientConfig clientConfig, ClientChannelFactory<O, I> channelFactory,
                     ClientConnectionFactory<O, I, UdpClientConnection<O, I>> connectionFactory,
                     MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject) {
        super(name, serverInfo, bootstrap, pipelineConfigurator, clientConfig, channelFactory, connectionFactory,
              eventsSubject);
    }
}
