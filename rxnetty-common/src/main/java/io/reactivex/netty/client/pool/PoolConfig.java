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
 *
 */
package io.reactivex.netty.client.pool;

import rx.Observable;

import java.util.concurrent.TimeUnit;

/**
 * A configuration for connection pooling for a client.
 *
 * @param <W> Type of object that is written to the client using this pool config.
 * @param <R> Type of object that is read from the the client using this pool config.
 */
public class PoolConfig<W, R> {

    public static final long DEFAULT_MAX_IDLE_TIME_MILLIS = TimeUnit.MILLISECONDS.convert(30, TimeUnit.SECONDS);

    private Observable<Long> idleConnCleanupTicker;
    private PoolLimitDeterminationStrategy limitDeterminationStrategy;
    private IdleConnectionsHolder<W, R> idleConnectionsHolder;
    private long maxIdleTimeMillis;

    public PoolConfig() {
        maxIdleTimeMillis = DEFAULT_MAX_IDLE_TIME_MILLIS;
        idleConnCleanupTicker = Observable.interval(maxIdleTimeMillis, maxIdleTimeMillis, TimeUnit.MILLISECONDS);
        idleConnectionsHolder = new FIFOIdleConnectionsHolder<>();
        limitDeterminationStrategy = UnboundedPoolLimitDeterminationStrategy.INSTANCE;
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
        limitDeterminationStrategy = new MaxConnectionsBasedStrategy(maxConnections);
        return this;
    }

    public PoolConfig<W, R> maxIdleTimeoutMillis(long maxIdleTimeoutMillis) {
        maxIdleTimeMillis = maxIdleTimeoutMillis;
        return this;
    }

    public PoolConfig<W, R> limitDeterminationStrategy(PoolLimitDeterminationStrategy strategy) {
        limitDeterminationStrategy = strategy;
        return this;
    }

    public PoolLimitDeterminationStrategy getLimitDeterminationStrategy() {
        return limitDeterminationStrategy;
    }

    public PoolConfig<W, R> idleConnectionsHolder(IdleConnectionsHolder<W, R> holder) {
        idleConnectionsHolder = holder;
        return this;
    }

    public IdleConnectionsHolder<W, R> getIdleConnectionsHolder() {
        return idleConnectionsHolder;
    }

    public PoolConfig<W, R> idleConnectionsCleanupTimer(Observable<Long> timer) {
        idleConnCleanupTicker = timer;
        return this;
    }

    public Observable<Long> getIdleConnCleanupTicker() {
        return idleConnCleanupTicker;
    }
}
