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
import io.reactivex.netty.metrics.WebSocketServerMetricEventsListener;
import io.reactivex.netty.protocol.http.websocket.WebSocketServerMetricsEvent;
import io.reactivex.netty.server.ServerMetricsEvent;
import io.reactivex.netty.servo.tcp.TcpServerListener;

import java.util.concurrent.TimeUnit;

import static com.netflix.servo.monitor.Monitors.newCounter;
import static io.reactivex.netty.servo.ServoUtils.incrementLongGauge;
import static io.reactivex.netty.servo.ServoUtils.newLongGauge;

/**
 * @author Tomasz Bak
 */
public class WebSocketServerListener extends TcpServerListener<ServerMetricsEvent<?>> {

    private final LongGauge processedHandshakes;
    private final LongGauge failedHandshakes;
    private final Counter webSocketWrites;
    private final Counter getWebSocketReads;

    private final WebSocketServerMetricEventsListenerImpl delegate = new WebSocketServerMetricEventsListenerImpl();

    protected WebSocketServerListener(String monitorId) {
        super(monitorId);
        processedHandshakes = newLongGauge("processedHandshakes");
        failedHandshakes = newLongGauge("failedHandshakes");
        webSocketWrites = newCounter("webSocketWrites");
        getWebSocketReads = newCounter("getWebSocketReads");
    }

    @Override
    public void onEvent(ServerMetricsEvent<?> event, long duration, TimeUnit timeUnit, Throwable throwable,
                        Object value) {
        if (event.getType().getClass() == WebSocketServerMetricsEvent.EventType.class) {
            delegate.onEvent(event, duration, timeUnit, throwable, value);
        } else {
            super.onEvent(event, duration, timeUnit, throwable, value);
        }
    }

    public long getProcessedHandshakes() {
        return processedHandshakes.getNumber().get();
    }

    public long getFailedHandshakes() {
        return failedHandshakes.getValue().longValue();
    }

    public long getWebSocketWrites() {
        return webSocketWrites.getValue().longValue();
    }

    public long getWebSocketReads() {
        return getWebSocketReads.getValue().longValue();
    }

    public static WebSocketServerListener newWebSocketListener(String monitorId) {
        return new WebSocketServerListener(monitorId);
    }

    private class WebSocketServerMetricEventsListenerImpl extends WebSocketServerMetricEventsListener {

        @Override
        protected void onHandshakeProcessed() {
            incrementLongGauge(processedHandshakes);
        }

        @Override
        protected void onHandshakeFailure() {
            incrementLongGauge(processedHandshakes);
            incrementLongGauge(failedHandshakes);
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
