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
package io.reactivex.netty.spectator.http.websocket;

import com.netflix.spectator.api.Counter;
import com.netflix.spectator.api.Timer;
import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.metrics.WebSocketClientMetricEventsListener;
import io.reactivex.netty.protocol.http.websocket.WebSocketClientMetricsEvent;
import io.reactivex.netty.spectator.tcp.TcpClientListener;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.reactivex.netty.spectator.SpectatorUtils.newCounter;
import static io.reactivex.netty.spectator.SpectatorUtils.newGauge;
import static io.reactivex.netty.spectator.SpectatorUtils.newTimer;
/**
 * @author Tomasz Bak
 */
public class WebSocketClientListener extends TcpClientListener<ClientMetricsEvent<?>> {
    private final AtomicInteger inflightHandshakes;
    private final Counter processedHandshakes;
    private final Counter failedHandshakes;
    private final Counter webSocketWrites;
    private final Counter getWebSocketReads;
    private final Timer handshakeProcessingTimes;

    private final WebSocketClientMetricEventsListenerImpl delegate = new WebSocketClientMetricEventsListenerImpl();

    protected WebSocketClientListener(String monitorId) {
        super(monitorId);
        inflightHandshakes = newGauge("inflightHandshakes", monitorId, new AtomicInteger());
        processedHandshakes = newCounter("processedHandshakes", monitorId);
        failedHandshakes = newCounter("failedHandshakes", monitorId);
        webSocketWrites = newCounter("webSocketWrites", monitorId);
        getWebSocketReads = newCounter("getWebSocketReads", monitorId);
        handshakeProcessingTimes = newTimer("handshakeProcessingTimes", monitorId);
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
        return inflightHandshakes.get();
    }

    public long getProcessedHandshakes() {
        return processedHandshakes.count();
    }

    public long getFailedHandshakes() {
        return failedHandshakes.count();
    }

    public Timer getHandshakeProcessingTimes() {
        return handshakeProcessingTimes;
    }

    public long getWebSocketWrites() {
        return webSocketWrites.count();
    }

    public long getWebSocketReads() {
        return getWebSocketReads.count();
    }

    private class WebSocketClientMetricEventsListenerImpl extends WebSocketClientMetricEventsListener {
        @Override
        protected void onHandshakeStart() {
            inflightHandshakes.incrementAndGet();
        }

        @Override
        protected void onHandshakeSuccess(long duration, TimeUnit timeUnit) {
            inflightHandshakes.decrementAndGet();
            processedHandshakes.increment();
            handshakeProcessingTimes.record(duration, timeUnit);
        }

        @Override
        protected void onHandshakeFailure(long duration, TimeUnit timeUnit, Throwable throwable) {
            inflightHandshakes.decrementAndGet();
            processedHandshakes.increment();
            failedHandshakes.increment();
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
