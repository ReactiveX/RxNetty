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
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.metrics.MetricEventsListener;
import io.reactivex.netty.metrics.MetricEventsListenerFactory;

/**
 * A builder to build an instance of {@link RxClientImpl}
 *
 * @author Nitesh Kant
 */
public class ClientBuilder<I, O> extends AbstractClientBuilder<I,O, ClientBuilder<I, O>, RxClient<I, O>> {

    public ClientBuilder(String host, int port) {
        this(host, port, new Bootstrap());
    }

    public ClientBuilder(String host, int port, Bootstrap bootstrap) {
        super(bootstrap, host, port, new UnpooledClientConnectionFactory<O, I>(),
              new ClientChannelFactoryImpl<O, I>(bootstrap));
        defaultTcpOptions();
    }

    public ClientBuilder(Bootstrap bootstrap, String host, int port,
                         ClientConnectionFactory<O, I, ? extends ObservableConnection<O, I>> connectionFactory,
                         ClientChannelFactory<O, I> factory) {
        super(bootstrap, host, port, connectionFactory, factory);
    }

    public ClientBuilder(Bootstrap bootstrap, String host, int port,
                         ConnectionPoolBuilder<O, I> poolBuilder) {
        super(bootstrap, host, port, poolBuilder);
    }

    @Override
    protected RxClient<I, O> createClient() {
        if (null == poolBuilder) {
            return new RxClientImpl<I, O>(getOrCreateName(), serverInfo, bootstrap, pipelineConfigurator, clientConfig,
                                          channelFactory, connectionFactory, eventsSubject);
        } else {
            return new RxClientImpl<I, O>(getOrCreateName(), serverInfo, bootstrap, pipelineConfigurator, clientConfig,
                                          poolBuilder, eventsSubject);
        }
    }

    @Override
    protected String generatedNamePrefix() {
        return "TcpClient-";
    }

    @Override
    protected Class<? extends SocketChannel> defaultSocketChannelClass() {
        if (RxNetty.isUsingNativeTransport()) {
            return EpollSocketChannel.class;
        }
        return super.defaultSocketChannelClass();
    }

    @Override
    protected EventLoopGroup defaultEventloop(Class<? extends Channel> socketChannel) {
        return RxNetty.getRxEventLoopProvider().globalClientEventLoop(true); // get native eventloop if configured.
    }

    @Override
    protected MetricEventsListener<? extends ClientMetricsEvent<ClientMetricsEvent.EventType>>
    newMetricsListener(MetricEventsListenerFactory factory, RxClient<I, O> client) {
        return factory.forTcpClient(client);
    }
}
