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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Nitesh Kant
 */
public class PoolStats implements MetricEventsListener<ClientMetricsEvent<?>> {

    private final AtomicLong idleConnections;
    private final AtomicLong inUseConnections;
    private final AtomicLong totalConnections;
    private final AtomicLong pendingAcquires;
    private final AtomicLong pendingReleases;

    public PoolStats() {
        idleConnections = new AtomicLong();
        inUseConnections = new AtomicLong();
        totalConnections = new AtomicLong();
        pendingAcquires = new AtomicLong();
        pendingReleases = new AtomicLong();
    }

    public long getIdleCount() {
        return idleConnections.longValue();
    }

    public long getInUseCount() {
        return inUseConnections.longValue();
    }

    public long getTotalConnectionCount() {
        return totalConnections.longValue();
    }

    public long getPendingAcquireRequestCount() {
        return pendingAcquires.longValue();
    }

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

    }

    @Override
    public void onSubscribe() {
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
}
