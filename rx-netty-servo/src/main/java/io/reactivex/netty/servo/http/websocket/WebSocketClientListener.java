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
package io.reactivex.netty.servo.http.websocket;

import com.netflix.servo.monitor.Counter;
import com.netflix.servo.monitor.LongGauge;
import com.netflix.servo.monitor.Timer;
import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.metrics.WebSocketClientMetricEventsListener;
import io.reactivex.netty.protocol.http.websocket.WebSocketClientMetricsEvent;
import io.reactivex.netty.servo.tcp.TcpClientListener;

import java.util.concurrent.TimeUnit;

import static com.netflix.servo.monitor.Monitors.newCounter;
import static com.netflix.servo.monitor.Monitors.newTimer;
import static io.reactivex.netty.servo.ServoUtils.decrementLongGauge;
import static io.reactivex.netty.servo.ServoUtils.incrementLongGauge;
import static io.reactivex.netty.servo.ServoUtils.newLongGauge;

/**
 * @author Tomasz Bak
 */
public class WebSocketClientListener extends TcpClientListener<ClientMetricsEvent<?>> {
    private final LongGauge inflightHandshakes;
    private final LongGauge processedHandshakes;
    private final LongGauge failedHandshakes;
    private final Counter webSocketWrites;
    private final Counter getWebSocketReads;
    private final Timer handshakeProcessingTimes;

    private final WebSocketClientMetricEventsListenerImpl delegate = new WebSocketClientMetricEventsListenerImpl();

    protected WebSocketClientListener(String monitorId) {
        super(monitorId);
        inflightHandshakes = newLongGauge("inflightHandshakes");
        processedHandshakes = newLongGauge("processedHandshakes");
        failedHandshakes = newLongGauge("failedHandshakes");
        webSocketWrites = newCounter("webSocketWrites");
        getWebSocketReads = newCounter("getWebSocketReads");
        handshakeProcessingTimes = newTimer("handshakeProcessingTimes");
    }

    public static WebSocketClientListener newWebSocketListener(String monitorId) {
        return new WebSocketClientListener(monitorId);
    }

    @Override
    public void onEvent(ClientMetricsEvent<?> event, long duration, TimeUnit timeUnit, Throwable throwable,
                        Object value) {
        if (event.getType().getClass() == WebSocketClientMetricsEvent.EventType.class) {
            delegate.onEvent(event, duration, timeUnit, throwable, value);
        } else {
            super.onEvent(event, duration, timeUnit, throwable, value);
        }
    }

    public long getInflightHandshakes() {
        return inflightHandshakes.getNumber().get();
    }

    public long getProcessedHandshakes() {
        return processedHandshakes.getNumber().get();
    }

    public long getFailedHandshakes() {
        return failedHandshakes.getValue().longValue();
    }

    public Timer getHandshakeProcessingTimes() {
        return handshakeProcessingTimes;
    }

    public long getWebSocketWrites() {
        return webSocketWrites.getValue().longValue();
    }

    public long getWebSocketReads() {
        return getWebSocketReads.getValue().longValue();
    }

    private class WebSocketClientMetricEventsListenerImpl extends WebSocketClientMetricEventsListener {
        @Override
        protected void onHandshakeStart() {
            incrementLongGauge(inflightHandshakes);
        }

        @Override
        protected void onHandshakeSuccess(long duration, TimeUnit timeUnit) {
            decrementLongGauge(inflightHandshakes);
            incrementLongGauge(processedHandshakes);
            handshakeProcessingTimes.record(duration, timeUnit);
        }

        @Override
        protected void onHandshakeFailure(long duration, TimeUnit timeUnit, Throwable throwable) {
            decrementLongGauge(inflightHandshakes);
            incrementLongGauge(processedHandshakes);
            incrementLongGauge(failedHandshakes);
            handshakeProcessingTimes.record(duration, timeUnit);
        }

        @Override
        protected void onWebSocketWrites() {
            webSocketWrites.increment();
        }

        @Override
        protected void onWebSocketReads() {
            getWebSocketReads.increment();
        }
    }
}
