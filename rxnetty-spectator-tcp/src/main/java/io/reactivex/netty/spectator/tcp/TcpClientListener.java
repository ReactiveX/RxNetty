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
import com.netflix.spectator.api.Timer;
import io.reactivex.netty.protocol.tcp.client.events.TcpClientEventListener;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.reactivex.netty.spectator.SpectatorUtils.*;

/**
 * TcpClientListener
 */
public class TcpClientListener extends TcpClientEventListener {

    private final AtomicInteger liveConnections;
    private final Counter connectionCount;
    private final AtomicInteger pendingConnects;
    private final Counter failedConnects;
    private final Timer connectionTimes;

    private final AtomicInteger pendingConnectionClose;
    private final Counter failedConnectionClose;

    private final AtomicInteger pendingPoolAcquires;
    private final Counter failedPoolAcquires;
    private final Timer poolAcquireTimes;

    private final AtomicInteger pendingPoolReleases;
    private final Counter failedPoolReleases;
    private final Timer poolReleaseTimes;

    private final Counter poolAcquires;
    private final Counter poolEvictions;
    private final Counter poolReuse;
    private final Counter poolReleases;

    private final AtomicInteger pendingWrites;
    private final AtomicInteger pendingFlushes;

    private final Counter bytesWritten;
    private final Timer writeTimes;
    private final Counter bytesRead;
    private final Counter failedWrites;
    private final Counter failedFlushes;
    private final Timer flushTimes;

    public TcpClientListener(String monitorId) {
        liveConnections = newGauge("liveConnections", monitorId, new AtomicInteger());
        connectionCount = newCounter("connectionCount", monitorId);
        pendingConnects = newGauge("pendingConnects", monitorId, new AtomicInteger());
        failedConnects = newCounter("failedConnects", monitorId);
        connectionTimes = newTimer("connectionTimes", monitorId);
        pendingConnectionClose = newGauge("pendingConnectionClose", monitorId, new AtomicInteger());
        failedConnectionClose = newCounter("failedConnectionClose", monitorId);
        pendingPoolAcquires = newGauge("pendingPoolAcquires", monitorId, new AtomicInteger());
        poolAcquireTimes = newTimer("poolAcquireTimes", monitorId);
        failedPoolAcquires = newCounter("failedPoolAcquires", monitorId);
        pendingPoolReleases = newGauge("pendingPoolReleases", monitorId, new AtomicInteger());
        poolReleaseTimes = newTimer("poolReleaseTimes", monitorId);
        failedPoolReleases = newCounter("failedPoolReleases", monitorId);
        poolAcquires = newCounter("poolAcquires", monitorId);
        poolEvictions = newCounter("poolEvictions", monitorId);
        poolReuse = newCounter("poolReuse", monitorId);
        poolReleases = newCounter("poolReleases", monitorId);

        pendingWrites = newGauge("pendingWrites", monitorId, new AtomicInteger());
        pendingFlushes = newGauge("pendingFlushes", monitorId, new AtomicInteger());

        bytesWritten = newCounter("bytesWritten", monitorId);
        writeTimes = newTimer("writeTimes", monitorId);
        bytesRead = newCounter("bytesRead", monitorId);
        failedWrites = newCounter("failedWrites", monitorId);
        failedFlushes = newCounter("failedFlushes", monitorId);
        flushTimes = newTimer("flushTimes", monitorId);
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

    public Timer getConnectionTimes() {
        return connectionTimes;
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

    public Timer getPoolAcquireTimes() {
        return poolAcquireTimes;
    }

    public long getPendingPoolReleases() {
        return pendingPoolReleases.get();
    }

    public long getFailedPoolReleases() {
        return failedPoolReleases.count();
    }

    public Timer getPoolReleaseTimes() {
        return poolReleaseTimes;
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

    public Timer getWriteTimes() {
        return writeTimes;
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

    public Timer getFlushTimes() {
        return flushTimes;
    }

    public long getPoolAcquires() {
        return poolAcquires.count();
    }

    public long getPoolReleases() {
        return poolReleases.count();
    }
}
