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

import io.reactivex.netty.channel.RxDefaultThreadFactory;
import io.reactivex.netty.metrics.MetricEventsSubject;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * A builder for creating instances of {@link ConnectionPool}
 *
 * @author Nitesh Kant
 */
public class ConnectionPoolBuilder<I, O> {

    private static final ScheduledExecutorService SHARED_IDLE_CLEANUP_SCHEDULER =
            Executors.newScheduledThreadPool(1, new RxDefaultThreadFactory("global-client-idle-conn-cleanup-scheduler"));

    private final RxClient.ServerInfo serverInfo;
    private final MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject;
    private final ClientConnectionFactory<I, O, PooledConnection<I, O>> connectionFactory;
    private final ClientChannelFactory<I, O> channelFactory; // Nullable
    private final PoolLimitDeterminationStrategy limitDeterminationStrategy;
    private final ScheduledExecutorService poolIdleCleanupScheduler;
    private final long idleConnectionsTimeoutMillis;

    public ConnectionPoolBuilder(RxClient.ServerInfo serverInfo,
                                 ClientChannelFactory<I, O> channelFactory,
                                 MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject) {
        this(serverInfo, channelFactory, new PooledConnectionFactory<I, O>(PoolConfig.DEFAULT_CONFIG, eventsSubject),
             eventsSubject);
    }

    public ConnectionPoolBuilder(RxClient.ServerInfo serverInfo,
                                 ClientChannelFactory<I, O> channelFactory,
                                 ClientConnectionFactory<I, O, PooledConnection<I, O>> connectionFactory,
                                 MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject) {
        this(serverInfo, channelFactory, connectionFactory, eventsSubject, new MaxConnectionsBasedStrategy(),
                SHARED_IDLE_CLEANUP_SCHEDULER, PoolConfig.DEFAULT_CONFIG.getMaxIdleTimeMillis());
    }

    protected ConnectionPoolBuilder(RxClient.ServerInfo serverInfo,
                                    ClientChannelFactory<I, O> channelFactory,
                                    ClientConnectionFactory<I, O, PooledConnection<I, O>> connectionFactory,
                                    MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject,
                                    PoolLimitDeterminationStrategy limitDeterminationStrategy,
                                    ScheduledExecutorService poolIdleCleanupScheduler,
                                    long idleConnectionsTimeoutMillis) {
        if (null == serverInfo) {
            throw new NullPointerException("Server info can not be null.");
        }
        if (null == channelFactory) {
            throw new NullPointerException("Channel factory can not be null.");
        }
        if (null == connectionFactory) {
            throw new NullPointerException("Connection factory can not be null.");
        }
        this.serverInfo = serverInfo;
        this.channelFactory = channelFactory;
        this.connectionFactory = connectionFactory;
        this.eventsSubject = eventsSubject;
        this.limitDeterminationStrategy = limitDeterminationStrategy;
        this.poolIdleCleanupScheduler = poolIdleCleanupScheduler;
        this.idleConnectionsTimeoutMillis = idleConnectionsTimeoutMillis;
    }

    public ConnectionPoolBuilder<I, O> withMaxConnections(int maxConnections) {
        return new ConnectionPoolBuilder<I, O>(this.serverInfo, this.channelFactory, this.connectionFactory,
                this.eventsSubject, new MaxConnectionsBasedStrategy(maxConnections), this.poolIdleCleanupScheduler,
                this.idleConnectionsTimeoutMillis);
    }

    public ConnectionPoolBuilder<I, O> withIdleConnectionsTimeoutMillis(long idleConnectionsTimeoutMillis) {
        return new ConnectionPoolBuilder<I, O>(this.serverInfo, this.channelFactory, this.connectionFactory,
                this.eventsSubject, this.limitDeterminationStrategy, this.poolIdleCleanupScheduler,
                idleConnectionsTimeoutMillis);
    }

    public ConnectionPoolBuilder<I, O> withConnectionPoolLimitStrategy(PoolLimitDeterminationStrategy strategy) {
        return new ConnectionPoolBuilder<I, O>(this.serverInfo, this.channelFactory, this.connectionFactory,
                this.eventsSubject, strategy, this.poolIdleCleanupScheduler, this.idleConnectionsTimeoutMillis);
    }

    public ConnectionPoolBuilder<I, O> withPoolIdleCleanupScheduler(ScheduledExecutorService poolIdleCleanupScheduler) {
        return new ConnectionPoolBuilder<I, O>(this.serverInfo, this.channelFactory, this.connectionFactory,
                this.eventsSubject, this.limitDeterminationStrategy, poolIdleCleanupScheduler,
                this.idleConnectionsTimeoutMillis);
    }

    public ConnectionPoolBuilder<I, O> withNoIdleConnectionCleanup() {
        return new ConnectionPoolBuilder<I, O>(this.serverInfo, this.channelFactory, this.connectionFactory,
                this.eventsSubject, this.limitDeterminationStrategy, null, this.idleConnectionsTimeoutMillis);
    }

    public ConnectionPoolBuilder<I, O> withChannelFactory(ClientChannelFactory<I, O> factory) {
        return new ConnectionPoolBuilder<I, O>(this.serverInfo, factory, this.connectionFactory, this.eventsSubject,
                this.limitDeterminationStrategy, this.poolIdleCleanupScheduler, this.idleConnectionsTimeoutMillis);
    }

    public ConnectionPoolBuilder<I, O> withConnectionFactory(ClientConnectionFactory<I, O, PooledConnection<I, O>> factory) {
        return new ConnectionPoolBuilder<I, O>(this.serverInfo, this.channelFactory, factory, this.eventsSubject,
                this.limitDeterminationStrategy, this.poolIdleCleanupScheduler, this.idleConnectionsTimeoutMillis);
    }

    public ClientChannelFactory<I, O> getChannelFactory() {
        return channelFactory;
    }

    public ClientConnectionFactory<I, O, PooledConnection<I, O>> getConnectionFactory() {
        return connectionFactory;
    }

    /**
     * Creates a new instance of the {@link ConnectionPool} if it is configured to do so.
     *
     * @return A new instance of {@link ConnectionPool} if configured, else {@code null}
     */
    public ConnectionPool<I, O> build() {
        PoolConfig poolConfig = new PoolConfig(idleConnectionsTimeoutMillis);

        return new ConnectionPoolImpl<I, O>(serverInfo, poolConfig, limitDeterminationStrategy, poolIdleCleanupScheduler,
                                            connectionFactory, channelFactory, eventsSubject);
    }

    public ConnectionPoolBuilder<I, O> copy(RxClient.ServerInfo serverInfo) {
        return new ConnectionPoolBuilder<I, O>(serverInfo, this.channelFactory, this.connectionFactory,
                this.eventsSubject, this.limitDeterminationStrategy, this.poolIdleCleanupScheduler,
                this.idleConnectionsTimeoutMillis);
    }

    public long getIdleConnectionsTimeoutMillis() {
        return idleConnectionsTimeoutMillis;
    }

    public PoolLimitDeterminationStrategy getLimitDeterminationStrategy() {
        return limitDeterminationStrategy;
    }

    public ScheduledExecutorService getPoolIdleCleanupScheduler() {
        return poolIdleCleanupScheduler;
    }
}
