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

import io.netty.util.internal.chmv8.LongAdder;
import io.reactivex.netty.protocol.http.client.CompositeHttpClientBuilder;

import java.util.concurrent.TimeUnit;

/**
 * @author Nitesh Kant
 */
public class PoolStatsImpl implements PoolStats, CompositeHttpClientBuilder.CloneablePoolStatsProvider {

    private final LongAdder idleConnections;
    private final LongAdder inUseConnections;
    private final LongAdder totalConnections;
    private final LongAdder pendingAcquires;
    private final LongAdder pendingReleases;

    public PoolStatsImpl() {
        idleConnections = new LongAdder();
        inUseConnections = new LongAdder();
        totalConnections = new LongAdder();
        pendingAcquires = new LongAdder();
        pendingReleases = new LongAdder();
    }

    @Override
    public long getIdleCount() {
        return idleConnections.longValue();
    }

    @Override
    public long getInUseCount() {
        return inUseConnections.longValue();
    }

    @Override
    public long getTotalConnectionCount() {
        return totalConnections.longValue();
    }

    @Override
    public long getPendingAcquireRequestCount() {
        return pendingAcquires.longValue();
    }

    @Override
    public long getPendingReleaseRequestCount() {
        return pendingReleases.longValue();
    }

    @Override
    public void onEvent(ClientMetricsEvent<ClientMetricsEvent.EventType> event, long duration, TimeUnit timeUnit,
                        Throwable throwable, Object value) {
        switch (event.getType()) {
            case ConnectSuccess:
                onConnectionCreation();
                break;
            case ConnectFailed:
                onConnectFailed();
                break;
            case PooledConnectionReuse:
                onConnectionReuse();
                break;
            case PooledConnectionEviction:
                onConnectionEviction();
                break;
            case PoolAcquireStart:
                onAcquireAttempted();
                break;
            case PoolAcquireSuccess:
                onAcquireSucceeded();
                break;
            case PoolAcquireFailed:
                onAcquireFailed();
                break;
            case PoolReleaseStart:
                onReleaseAttempted();
                break;
            case PoolReleaseSuccess:
                onReleaseSucceeded();
                break;
            case PoolReleaseFailed:
                onReleaseFailed();
                break;
        }
    }

    @Override
    public void onCompleted() {
        // No Op.
    }

    @Override
    public PoolStats getStats() {
        return this;
    }

    private void onConnectionCreation() {
        totalConnections.increment();
    }

    private void onConnectFailed() {
        // No op
    }

    private void onConnectionReuse() {
        idleConnections.decrement();
    }

    private void onConnectionEviction() {
        idleConnections.decrement();
        totalConnections.decrement();
    }

    private void onAcquireAttempted() {
        pendingAcquires.increment();
    }

    private void onAcquireSucceeded() {
        inUseConnections.increment();
        pendingAcquires.decrement();
    }

    private void onAcquireFailed() {
        pendingAcquires.decrement();
    }

    private void onReleaseAttempted() {
        pendingReleases.increment();
    }

    private void onReleaseSucceeded() {
        idleConnections.increment();
        inUseConnections.decrement();
        pendingReleases.decrement();
    }

    private void onReleaseFailed() {
        pendingReleases.decrement();
    }

    @Override
    public CompositeHttpClientBuilder.CloneablePoolStatsProvider copy() {
        return new PoolStatsImpl();
    }
}
