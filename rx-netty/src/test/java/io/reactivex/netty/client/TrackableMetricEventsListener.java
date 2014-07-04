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
public class TrackableMetricEventsListener implements MetricEventsListener<ClientMetricsEvent<?>> {

    private final AtomicLong creationCount = new AtomicLong();
    private final AtomicLong failedCount = new AtomicLong();
    private final AtomicLong reuseCount = new AtomicLong();
    private final AtomicLong evictionCount = new AtomicLong();
    private final AtomicLong acquireAttemptedCount = new AtomicLong();
    private final AtomicLong acquireSucceededCount = new AtomicLong();
    private final AtomicLong acquireFailedCount = new AtomicLong();
    private final AtomicLong releaseAttemptedCount = new AtomicLong();
    private final AtomicLong releaseSucceededCount = new AtomicLong();
    private final AtomicLong releaseFailedCount = new AtomicLong();

    public void onConnectionCreation() {
        creationCount.incrementAndGet();
    }

    public void onConnectFailed() {
        failedCount.incrementAndGet();
    }

    public void onConnectionReuse() {
        reuseCount.incrementAndGet();
    }

    public void onConnectionEviction() {
        evictionCount.incrementAndGet();
    }

    public void onAcquireAttempted() {
        acquireAttemptedCount.incrementAndGet();
    }

    public void onAcquireSucceeded() {
        acquireSucceededCount.incrementAndGet();
    }

    public void onAcquireFailed() {
        acquireFailedCount.incrementAndGet();
    }

    public void onReleaseAttempted() {
        releaseAttemptedCount.incrementAndGet();
    }

    public void onReleaseSucceeded() {
        releaseSucceededCount.incrementAndGet();
    }

    public void onReleaseFailed() {
        releaseFailedCount.incrementAndGet();
    }

    public long getAcquireAttemptedCount() {
        return acquireAttemptedCount.longValue();
    }

    public long getAcquireFailedCount() {
        return acquireFailedCount.longValue();
    }

    public long getAcquireSucceededCount() {
        return acquireSucceededCount.longValue();
    }

    public long getCreationCount() {
        return creationCount.longValue();
    }

    public long getEvictionCount() {
        return evictionCount.longValue();
    }

    public long getFailedCount() {
        return failedCount.longValue();
    }

    public long getReleaseAttemptedCount() {
        return releaseAttemptedCount.longValue();
    }

    public long getReleaseFailedCount() {
        return releaseFailedCount.longValue();
    }

    public long getReleaseSucceededCount() {
        return releaseSucceededCount.longValue();
    }

    public long getReuseCount() {
        return reuseCount.longValue();
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
        // No op
    }

    @Override
    public void onSubscribe() {
        // No op.
    }
}
