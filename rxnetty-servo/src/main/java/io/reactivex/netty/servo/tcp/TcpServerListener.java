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
import io.reactivex.netty.metrics.ServerMetricEventsListener;
import io.reactivex.netty.server.ServerMetricsEvent;
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
public class TcpServerListener<T extends ServerMetricsEvent<?>> extends ServerMetricEventsListener<T> {

    private final LongGauge liveConnections;
    private final LongGauge inflightConnections;
    private final Counter failedConnections;
    private final Timer connectionProcessingTimes;
    private final LongGauge pendingConnectionClose;
    private final Counter failedConnectionClose;
    private final Timer connectionCloseTimes;

    private final LongGauge pendingWrites;
    private final LongGauge pendingFlushes;

    private final Counter bytesWritten;
    private final Timer writeTimes;
    private final Counter bytesRead;
    private final Counter failedWrites;
    private final Counter failedFlushes;
    private final Timer flushTimes;
    private final RefCountingMonitor refCounter;

    protected TcpServerListener(String monitorId) {
        refCounter = new RefCountingMonitor(monitorId);
        liveConnections = newLongGauge("liveConnections");
        inflightConnections = newLongGauge("inflightConnections");
        pendingConnectionClose = newLongGauge("pendingConnectionClose");
        failedConnectionClose = newCounter("failedConnectionClose");
        failedConnections = newCounter("failedConnections");
        connectionProcessingTimes = newTimer("connectionProcessingTimes");
        connectionCloseTimes = newTimer("connectionCloseTimes");

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
    protected void onConnectionHandlingFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        decrementLongGauge(inflightConnections);
        failedConnections.increment();
    }

    @Override
    protected void onConnectionHandlingSuccess(long duration, TimeUnit timeUnit) {
        decrementLongGauge(inflightConnections);
        connectionProcessingTimes.record(duration, timeUnit);
    }

    @Override
    protected void onConnectionHandlingStart(long duration, TimeUnit timeUnit) {
        incrementLongGauge(inflightConnections);
    }

    @Override
    protected void onConnectionCloseStart() {
        incrementLongGauge(pendingConnectionClose);
    }

    @Override
    protected void onConnectionCloseSuccess(long duration, TimeUnit timeUnit) {
        decrementLongGauge(liveConnections);
        decrementLongGauge(pendingConnectionClose);
        connectionCloseTimes.record(duration, timeUnit);
    }

    @Override
    protected void onConnectionCloseFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        decrementLongGauge(liveConnections);
        decrementLongGauge(pendingConnectionClose);
        connectionCloseTimes.record(duration, timeUnit);
        failedConnectionClose.increment();
    }

    @Override
    protected void onNewClientConnected() {
        incrementLongGauge(liveConnections);
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
    public void onCompleted() {
        refCounter.onCompleted(this);
    }

    @Override
    public void onSubscribe() {
        refCounter.onSubscribe(this);
    }

    public static TcpServerListener<ServerMetricsEvent<ServerMetricsEvent.EventType>> newListener(String monitorId) {
        return new TcpServerListener<ServerMetricsEvent<ServerMetricsEvent.EventType>>(monitorId);
    }

    public long getLiveConnections() {
        return liveConnections.getValue().longValue();
    }

    public long getInflightConnections() {
        return inflightConnections.getValue().longValue();
    }

    public long getFailedConnections() {
        return failedConnections.getValue().longValue();
    }

    public Timer getConnectionProcessingTimes() {
        return connectionProcessingTimes;
    }

    public long getPendingWrites() {
        return pendingWrites.getValue().longValue();
    }

    public long getPendingFlushes() {
        return pendingFlushes.getValue().longValue();
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
}
