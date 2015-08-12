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
import io.reactivex.netty.protocol.tcp.server.events.TcpServerEventListener;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.reactivex.netty.spectator.SpectatorUtils.*;

/**
 * TcpServerListener.
 */
public class TcpServerListener extends TcpServerEventListener {

    private final AtomicInteger liveConnections;
    private final AtomicInteger inflightConnections;
    private final Counter failedConnections;
    private final Timer connectionProcessingTimes;
    private final AtomicInteger pendingConnectionClose;
    private final Counter failedConnectionClose;
    private final Timer connectionCloseTimes;

    private final AtomicInteger pendingWrites;
    private final AtomicInteger pendingFlushes;

    private final Counter bytesWritten;
    private final Timer writeTimes;
    private final Counter bytesRead;
    private final Counter failedWrites;
    private final Counter failedFlushes;
    private final Timer flushTimes;

    public TcpServerListener(String monitorId) {
        liveConnections = newGauge("liveConnections", monitorId, new AtomicInteger());
        inflightConnections = newGauge("inflightConnections", monitorId, new AtomicInteger());
        pendingConnectionClose = newGauge("pendingConnectionClose", monitorId, new AtomicInteger());
        failedConnectionClose = newCounter("failedConnectionClose", monitorId);
        failedConnections = newCounter("failedConnections", monitorId);
        connectionProcessingTimes = newTimer("connectionProcessingTimes", monitorId);
        connectionCloseTimes = newTimer("connectionCloseTimes", monitorId);

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
    public void onConnectionHandlingFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        inflightConnections.decrementAndGet();
        failedConnections.increment();
    }

    @Override
    public void onConnectionHandlingSuccess(long duration, TimeUnit timeUnit) {
        inflightConnections.decrementAndGet();
        connectionProcessingTimes.record(duration, timeUnit);
    }

    @Override
    public void onConnectionHandlingStart(long duration, TimeUnit timeUnit) {
        inflightConnections.incrementAndGet();
    }

    @Override
    public void onConnectionCloseStart() {
        pendingConnectionClose.incrementAndGet();
    }

    @Override
    public void onConnectionCloseSuccess(long duration, TimeUnit timeUnit) {
        liveConnections.decrementAndGet();
        pendingConnectionClose.decrementAndGet();
        connectionCloseTimes.record(duration, timeUnit);
    }

    @Override
    public void onConnectionCloseFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        liveConnections.decrementAndGet();
        pendingConnectionClose.decrementAndGet();
        connectionCloseTimes.record(duration, timeUnit);
        failedConnectionClose.increment();
    }

    @Override
    public void onNewClientConnected() {
        liveConnections.incrementAndGet();
    }

    @Override
    public void onByteRead(long bytesRead) {
        this.bytesRead.increment(bytesRead);
    }

    @Override
    public void onFlushFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        pendingFlushes.decrementAndGet();
        failedFlushes.increment();
    }

    @Override
    public void onFlushSuccess(long duration, TimeUnit timeUnit) {
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
    public void onWriteSuccess(long duration, TimeUnit timeUnit, long bytesWritten) {
        pendingWrites.decrementAndGet();
        this.bytesWritten.increment(bytesWritten);
        writeTimes.record(duration, timeUnit);
    }

    @Override
    public void onWriteStart() {
        pendingWrites.incrementAndGet();
    }

    public long getLiveConnections() {
        return liveConnections.get();
    }

    public long getInflightConnections() {
        return inflightConnections.get();
    }

    public long getFailedConnections() {
        return failedConnections.count();
    }

    public Timer getConnectionProcessingTimes() {
        return connectionProcessingTimes;
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
}
