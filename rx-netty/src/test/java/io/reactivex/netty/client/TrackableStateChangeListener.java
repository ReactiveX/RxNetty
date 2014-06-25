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
import io.reactivex.netty.metrics.MetricEventsListener;

import java.util.concurrent.TimeUnit;

/**
* @author Nitesh Kant
*/
public class TrackableStateChangeListener implements MetricEventsListener<ClientMetricsEvent<?>> {

    private final LongAdder creationCount = new LongAdder();
    private final LongAdder failedCount = new LongAdder();
    private final LongAdder reuseCount = new LongAdder();
    private final LongAdder evictionCount = new LongAdder();
    private final LongAdder acquireAttemptedCount = new LongAdder();
    private final LongAdder acquireSucceededCount = new LongAdder();
    private final LongAdder acquireFailedCount = new LongAdder();
    private final LongAdder releaseAttemptedCount = new LongAdder();
    private final LongAdder releaseSucceededCount = new LongAdder();
    private final LongAdder releaseFailedCount = new LongAdder();

    public void onConnectionCreation() {
        creationCount.increment();
    }

    public void onConnectFailed() {
        failedCount.increment();
    }

    public void onConnectionReuse() {
        reuseCount.increment();
    }

    public void onConnectionEviction() {
        evictionCount.increment();
    }

    public void onAcquireAttempted() {
        acquireAttemptedCount.increment();
    }

    public void onAcquireSucceeded() {
        acquireSucceededCount.increment();
    }

    public void onAcquireFailed() {
        acquireFailedCount.increment();
    }

    public void onReleaseAttempted() {
        releaseAttemptedCount.increment();
    }

    public void onReleaseSucceeded() {
        releaseSucceededCount.increment();
    }

    public void onReleaseFailed() {
        releaseFailedCount.increment();
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
