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

import io.reactivex.netty.metrics.MetricEventsListener;
import io.reactivex.netty.protocol.http.client.CompositeHttpClientBuilder;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @deprecated Use {@link MetricEventsListener} to get the stats.
 * @author Nitesh Kant
 */
@Deprecated
public class PoolStatsImpl implements PoolStats, CompositeHttpClientBuilder.CloneablePoolStatsProvider {

    private final AtomicLong idleConnections; // LongAdder backport is not distributed with netty anymore. So moving to AtomicLong temporarily before we remove this class.
    private final AtomicLong inUseConnections;
    private final AtomicLong totalConnections;
    private final AtomicLong pendingAcquires;
    private final AtomicLong pendingReleases;

    public PoolStatsImpl() {
        idleConnections = new AtomicLong();
        inUseConnections = new AtomicLong();
        totalConnections = new AtomicLong();
        pendingAcquires = new AtomicLong();
        pendingReleases = new AtomicLong();
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
    public void onEvent(ClientMetricsEvent<?> event, long duration, TimeUnit timeUnit,
                        Throwable throwable, Object value) {
        if (event.getType() instanceof ClientMetricsEvent.EventType) {
            switch ((ClientMetricsEvent.EventType) event.getType()) {
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
    }

    @Override
    public void onCompleted() {
        // No Op.
    }

    @Override
    public void onSubscribe() {
        // No op.

    }

    @Override
    public PoolStats getStats() {
        return this;
    }

    private void onConnectionCreation() {
        totalConnections.incrementAndGet();
    }

    private void onConnectFailed() {
        // No op
    }

    private void onConnectionReuse() {
        idleConnections.decrementAndGet();
    }

    private void onConnectionEviction() {
        idleConnections.decrementAndGet();
        totalConnections.decrementAndGet();
    }

    private void onAcquireAttempted() {
        pendingAcquires.incrementAndGet();
    }

    private void onAcquireSucceeded() {
        inUseConnections.incrementAndGet();
        pendingAcquires.decrementAndGet();
    }

    private void onAcquireFailed() {
        pendingAcquires.decrementAndGet();
    }

    private void onReleaseAttempted() {
        pendingReleases.incrementAndGet();
    }

    private void onReleaseSucceeded() {
        idleConnections.incrementAndGet();
        inUseConnections.decrementAndGet();
        pendingReleases.decrementAndGet();
    }

    private void onReleaseFailed() {
        pendingReleases.decrementAndGet();
    }

    @Override
    public CompositeHttpClientBuilder.CloneablePoolStatsProvider copy() {
        return new PoolStatsImpl();
    }
}
