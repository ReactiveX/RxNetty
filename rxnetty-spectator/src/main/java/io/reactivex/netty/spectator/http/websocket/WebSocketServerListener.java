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
import io.reactivex.netty.metrics.WebSocketServerMetricEventsListener;
import io.reactivex.netty.protocol.http.websocket.WebSocketServerMetricsEvent;
import io.reactivex.netty.server.ServerMetricsEvent;
import io.reactivex.netty.spectator.tcp.TcpServerListener;

import java.util.concurrent.TimeUnit;

import static io.reactivex.netty.spectator.SpectatorUtils.newCounter;

/**
 *
 */
public class WebSocketServerListener extends TcpServerListener<ServerMetricsEvent<?>> {

    private final Counter processedHandshakes;
    private final Counter failedHandshakes;
    private final Counter webSocketWrites;
    private final Counter getWebSocketReads;

    private final WebSocketServerMetricEventsListenerImpl delegate = new WebSocketServerMetricEventsListenerImpl();

    protected WebSocketServerListener(String monitorId) {
        super(monitorId);
        processedHandshakes = newCounter("processedHandshakes", monitorId);
        failedHandshakes = newCounter("failedHandshakes", monitorId);
        webSocketWrites = newCounter("webSocketWrites", monitorId);
        getWebSocketReads = newCounter("getWebSocketReads", monitorId);
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
        return processedHandshakes.count();
    }

    public long getFailedHandshakes() {
        return failedHandshakes.count();
    }

    public long getWebSocketWrites() {
        return webSocketWrites.count();
    }

    public long getWebSocketReads() {
        return getWebSocketReads.count();
    }

    public static WebSocketServerListener newWebSocketListener(String monitorId) {
        return new WebSocketServerListener(monitorId);
    }

    private class WebSocketServerMetricEventsListenerImpl extends WebSocketServerMetricEventsListener {

        @Override
        protected void onHandshakeProcessed() {
            processedHandshakes.increment();
        }

        @Override
        protected void onHandshakeFailure() {
            processedHandshakes.increment();
            failedHandshakes.increment();
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
