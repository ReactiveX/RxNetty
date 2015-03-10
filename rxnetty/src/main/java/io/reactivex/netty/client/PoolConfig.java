/*
 * Copyright 2015 Netflix, Inc.
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

import io.reactivex.netty.protocol.tcp.client.IdleConnectionsHolder;
import rx.Observable;

import java.util.concurrent.TimeUnit;

/**
 * A configuration for connection pooling for a client.
 *
 * @param <W> Type of object that is written to the client using this pool config.
 * @param <R> Type of object that is read from the the client using this pool config.
 *
 * @author Nitesh Kant
 */
public class PoolConfig<W, R> {

    public static final long DEFAULT_MAX_IDLE_TIME_MILLIS = TimeUnit.MILLISECONDS.convert(30, TimeUnit.SECONDS);

    private final Observable<Long> idleConnCleanupTicker;
    private final PoolLimitDeterminationStrategy limitDeterminationStrategy;
    private final IdleConnectionsHolder<W, R> idleConnectionsHolder;
    private final long maxIdleTimeMillis;

    public PoolConfig(long maxIdleTimeMillis, Observable<Long> idleConnCleanupTimer,
                      PoolLimitDeterminationStrategy limitDeterminationStrategy, IdleConnectionsHolder<W, R> holder) {
        this.maxIdleTimeMillis = maxIdleTimeMillis;
        this.limitDeterminationStrategy = limitDeterminationStrategy;
        idleConnCleanupTicker = idleConnCleanupTimer;
        idleConnectionsHolder = holder;
    }

    public long getMaxIdleTimeMillis() {
        return maxIdleTimeMillis;
    }

    public Observable<Long> getIdleConnectionsCleanupTimer() {
        return idleConnCleanupTicker;
    }

    public PoolLimitDeterminationStrategy getPoolLimitDeterminationStrategy() {
        return limitDeterminationStrategy;
    }

    public PoolConfig<W, R> maxConnections(int maxConnections) {
        return new PoolConfig<W, R>(maxIdleTimeMillis, idleConnCleanupTicker,
                                    new MaxConnectionsBasedStrategy(maxConnections), idleConnectionsHolder);
    }

    public PoolConfig<W, R> maxIdleTimeoutMillis(long maxIdleTimeoutMillis) {
        return new PoolConfig<W, R>(maxIdleTimeoutMillis, idleConnCleanupTicker, limitDeterminationStrategy.copy(),
                                    idleConnectionsHolder);
    }

    public PoolConfig<W, R> limitDeterminationStrategy(PoolLimitDeterminationStrategy strategy) {
        return new PoolConfig<W, R>(maxIdleTimeMillis, idleConnCleanupTicker, strategy, idleConnectionsHolder);
    }

    public PoolConfig<W, R> idleConnectionsHolder(IdleConnectionsHolder<W, R> holder) {
        return new PoolConfig<W, R>(maxIdleTimeMillis, idleConnCleanupTicker, limitDeterminationStrategy.copy(),
                                    holder);
    }

    public PoolConfig<W, R> idleConnectionsCleanupTimer(Observable<Long> timer) {
        return new PoolConfig<W, R>(maxIdleTimeMillis, timer, limitDeterminationStrategy.copy(), idleConnectionsHolder);
    }

    public PoolConfig<W, R> copy() {
        return new PoolConfig<W, R>(maxIdleTimeMillis, idleConnCleanupTicker, limitDeterminationStrategy.copy(),
                                    idleConnectionsHolder);
    }
}
