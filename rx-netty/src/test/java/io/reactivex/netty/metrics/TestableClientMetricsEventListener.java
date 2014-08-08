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

package io.reactivex.netty.metrics;

import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.protocol.http.client.HttpClientMetricsEvent;

/**
 * @author Nitesh Kant
 */
public class TestableClientMetricsEventListener extends HttpClientMetricEventsListener {

    private final EventInvocationsStore<ClientMetricsEvent.EventType> clientEventStore;
    private final EventInvocationsStore<HttpClientMetricsEvent.EventType> httpClientEventStore;

    TestableClientMetricsEventListener() {
        clientEventStore = new EventInvocationsStore<ClientMetricsEvent.EventType>(ClientMetricsEvent.EventType.class);
        httpClientEventStore = new EventInvocationsStore<HttpClientMetricsEvent.EventType>(HttpClientMetricsEvent.EventType.class);
    }

    public EnumMap<ClientMetricsEvent.EventType, Integer> getEventTypeVsInvocations() {
        return clientEventStore.getEventTypeVsInvocations();
    }

    public EnumMap<ClientMetricsEvent.EventType, List<String>> getEventTypeVsInvalidInvocations() {
        return clientEventStore.getEventTypeVsInvalidInvocations();
    }

    public EnumMap<HttpClientMetricsEvent.EventType, Integer> getHttpEeventTypeVsInvocations() {
        return httpClientEventStore.getEventTypeVsInvocations();
    }

    public EnumMap<HttpClientMetricsEvent.EventType, List<String>> getHttpEventTypeVsInvalidInvocations() {
        return httpClientEventStore.getEventTypeVsInvalidInvocations();
    }

    @Override
    public void onEvent(ClientMetricsEvent<?> event, long duration, TimeUnit timeUnit,
                        Throwable throwable, Object value) {
        if (ClientMetricsEvent.EventType.class == event.getType().getClass()) {
            clientEventStore.onEvent((MetricsEvent<ClientMetricsEvent.EventType>) event, duration, timeUnit, throwable, value);
        } else if (HttpClientMetricsEvent.EventType.class == event.getType().getClass()) {
            httpClientEventStore.onEvent((MetricsEvent<HttpClientMetricsEvent.EventType>) event, duration, timeUnit, throwable, value);
        }
    }
}
