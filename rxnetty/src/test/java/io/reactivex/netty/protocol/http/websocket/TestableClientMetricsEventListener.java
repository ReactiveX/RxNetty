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
package io.reactivex.netty.protocol.http.websocket;

import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.metrics.EventInvocationsStore;
import io.reactivex.netty.metrics.MetricsEvent;
import io.reactivex.netty.metrics.WebSocketClientMetricEventsListener;

import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Tomasz Bak
 */
public class TestableClientMetricsEventListener extends WebSocketClientMetricEventsListener {

    private final EventInvocationsStore<WebSocketClientMetricsEvent.EventType> webSocketEventStore;

    TestableClientMetricsEventListener() {
        webSocketEventStore = new EventInvocationsStore<WebSocketClientMetricsEvent.EventType>(WebSocketClientMetricsEvent.EventType.class);
    }

    public EnumMap<WebSocketClientMetricsEvent.EventType, Integer> getEventTypeVsInvocations() {
        return webSocketEventStore.getEventTypeVsInvocations();
    }

    public EnumMap<WebSocketClientMetricsEvent.EventType, List<String>> getEventTypeVsInvalidInvocations() {
        return webSocketEventStore.getEventTypeVsInvalidInvocations();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onEvent(ClientMetricsEvent<?> event, long duration, TimeUnit timeUnit,
                        Throwable throwable, Object value) {
        if (WebSocketClientMetricsEvent.EventType.class == event.getType().getClass()) {
            webSocketEventStore.onEvent((MetricsEvent<WebSocketClientMetricsEvent.EventType>) event, duration, timeUnit, throwable, value);
        }
    }
}
