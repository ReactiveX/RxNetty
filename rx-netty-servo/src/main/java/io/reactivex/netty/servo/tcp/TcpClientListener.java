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

import java.util.concurrent.TimeUnit;

import static com.netflix.servo.monitor.Monitors.newCounter;
import static com.netflix.servo.monitor.Monitors.newTimer;
import static io.reactivex.netty.servo.ServoUtils.decrementLongGauge;
import static io.reactivex.netty.servo.ServoUtils.incrementLongGauge;
import static io.reactivex.netty.servo.ServoUtils.newLongGauge;

/**
 * @author Nitesh Kant
 */
public class TcpClientListener<T extends ClientMetricsEvent<?>> extends AbstractShareableListener<T> {

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

    private final Counter poolEvictions;
    private final Counter poolReuse;

    private final LongGauge pendingWrites;
    private final LongGauge pendingFlushes;

    private final Counter bytesWritten;
    private final Timer writeTimes;
    private final Counter bytesRead;
    private final Counter failedWrites;
    private final Counter failedFlushes;
    private final Timer flushTimes;

    protected TcpClientListener(String monitorId) {
        super(monitorId);
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
        poolEvictions = newCounter("poolEvictions");
        poolReuse = newCounter("poolReuse");

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
    public void onEvent(T event, long duration, TimeUnit timeUnit, Throwable throwable, Object value) {
        switch ((ClientMetricsEvent.EventType) event.getType()) {
            case ConnectStart:
                incrementLongGauge(pendingConnects);
                break;
            case ConnectSuccess:
                decrementLongGauge(pendingConnects);
                incrementLongGauge(liveConnections);
                connectionCount.increment();
                connectionTimes.record(duration, timeUnit);
                break;
            case ConnectFailed:
                decrementLongGauge(pendingConnects);
                failedConnects.increment();
                break;
            case ConnectionCloseStart:
                incrementLongGauge(pendingConnectionClose);
                break;
            case ConnectionCloseSuccess:
                decrementLongGauge(liveConnections);
                decrementLongGauge(pendingConnectionClose);
                break;
            case ConnectionCloseFailed:
                decrementLongGauge(pendingConnectionClose);
                failedConnectionClose.increment();
                break;
            case PoolAcquireStart:
                incrementLongGauge(pendingPoolAcquires);
                break;
            case PoolAcquireSuccess:
                decrementLongGauge(pendingPoolAcquires);
                poolAcquireTimes.record(duration, timeUnit);
                break;
            case PoolAcquireFailed:
                decrementLongGauge(pendingPoolAcquires);
                failedPoolAcquires.increment();
                break;
            case PooledConnectionReuse:
                poolReuse.increment();
                break;
            case PooledConnectionEviction:
                poolEvictions.increment();
                break;
            case PoolReleaseStart:
                incrementLongGauge(pendingPoolReleases);
                break;
            case PoolReleaseSuccess:
                decrementLongGauge(pendingPoolReleases);
                poolReleaseTimes.record(duration, timeUnit);
                break;
            case PoolReleaseFailed:
                decrementLongGauge(pendingPoolReleases);
                failedPoolReleases.increment();
                break;
            case WriteStart:
                incrementLongGauge(pendingWrites);
                break;
            case WriteSuccess:
                decrementLongGauge(pendingWrites);
                bytesWritten.increment(((Number)value).longValue());
                writeTimes.record(duration, timeUnit);
                break;
            case WriteFailed:
                decrementLongGauge(pendingWrites);
                failedWrites.increment();
                break;
            case FlushStart:
                incrementLongGauge(pendingFlushes);
                break;
            case FlushSuccess:
                decrementLongGauge(pendingFlushes);
                flushTimes.record(duration, timeUnit);
                break;
            case FlushFailed:
                decrementLongGauge(pendingFlushes);
                failedFlushes.increment();
                break;
            case BytesRead:
                bytesRead.increment(((Number)value).longValue());
                break;
        }
    }

    public static TcpClientListener<ClientMetricsEvent<ClientMetricsEvent.EventType>> newListener(String monitorId) {
        return new TcpClientListener<ClientMetricsEvent<ClientMetricsEvent.EventType>>(monitorId);
    }
}
