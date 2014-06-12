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
import io.reactivex.netty.server.ServerMetricsEvent;

import java.util.concurrent.TimeUnit;

import static com.netflix.servo.monitor.Monitors.newCounter;
import static com.netflix.servo.monitor.Monitors.newTimer;
import static io.reactivex.netty.servo.ServoUtils.decrementLongGauge;
import static io.reactivex.netty.servo.ServoUtils.incrementLongGauge;
import static io.reactivex.netty.servo.ServoUtils.newLongGauge;

/**
 * @author Nitesh Kant
 */
public class TcpServerListener extends AbstractShareableListener<ServerMetricsEvent<ServerMetricsEvent.EventType>> {

    private final LongGauge liveConnections;
    private final LongGauge inflightConnections;
    private final Counter failedConnections;
    private final Timer connectionProcessingTimes;

    private final LongGauge pendingWrites;
    private final LongGauge pendingFlushes;

    private final Counter bytesWritten;
    private final Timer writeTimes;
    private final Counter bytesRead;
    private final Counter failedWrites;
    private final Counter failedFlushes;
    private final Timer flushTimes;

    protected TcpServerListener(String monitorId) {
        super(monitorId);
        liveConnections = newLongGauge("liveConnections");
        inflightConnections = newLongGauge("inflightConnections");
        failedConnections = newCounter("failedConnections");
        connectionProcessingTimes = newTimer("connectionProcessingTimes");

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
    public void onEvent(ServerMetricsEvent<ServerMetricsEvent.EventType> event, long duration, TimeUnit timeUnit,
                        Throwable throwable, Object value) {
        switch (event.getType()) {
            case NewClientConnected:
                incrementLongGauge(liveConnections);
                break;
            case ConnectionHandlingStart:
                incrementLongGauge(inflightConnections);
                break;
            case ConnectionHandlingSuccess:
                decrementLongGauge(inflightConnections);
                connectionProcessingTimes.record(duration, timeUnit);
                break;
            case ConnectionHandlingFailed:
                decrementLongGauge(inflightConnections);
                failedConnections.increment();
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

    public static TcpServerListener newListener(String monitorId) {
        return new TcpServerListener(monitorId);
    }
}
