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

import io.reactivex.netty.protocol.http.server.HttpServerMetricsEvent;
import io.reactivex.netty.server.ServerMetricsEvent;

/**
 * @author Nitesh Kant
 */
public class TestableServerMetricsEventListener extends HttpServerMetricEventsListener {

    private final EventInvocationsStore<ServerMetricsEvent.EventType> serverEventStore;
    private final EventInvocationsStore<HttpServerMetricsEvent.EventType> httpserverEventStore;

    TestableServerMetricsEventListener() {
        serverEventStore = new EventInvocationsStore<ServerMetricsEvent.EventType>(ServerMetricsEvent.EventType.class);
        httpserverEventStore = new EventInvocationsStore<HttpServerMetricsEvent.EventType>(HttpServerMetricsEvent.EventType.class);
    }

    public EnumMap<ServerMetricsEvent.EventType, Integer> getEventTypeVsInvocations() {
        return serverEventStore.getEventTypeVsInvocations();
    }

    public EnumMap<ServerMetricsEvent.EventType, List<String>> getEventTypeVsInvalidInvocations() {
        return serverEventStore.getEventTypeVsInvalidInvocations();
    }

    public EnumMap<HttpServerMetricsEvent.EventType, Integer> getHttpEeventTypeVsInvocations() {
        return httpserverEventStore.getEventTypeVsInvocations();
    }

    public EnumMap<HttpServerMetricsEvent.EventType, List<String>> getHttpEventTypeVsInvalidInvocations() {
        return httpserverEventStore.getEventTypeVsInvalidInvocations();
    }

    @Override
    public void onEvent(ServerMetricsEvent<?> event, long duration, TimeUnit timeUnit,
                        Throwable throwable, Object value) {
        if (ServerMetricsEvent.EventType.class == event.getType().getClass()) {
            serverEventStore.onEvent((MetricsEvent<ServerMetricsEvent.EventType>) event, duration, timeUnit, throwable, value);
        } else if (HttpServerMetricsEvent.EventType.class == event.getType().getClass()) {
            httpserverEventStore.onEvent((MetricsEvent<HttpServerMetricsEvent.EventType>) event, duration, timeUnit, throwable, value);
        }
    }
}
