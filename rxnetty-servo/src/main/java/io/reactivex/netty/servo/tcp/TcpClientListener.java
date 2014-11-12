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

package io.reactivex.netty.servo.tcp;

import com.netflix.servo.monitor.Counter;
import com.netflix.servo.monitor.LongGauge;
import com.netflix.servo.monitor.Timer;
import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.metrics.ClientMetricEventsListener;
import io.reactivex.netty.servo.RefCountingMonitor;

import java.util.concurrent.TimeUnit;

import static com.netflix.servo.monitor.Monitors.newCounter;
import static com.netflix.servo.monitor.Monitors.newTimer;
import static io.reactivex.netty.servo.ServoUtils.decrementLongGauge;
import static io.reactivex.netty.servo.ServoUtils.incrementLongGauge;
import static io.reactivex.netty.servo.ServoUtils.newLongGauge;

/**
 * @author Nitesh Kant
 */
public class TcpClientListener<T extends ClientMetricsEvent<?>> extends ClientMetricEventsListener<T> {

    private final LongGauge liveConnections;
    private final Counter connectionCount;
    private final LongGauge pendingConnects;
    private final Counter failedConnects;
    private final Timer connectionTimes;

    private final LongGauge pendingConnectionClose;
    private final Counter failedConnectionClose;

    private final LongGauge pendingPoolAcquires;
    private final Counter failedPoolAcquires;
    private final Timer poolAcquireTimes;

    private final LongGauge pendingPoolReleases;
    private final Counter failedPoolReleases;
    private final Timer poolReleaseTimes;

    private final Counter poolAcquires;
    private final Counter poolEvictions;
    private final Counter poolReuse;
    private final Counter poolReleases;

    private final LongGauge pendingWrites;
    private final LongGauge pendingFlushes;

    private final Counter bytesWritten;
    private final Timer writeTimes;
    private final Counter bytesRead;
    private final Counter failedWrites;
    private final Counter failedFlushes;
    private final Timer flushTimes;
    private final RefCountingMonitor refCounter;

    protected TcpClientListener(String monitorId) {
        refCounter = new RefCountingMonitor(monitorId);
        liveConnections = newLongGauge("liveConnections");
        connectionCount = newCounter("connectionCount");
        pendingConnects = newLongGauge("pendingConnects");
        failedConnects = newCounter("failedConnects");
        connectionTimes = newTimer("connectionTimes");
        pendingConnectionClose = newLongGauge("pendingConnectionClose");
        failedConnectionClose = newCounter("failedConnectionClose");
        pendingPoolAcquires = newLongGauge("pendingPoolAcquires");
        poolAcquireTimes = newTimer("poolAcquireTimes");
        failedPoolAcquires = newCounter("failedPoolAcquires");
        pendingPoolReleases = newLongGauge("pendingPoolReleases");
        poolReleaseTimes = newTimer("poolReleaseTimes");
        failedPoolReleases = newCounter("failedPoolReleases");
        poolAcquires = newCounter("poolAcquires");
        poolEvictions = newCounter("poolEvictions");
        poolReuse = newCounter("poolReuse");
        poolReleases = newCounter("poolReleases");

        pendingWrites = newLongGauge("pendingWrites");
        pendingFlushes = newLongGauge("pendingFlushes");

        bytesWritten = newCounter("bytesWritten");
        writeTimes = newTimer("writeTimes");
        bytesRead = newCounter("bytesRead");
        failedWrites = newCounter("failedWrites");
        failedFlushes = newCounter("failedFlushes");
        flushTimes = newTimer("flushTimes");
    }

    @Override
    protected void onByteRead(long bytesRead) {
        this.bytesRead.increment(bytesRead);
    }

    @Override
    protected void onFlushFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        decrementLongGauge(pendingFlushes);
        failedFlushes.increment();
    }

    @Override
    protected void onFlushSuccess(long duration, TimeUnit timeUnit) {
        decrementLongGauge(pendingFlushes);
        flushTimes.record(duration, timeUnit);
    }

    @Override
    protected void onFlushStart() {
        incrementLongGauge(pendingFlushes);
    }

    @Override
    protected void onWriteFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        decrementLongGauge(pendingWrites);
        failedWrites.increment();
    }

    @Override
    protected void onWriteSuccess(long duration, TimeUnit timeUnit, long bytesWritten) {
        decrementLongGauge(pendingWrites);
        this.bytesWritten.increment(bytesWritten);
        writeTimes.record(duration, timeUnit);
    }

    @Override
    protected void onWriteStart() {
        incrementLongGauge(pendingWrites);
    }

    @Override
    protected void onPoolReleaseFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        decrementLongGauge(pendingPoolReleases);
        poolReleases.increment();
        failedPoolReleases.increment();
    }

    @Override
    protected void onPoolReleaseSuccess(long duration, TimeUnit timeUnit) {
        decrementLongGauge(pendingPoolReleases);
        poolReleases.increment();
        poolReleaseTimes.record(duration, timeUnit);
    }

    @Override
    protected void onPoolReleaseStart() {
        incrementLongGauge(pendingPoolReleases);
    }

    @Override
    protected void onPooledConnectionEviction() {
        poolEvictions.increment();
    }

    @Override
    protected void onPooledConnectionReuse(long duration, TimeUnit timeUnit) {
        poolReuse.increment();
    }

    @Override
    protected void onPoolAcquireFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        decrementLongGauge(pendingPoolAcquires);
        poolAcquires.increment();
        failedPoolAcquires.increment();
    }

    @Override
    protected void onPoolAcquireSuccess(long duration, TimeUnit timeUnit) {
        decrementLongGauge(pendingPoolAcquires);
        poolAcquires.increment();
        poolAcquireTimes.record(duration, timeUnit);
    }

    @Override
    protected void onPoolAcquireStart() {
        incrementLongGauge(pendingPoolAcquires);
    }

    @Override
    protected void onConnectionCloseFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        decrementLongGauge(liveConnections); // Even though the close failed, the connection isn't live.
        decrementLongGauge(pendingConnectionClose);
        failedConnectionClose.increment();
    }

    @Override
    protected void onConnectionCloseSuccess(long duration, TimeUnit timeUnit) {
        decrementLongGauge(liveConnections);
        decrementLongGauge(pendingConnectionClose);
    }

    @Override
    protected void onConnectionCloseStart() {
        incrementLongGauge(pendingConnectionClose);
    }

    @Override
    protected void onConnectFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        decrementLongGauge(pendingConnects);
        failedConnects.increment();
    }

    @Override
    protected void onConnectSuccess(long duration, TimeUnit timeUnit) {
        decrementLongGauge(pendingConnects);
        incrementLongGauge(liveConnections);
        connectionCount.increment();
        connectionTimes.record(duration, timeUnit);
    }

    @Override
    protected void onConnectStart() {
        incrementLongGauge(pendingConnects);
    }

    @Override
    public void onCompleted() {
        refCounter.onCompleted(this);
    }

    @Override
    public void onSubscribe() {
        refCounter.onSubscribe(this);
    }

    public long getLiveConnections() {
        return liveConnections.getNumber().get();
    }

    public long getConnectionCount() {
        return connectionCount.getValue().longValue();
    }

    public long getPendingConnects() {
        return pendingConnects.getNumber().get();
    }

    public long getFailedConnects() {
        return failedConnects.getValue().longValue();
    }

    public Timer getConnectionTimes() {
        return connectionTimes;
    }

    public long getPendingConnectionClose() {
        return pendingConnectionClose.getNumber().get();
    }

    public long getFailedConnectionClose() {
        return failedConnectionClose.getValue().longValue();
    }

    public long getPendingPoolAcquires() {
        return pendingPoolAcquires.getNumber().get();
    }

    public long getFailedPoolAcquires() {
        return failedPoolAcquires.getValue().longValue();
    }

    public Timer getPoolAcquireTimes() {
        return poolAcquireTimes;
    }

    public long getPendingPoolReleases() {
        return pendingPoolReleases.getNumber().get();
    }

    public long getFailedPoolReleases() {
        return failedPoolReleases.getValue().longValue();
    }

    public Timer getPoolReleaseTimes() {
        return poolReleaseTimes;
    }

    public long getPoolEvictions() {
        return poolEvictions.getValue().longValue();
    }

    public long getPoolReuse() {
        return poolReuse.getValue().longValue();
    }

    public long getPendingWrites() {
        return pendingWrites.getNumber().get();
    }

    public long getPendingFlushes() {
        return pendingFlushes.getNumber().get();
    }

    public long getBytesWritten() {
        return bytesWritten.getValue().longValue();
    }

    public Timer getWriteTimes() {
        return writeTimes;
    }

    public long getBytesRead() {
        return bytesRead.getValue().longValue();
    }

    public long getFailedWrites() {
        return failedWrites.getValue().longValue();
    }

    public long getFailedFlushes() {
        return failedFlushes.getValue().longValue();
    }

    public Timer getFlushTimes() {
        return flushTimes;
    }

    public long getPoolAcquires() {
        return poolAcquires.getValue().longValue();
    }

    public long getPoolReleases() {
        return poolReleases.getValue().longValue();
    }

    public static TcpClientListener<ClientMetricsEvent<ClientMetricsEvent.EventType>> newListener(String monitorId) {
        return new TcpClientListener<ClientMetricsEvent<ClientMetricsEvent.EventType>>(monitorId);
    }
}
