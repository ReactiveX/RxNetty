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

import io.reactivex.netty.protocol.tcp.client.events.TcpClientEventListener;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class TrackableMetricEventsListener extends TcpClientEventListener {

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

    @Override
    public void onConnectSuccess(long duration, TimeUnit timeUnit) {
        creationCount.incrementAndGet();
    }

    @Override
    public void onConnectFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        failedCount.incrementAndGet();
    }

    @Override
    public void onPoolReleaseStart() {
        releaseAttemptedCount.incrementAndGet();
    }

    @Override
    public void onPoolReleaseSuccess(long duration, TimeUnit timeUnit) {
        releaseSucceededCount.incrementAndGet();
    }

    @Override
    public void onPoolReleaseFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        releaseFailedCount.incrementAndGet();
    }

    @Override
    public void onPooledConnectionEviction() {
        evictionCount.incrementAndGet();
    }

    @Override
    public void onPooledConnectionReuse() {
        reuseCount.incrementAndGet();
    }

    @Override
    public void onPoolAcquireStart() {
        acquireAttemptedCount.incrementAndGet();
    }

    @Override
    public void onPoolAcquireSuccess(long duration, TimeUnit timeUnit) {
        acquireSucceededCount.incrementAndGet();
    }

    @Override
    public void onPoolAcquireFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        acquireFailedCount.incrementAndGet();
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
}
