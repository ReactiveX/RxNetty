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

package io.reactivex.netty.spectator.tcp;

import com.netflix.spectator.api.Counter;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.Spectator;
import io.reactivex.netty.protocol.tcp.client.events.TcpClientEventListener;
import io.reactivex.netty.spectator.internal.LatencyMetrics;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.reactivex.netty.spectator.internal.SpectatorUtils.*;

/**
 * TcpClientListener
 */
public class TcpClientListener extends TcpClientEventListener {

    private final AtomicInteger liveConnections;
    private final Counter connectionCount;
    private final AtomicInteger pendingConnects;
    private final Counter failedConnects;
    private final LatencyMetrics connectionTimes;

    private final AtomicInteger pendingConnectionClose;
    private final Counter failedConnectionClose;

    private final AtomicInteger pendingPoolAcquires;
    private final Counter failedPoolAcquires;
    private final LatencyMetrics poolAcquireTimes;

    private final AtomicInteger pendingPoolReleases;
    private final Counter failedPoolReleases;
    private final LatencyMetrics poolReleaseTimes;

    private final Counter poolAcquires;
    private final Counter poolEvictions;
    private final Counter poolReuse;
    private final Counter poolReleases;

    private final AtomicInteger pendingWrites;
    private final AtomicInteger pendingFlushes;

    private final Counter bytesWritten;
    private final LatencyMetrics writeTimes;
    private final Counter bytesRead;
    private final Counter failedWrites;
    private final Counter failedFlushes;
    private final LatencyMetrics flushTimes;

    public TcpClientListener(Registry registry, String monitorId) {
        liveConnections = newGauge(registry, "liveConnections", monitorId, new AtomicInteger());
        connectionCount = newCounter(registry, "connectionCount", monitorId);
        pendingConnects = newGauge(registry, "pendingConnects", monitorId, new AtomicInteger());
        failedConnects = newCounter(registry, "failedConnects", monitorId);
        connectionTimes = new LatencyMetrics("connectionTimes", monitorId, registry);
        pendingConnectionClose = newGauge(registry, "pendingConnectionClose", monitorId, new AtomicInteger());
        failedConnectionClose = newCounter(registry, "failedConnectionClose", monitorId);
        pendingPoolAcquires = newGauge(registry, "pendingPoolAcquires", monitorId, new AtomicInteger());
        poolAcquireTimes = new LatencyMetrics("poolAcquireTimes", monitorId, registry);
        failedPoolAcquires = newCounter(registry, "failedPoolAcquires", monitorId);
        pendingPoolReleases = newGauge(registry, "pendingPoolReleases", monitorId, new AtomicInteger());
        poolReleaseTimes = new LatencyMetrics("poolReleaseTimes", monitorId, registry);
        failedPoolReleases = newCounter(registry, "failedPoolReleases", monitorId);
        poolAcquires = newCounter(registry, "poolAcquires", monitorId);
        poolEvictions = newCounter(registry, "poolEvictions", monitorId);
        poolReuse = newCounter(registry, "poolReuse", monitorId);
        poolReleases = newCounter(registry, "poolReleases", monitorId);

        pendingWrites = newGauge(registry, "pendingWrites", monitorId, new AtomicInteger());
        pendingFlushes = newGauge(registry, "pendingFlushes", monitorId, new AtomicInteger());

        bytesWritten = newCounter(registry, "bytesWritten", monitorId);
        writeTimes = new LatencyMetrics("writeTimes", monitorId, registry);
        bytesRead = newCounter(registry, "bytesRead", monitorId);
        failedWrites = newCounter(registry, "failedWrites", monitorId);
        failedFlushes = newCounter(registry, "failedFlushes", monitorId);
        flushTimes = new LatencyMetrics("flushTimes", monitorId, registry);
    }

    public TcpClientListener(String monitorId) {
        this(Spectator.globalRegistry(), monitorId);
    }

    @Override
    public void onByteRead(long bytesRead) {
        this.bytesRead.increment(bytesRead);
    }

    @Override
    public void onByteWritten(long bytesWritten) {
        this.bytesWritten.increment(bytesWritten);
    }

    @Override
    public void onFlushComplete(long duration, TimeUnit timeUnit) {
        pendingFlushes.decrementAndGet();
        flushTimes.record(duration, timeUnit);
    }

    @Override
    public void onFlushStart() {
        pendingFlushes.incrementAndGet();
    }

    @Override
    public void onWriteFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        pendingWrites.decrementAndGet();
        failedWrites.increment();
    }

    @Override
    public void onWriteSuccess(long duration, TimeUnit timeUnit) {
        pendingWrites.decrementAndGet();
        writeTimes.record(duration, timeUnit);
    }

    @Override
    public void onWriteStart() {
        pendingWrites.incrementAndGet();
    }

    @Override
    public void onPoolReleaseFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        pendingPoolReleases.decrementAndGet();
        poolReleases.increment();
        failedPoolReleases.increment();
    }

    @Override
    public void onPoolReleaseSuccess(long duration, TimeUnit timeUnit) {
        pendingPoolReleases.decrementAndGet();
        poolReleases.increment();
        poolReleaseTimes.record(duration, timeUnit);
    }

    @Override
    public void onPoolReleaseStart() {
        pendingPoolReleases.incrementAndGet();
    }

    @Override
    public void onPooledConnectionEviction() {
        poolEvictions.increment();
    }

    @Override
    public void onPooledConnectionReuse() {
        poolReuse.increment();
    }

    @Override
    public void onPoolAcquireFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        pendingPoolAcquires.decrementAndGet();
        poolAcquires.increment();
        failedPoolAcquires.increment();
    }

    @Override
    public void onPoolAcquireSuccess(long duration, TimeUnit timeUnit) {
        pendingPoolAcquires.decrementAndGet();
        poolAcquires.increment();
        poolAcquireTimes.record(duration, timeUnit);
    }

    @Override
    public void onPoolAcquireStart() {
        pendingPoolAcquires.incrementAndGet();
    }

    @Override
    public void onConnectionCloseFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        liveConnections.decrementAndGet(); // Even though the close failed, the connection isn't live.
        pendingConnectionClose.decrementAndGet();
        failedConnectionClose.increment();
    }

    @Override
    public void onConnectionCloseSuccess(long duration, TimeUnit timeUnit) {
        liveConnections.decrementAndGet();
        pendingConnectionClose.decrementAndGet();
    }

    @Override
    public void onConnectionCloseStart() {
        pendingConnectionClose.incrementAndGet();
    }

    @Override
    public void onConnectFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        pendingConnects.decrementAndGet();
        failedConnects.increment();
    }

    @Override
    public void onConnectSuccess(long duration, TimeUnit timeUnit) {
        pendingConnects.decrementAndGet();
        liveConnections.incrementAndGet();
        connectionCount.increment();
        connectionTimes.record(duration, timeUnit);
    }

    @Override
    public void onConnectStart() {
        pendingConnects.incrementAndGet();
    }

    public long getLiveConnections() {
        return liveConnections.get();
    }

    public long getConnectionCount() {
        return connectionCount.count();
    }

    public long getPendingConnects() {
        return pendingConnects.get();
    }

    public long getFailedConnects() {
        return failedConnects.count();
    }

    public long getPendingConnectionClose() {
        return pendingConnectionClose.get();
    }

    public long getFailedConnectionClose() {
        return failedConnectionClose.count();
    }

    public long getPendingPoolAcquires() {
        return pendingPoolAcquires.get();
    }

    public long getFailedPoolAcquires() {
        return failedPoolAcquires.count();
    }

    public long getPendingPoolReleases() {
        return pendingPoolReleases.get();
    }

    public long getFailedPoolReleases() {
        return failedPoolReleases.count();
    }

    public long getPoolEvictions() {
        return poolEvictions.count();
    }

    public long getPoolReuse() {
        return poolReuse.count();
    }

    public long getPendingWrites() {
        return pendingWrites.get();
    }

    public long getPendingFlushes() {
        return pendingFlushes.get();
    }

    public long getBytesWritten() {
        return bytesWritten.count();
    }

    public long getBytesRead() {
        return bytesRead.count();
    }

    public long getFailedWrites() {
        return failedWrites.count();
    }

    public long getFailedFlushes() {
        return failedFlushes.count();
    }

    public long getPoolAcquires() {
        return poolAcquires.count();
    }

    public long getPoolReleases() {
        return poolReleases.count();
    }
}
