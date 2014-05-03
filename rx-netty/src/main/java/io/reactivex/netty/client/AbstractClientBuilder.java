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
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.RxDefaultThreadFactory;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.pipeline.PipelineConfigurators;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author Nitesh Kant
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractClientBuilder<I, O, B extends AbstractClientBuilder, C extends RxClient<I, O>> {

    private static final ScheduledExecutorService SHARED_IDLE_CLEANUP_SCHEDULER =
            Executors.newScheduledThreadPool(1, new RxDefaultThreadFactory("global-client-idle-conn-cleanup-scheduler"));

    protected final RxClientImpl.ServerInfo serverInfo;
    protected final Bootstrap bootstrap;
    protected PipelineConfigurator<O, I> pipelineConfigurator;
    protected Class<? extends Channel> socketChannel;
    protected EventLoopGroup eventLoopGroup;
    protected RxClient.ClientConfig clientConfig;
    protected ConnectionPool<O, I> connectionPool;
    protected PoolLimitDeterminationStrategy limitDeterminationStrategy;
    protected ClientChannelAbstractFactory<O, I> clientChannelFactory;
    protected long idleConnectionsTimeoutMillis = PoolConfig.DEFAULT_CONFIG.getMaxIdleTimeMillis();
    protected ScheduledExecutorService poolIdleCleanupScheduler = SHARED_IDLE_CLEANUP_SCHEDULER;
    protected PoolStatsProvider statsProvider = new PoolStatsImpl();
    protected LogLevel wireLogginLevel;

    protected AbstractClientBuilder(Bootstrap bootstrap, String host, int port,
                                    ClientChannelAbstractFactory<O, I> clientChannelFactory) {
        this.bootstrap = bootstrap;
        this.clientChannelFactory = clientChannelFactory;
        serverInfo = new RxClientImpl.ServerInfo(host, port);
        clientConfig = RxClient.ClientConfig.Builder.newDefaultConfig();
        defaultChannelOptions();
    }

    protected AbstractClientBuilder(String host, int port, ClientChannelAbstractFactory<O, I> clientChannelFactory) {
        this(new Bootstrap(), host, port, clientChannelFactory);
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

    public B connectionPool(ConnectionPool<O, I> pool) {
        connectionPool = pool;
        return returnBuilder();
    }

    public B withMaxConnections(int maxConnections) {
        limitDeterminationStrategy = new MaxConnectionsBasedStrategy(maxConnections);
        return returnBuilder();
    }

    public B withIdleConnectionsTimeoutMillis(long idleConnectionsTimeoutMillis) {
        this.idleConnectionsTimeoutMillis = idleConnectionsTimeoutMillis;
        return returnBuilder();
    }

    public B withConnectionPoolLimitStrategy(PoolLimitDeterminationStrategy limitDeterminationStrategy) {
        this.limitDeterminationStrategy = limitDeterminationStrategy;
        return returnBuilder();
    }

    public B withPoolIdleCleanupScheduler(ScheduledExecutorService poolIdleCleanupScheduler) {
        this.poolIdleCleanupScheduler = poolIdleCleanupScheduler;
        return returnBuilder();
    }

    public B withNoIdleConnectionCleanup() {
        poolIdleCleanupScheduler = null;
        return returnBuilder();
    }

    public B withPoolStatsProvider(PoolStatsProvider statsProvider) {
        this.statsProvider = statsProvider;
        return returnBuilder();
    }

    public B withClientChannelFactory(ClientChannelAbstractFactory<O, I> clientChannelFactory) {
        this.clientChannelFactory = clientChannelFactory;
        return returnBuilder();
    }

    public PipelineConfigurator<O, I> getPipelineConfigurator() {
        return pipelineConfigurator;
    }

    public B appendPipelineConfigurator(PipelineConfigurator<O, I> additionalConfigurator) {
        return pipelineConfigurator(PipelineConfigurators.composeConfigurators(pipelineConfigurator,
                                                                               additionalConfigurator));
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
     * @see {@link LoggingHandler}
     */
    public B enableWireLogging(LogLevel wireLogginLevel) {
        this.wireLogginLevel = wireLogginLevel;
        return returnBuilder();
    }

    public C build() {
        if (null == socketChannel) {
            socketChannel = NioSocketChannel.class;
            if (null == eventLoopGroup) {
                eventLoopGroup = RxNetty.getRxEventLoopProvider().globalClientEventLoop();
            }
        }

        if (null == eventLoopGroup) {
            if (NioSocketChannel.class == socketChannel) {
                eventLoopGroup = RxNetty.getRxEventLoopProvider().globalClientEventLoop();
            } else {
                // Fail fast for defaults we do not support.
                throw new IllegalStateException("Specified a channel class but not the event loop group.");
            }
        }

        bootstrap.channel(socketChannel).group(eventLoopGroup);
        if (shouldCreateConnectionPool()) {
            PoolConfig poolConfig = new PoolConfig(idleConnectionsTimeoutMillis);
            connectionPool = new ConnectionPoolImpl<O, I>(poolConfig, limitDeterminationStrategy,
                                                          poolIdleCleanupScheduler, statsProvider);
        }

        if (null != wireLogginLevel) {
            pipelineConfigurator = PipelineConfigurators.appendLoggingConfigurator(pipelineConfigurator,
                                                                                   wireLogginLevel);
        }
        return createClient();
    }

    protected boolean shouldCreateConnectionPool() {
        return null == connectionPool && null != limitDeterminationStrategy
               || idleConnectionsTimeoutMillis != PoolConfig.DEFAULT_CONFIG.getMaxIdleTimeMillis();
    }

    protected abstract C createClient();

    @SuppressWarnings("unchecked")
    protected B returnBuilder() {
        return (B) this;
    }
}
