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

package io.reactivex.netty.protocol.http.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.client.AbstractClientBuilder;
import io.reactivex.netty.client.ClientChannelFactory;
import io.reactivex.netty.client.ClientChannelFactoryImpl;
import io.reactivex.netty.client.ClientConnectionFactory;
import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.client.ConnectionPoolBuilder;
import io.reactivex.netty.client.PoolLimitDeterminationStrategy;
import io.reactivex.netty.client.RxClient;
import io.reactivex.netty.client.UnpooledClientConnectionFactory;
import io.reactivex.netty.metrics.MetricEventsListener;
import io.reactivex.netty.metrics.MetricEventsListenerFactory;
import io.reactivex.netty.pipeline.PipelineConfigurators;

/**
 * @param <I> The type of the content of request.
 * @param <O> The type of the content of response.
 *
 * @author Nitesh Kant
 */
public class CompositeHttpClientBuilder<I, O>
        extends AbstractClientBuilder<HttpClientRequest<I>, HttpClientResponse<O>, CompositeHttpClientBuilder<I, O>,
        CompositeHttpClient<I, O>> {

    private static final RxClient.ServerInfo defaultServer = new RxClient.ServerInfo();

    public CompositeHttpClientBuilder() {
        this(new Bootstrap());
    }

    public CompositeHttpClientBuilder(Bootstrap bootstrap) {
        this(bootstrap, new UnpooledClientConnectionFactory<HttpClientResponse<O>, HttpClientRequest<I>>(),
             new ClientChannelFactoryImpl<HttpClientResponse<O>, HttpClientRequest<I>>(bootstrap));
    }

    public CompositeHttpClientBuilder(Bootstrap bootstrap,
                                      ClientConnectionFactory<HttpClientResponse<O>, HttpClientRequest<I>,
                                              ? extends ObservableConnection<HttpClientResponse<O>, HttpClientRequest<I>>> connectionFactory,
                                      ClientChannelFactory<HttpClientResponse<O>, HttpClientRequest<I>> factory) {
        super(bootstrap, defaultServer.getHost(), defaultServer.getPort(), connectionFactory, factory);
        clientConfig = HttpClient.HttpClientConfig.Builder.newDefaultConfig();
        pipelineConfigurator(PipelineConfigurators.<I, O>httpClientConfigurator());
    }

    public CompositeHttpClientBuilder(Bootstrap bootstrap,
                                      ConnectionPoolBuilder<HttpClientResponse<O>, HttpClientRequest<I>> poolBuilder) {
        super(bootstrap, defaultServer.getHost(), defaultServer.getPort(), poolBuilder);
        clientConfig = HttpClient.HttpClientConfig.Builder.newDefaultConfig();
        pipelineConfigurator(PipelineConfigurators.<I, O>httpClientConfigurator());
    }

    @Override
    public CompositeHttpClientBuilder<I, O> withConnectionPoolLimitStrategy(PoolLimitDeterminationStrategy strategy) {
        if (strategy instanceof CloneablePoolLimitDeterminationStrategy) {
            return withConnectionPoolLimitStrategy((CloneablePoolLimitDeterminationStrategy) strategy);
        } else {
            throw new IllegalArgumentException("Only " + CloneablePoolLimitDeterminationStrategy.class.getName() +
                                               " strategy implementations are allowed.");
        }
    }

    public CompositeHttpClientBuilder<I, O> withConnectionPoolLimitStrategy(CloneablePoolLimitDeterminationStrategy strategy) {
        super.withConnectionPoolLimitStrategy(strategy);
        return this;
    }

    @Override
    public CompositeHttpClientBuilder<I, O> withMaxConnections(int maxConnections) {
        return super.withMaxConnections(maxConnections);
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
    protected CompositeHttpClient<I, O> createClient() {
        if (null == poolBuilder) {
            return new CompositeHttpClient<I, O>(getOrCreateName(), serverInfo, bootstrap, pipelineConfigurator,
                                                 clientConfig, channelFactory, connectionFactory, eventsSubject);
        } else {
            return new CompositeHttpClient<I, O>(getOrCreateName(), serverInfo, bootstrap, pipelineConfigurator,
                                                 clientConfig, poolBuilder, eventsSubject);
        }
    }

    @Override
    protected String generatedNamePrefix() {
        return "HttpClient-";
    }

    @Override
    protected MetricEventsListener<? extends ClientMetricsEvent<?>>
    newMetricsListener(MetricEventsListenerFactory factory, CompositeHttpClient<I, O> client) {
        return factory.forHttpClient(client);
    }

    public interface CloneablePoolLimitDeterminationStrategy extends PoolLimitDeterminationStrategy {

        CloneablePoolLimitDeterminationStrategy copy();
    }
}
