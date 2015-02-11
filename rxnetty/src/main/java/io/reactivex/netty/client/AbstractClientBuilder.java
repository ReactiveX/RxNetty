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
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.metrics.MetricEventsListener;
import io.reactivex.netty.metrics.MetricEventsListenerFactory;
import io.reactivex.netty.metrics.MetricEventsSubject;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.pipeline.ssl.SSLEngineFactory;

import java.util.concurrent.ScheduledExecutorService;

/**
 * @author Nitesh Kant
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractClientBuilder<I, O, B extends AbstractClientBuilder, C extends RxClient<I, O>> {

    private String name;
    protected final RxClientImpl.ServerInfo serverInfo;
    protected final Bootstrap bootstrap;
    protected final ClientConnectionFactory<O, I, ? extends ObservableConnection<O, I>> connectionFactory;
    protected ClientChannelFactory<O, I> channelFactory;
    protected ConnectionPoolBuilder<O, I> poolBuilder;
    protected PipelineConfigurator<O, I> pipelineConfigurator;
    protected Class<? extends Channel> socketChannel;
    protected EventLoopGroup eventLoopGroup;
    protected RxClient.ClientConfig clientConfig;
    protected LogLevel wireLogginLevel;
    protected MetricEventsListenerFactory eventListenersFactory;
    protected MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject;
    private SSLEngineFactory sslEngineFactory;

    protected AbstractClientBuilder(Bootstrap bootstrap, String host, int port,
                                    ClientConnectionFactory<O, I, ? extends ObservableConnection<O, I>> connectionFactory,
                                    ClientChannelFactory<O, I> factory) {
        eventsSubject = new MetricEventsSubject<ClientMetricsEvent<?>>();
        factory.useMetricEventsSubject(eventsSubject);
        connectionFactory.useMetricEventsSubject(eventsSubject);
        this.bootstrap = bootstrap;
        serverInfo = new RxClientImpl.ServerInfo(host, port);
        clientConfig = RxClient.ClientConfig.Builder.newDefaultConfig();
        this.connectionFactory = connectionFactory;
        channelFactory = factory;
        poolBuilder = null;
        defaultChannelOptions();
    }

    protected AbstractClientBuilder(Bootstrap bootstrap, String host, int port, ConnectionPoolBuilder<O, I> poolBuilder) {
        eventsSubject = new MetricEventsSubject<ClientMetricsEvent<?>>();
        this.bootstrap = bootstrap;
        this.poolBuilder = poolBuilder;
        serverInfo = new RxClientImpl.ServerInfo(host, port);
        clientConfig = RxClient.ClientConfig.Builder.newDefaultConfig();
        connectionFactory = null;
        channelFactory = null;
        defaultChannelOptions();
    }

    public B defaultChannelOptions() {
        return channelOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
    }

    public B defaultTcpOptions() {
        defaultChannelOptions();
        channelOption(ChannelOption.SO_KEEPALIVE, true);
        return channelOption(ChannelOption.TCP_NODELAY, true);
    }

    public B defaultUdpOptions() {
        defaultChannelOptions();
        return channelOption(ChannelOption.SO_BROADCAST, true);
    }

    public B pipelineConfigurator(PipelineConfigurator<O, I> pipelineConfigurator) {
        this.pipelineConfigurator = pipelineConfigurator;
        return returnBuilder();
    }

    public <T> B channelOption(ChannelOption<T> option, T value) {
        bootstrap.option(option, value);
        return returnBuilder();
    }

    public B channel(Class<? extends Channel> socketChannel) {
        this.socketChannel = socketChannel;
        return returnBuilder();
    }

    public B eventloop(EventLoopGroup eventLoopGroup) {
        this.eventLoopGroup = eventLoopGroup;
        return returnBuilder();
    }

    public B config(RxClient.ClientConfig clientConfig) {
        this.clientConfig = clientConfig;
        return returnBuilder();
    }

    public B withMaxConnections(int maxConnections) {
        getPoolBuilder(true).withMaxConnections(maxConnections);
        return returnBuilder();
    }

    public B withIdleConnectionsTimeoutMillis(long idleConnectionsTimeoutMillis) {
        getPoolBuilder(true).withIdleConnectionsTimeoutMillis(idleConnectionsTimeoutMillis);
        return returnBuilder();
    }

    public B withConnectionPoolLimitStrategy(PoolLimitDeterminationStrategy limitDeterminationStrategy) {
        getPoolBuilder(true).withConnectionPoolLimitStrategy(limitDeterminationStrategy);
        return returnBuilder();
    }

    public B withPoolIdleCleanupScheduler(ScheduledExecutorService poolIdleCleanupScheduler) {
        getPoolBuilder(true).withPoolIdleCleanupScheduler(poolIdleCleanupScheduler);
        return returnBuilder();
    }

    public B withNoIdleConnectionCleanup() {
        getPoolBuilder(true).withNoIdleConnectionCleanup();
        return returnBuilder();
    }

    public PipelineConfigurator<O, I> getPipelineConfigurator() {
        return pipelineConfigurator;
    }

    public B appendPipelineConfigurator(PipelineConfigurator<O, I> additionalConfigurator) {
        return pipelineConfigurator(PipelineConfigurators.composeConfigurators(pipelineConfigurator,
                                                                               additionalConfigurator));
    }

    public B withChannelFactory(ClientChannelFactory<O, I> factory) {
        ConnectionPoolBuilder<O, I> builder = getPoolBuilder(false);
        if (null != builder) {
            builder.withChannelFactory(factory);
        } else {
            channelFactory = factory;
        }
        return returnBuilder();
    }

    /**
     * Enables wire level logs (all events received by netty) to be logged at the passed {@code wireLogginLevel}. <br/>
     *
     * Since, in most of the production systems, the logging level is set to {@link LogLevel#WARN} or
     * {@link LogLevel#ERROR}, if this wire level logging is required for all requests (not at all recommended as this
     * logging is very verbose), the passed level must be {@link LogLevel#WARN} or {@link LogLevel#ERROR} respectively. <br/>
     *
     * It is recommended to set this level to {@link LogLevel#DEBUG} and then dynamically enabled disable this log level
     * whenever required. <br/>
     *
     * @param wireLogginLevel Log level at which the wire level logs will be logged.
     *
     * @return This builder.
     *
     * @see LoggingHandler
     */
    public B enableWireLogging(LogLevel wireLogginLevel) {
        this.wireLogginLevel = wireLogginLevel;
        return returnBuilder();
    }

    public B withName(String name) {
        this.name = name;
        return returnBuilder();
    }

    public B withMetricEventsListenerFactory(MetricEventsListenerFactory eventListenersFactory) {
        this.eventListenersFactory = eventListenersFactory;
        return returnBuilder();
    }

    /**
     * Overrides all the connection pool settings done previous to this call and disables connection pooling for this
     * client, unless enabled again after this call returns.
     *
     * @return This builder.
     */
    public B withNoConnectionPooling() {
        poolBuilder = null;
        return returnBuilder();
    }

    public B withSslEngineFactory(SSLEngineFactory sslEngineFactory) {
        this.sslEngineFactory = sslEngineFactory;
        return returnBuilder();
    }

    public Bootstrap getBootstrap() {
        return bootstrap;
    }

    public RxClientImpl.ServerInfo getServerInfo() {
        return serverInfo;
    }

    public MetricEventsSubject<ClientMetricsEvent<?>> getEventsSubject() {
        return eventsSubject;
    }

    public C build() {
        if (null == socketChannel) {
            socketChannel = defaultSocketChannelClass();
            if (null == eventLoopGroup) {
                eventLoopGroup = defaultEventloop(socketChannel);
            }
        }

        if (null == eventLoopGroup) {
            if (defaultSocketChannelClass() == socketChannel) {
                eventLoopGroup = defaultEventloop(socketChannel);
            } else {
                // Fail fast for defaults we do not support.
                throw new IllegalStateException("Specified a channel class but not the event loop group.");
            }
        }

        bootstrap.channel(socketChannel).group(eventLoopGroup);

        if (null != wireLogginLevel) {
            pipelineConfigurator = PipelineConfigurators.appendLoggingConfigurator(pipelineConfigurator,
                                                                                   wireLogginLevel);
        }
        if(null != sslEngineFactory) {
            appendPipelineConfigurator(PipelineConfigurators.<O,I>sslConfigurator(sslEngineFactory));
        }
        C client = createClient();
        if (null != eventListenersFactory) {
            MetricEventsListener<? extends ClientMetricsEvent<?>> listener =
                    newMetricsListener(eventListenersFactory, client);
            client.subscribe(listener);
        }
        return client;
    }

    protected EventLoopGroup defaultEventloop(Class<? extends Channel> socketChannel) {
        return RxNetty.getRxEventLoopProvider().globalClientEventLoop();
    }

    protected Class<? extends SocketChannel> defaultSocketChannelClass() {
        return NioSocketChannel.class;
    }

    protected abstract C createClient();

    @SuppressWarnings("unchecked")
    protected B returnBuilder() {
        return (B) this;
    }

    protected ConnectionPoolBuilder<O, I> getPoolBuilder(boolean createNew) {
        if (null == poolBuilder && createNew) {
            /**
             * Here we override the connection factory if provided because at runtime we can not determine whether it
             * is a pooled connection factory or not, which is required by the builder.
             * This works well as someone who wants to override the connection factory should either start with a
             * pool builder or don't choose a pooled connection later.
             */
            poolBuilder = new ConnectionPoolBuilder<O, I>(serverInfo, channelFactory, eventsSubject); // Overrides the connection factory
        }
        return poolBuilder;
    }

    protected String getOrCreateName() {
        if (null != name) {
            return name;
        }

        name = generatedNamePrefix() + "-no-name";
        return name;
    }

    protected String generatedNamePrefix() {
        return "RxClient-";
    }

    protected abstract MetricEventsListener<? extends ClientMetricsEvent<? extends Enum>>
    newMetricsListener(MetricEventsListenerFactory factory, C client);
}
