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

import io.reactivex.netty.protocol.http.server.HttpServerMetricsEvent;
import io.reactivex.netty.server.ServerMetricsEvent;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Nitesh Kant
 */
public class TestableServerMetricsEventListener extends HttpServerMetricEventsListener {

    private final EnumMap<ServerMetricsEvent.EventType, Integer> eventTypeVsInvocations;
    private final EnumMap<ServerMetricsEvent.EventType, List<String>> eventTypeVsInvalidInvocations;

    private final EnumMap<HttpServerMetricsEvent.EventType, Integer> httpEeventTypeVsInvocations;
    private final EnumMap<HttpServerMetricsEvent.EventType, List<String>> httpEventTypeVsInvalidInvocations;

    TestableServerMetricsEventListener() {
        eventTypeVsInvocations = new EnumMap<ServerMetricsEvent.EventType, Integer>(ServerMetricsEvent.EventType.class);
        eventTypeVsInvalidInvocations = new EnumMap<ServerMetricsEvent.EventType, List<String>>(ServerMetricsEvent.EventType.class);
        httpEeventTypeVsInvocations = new EnumMap<HttpServerMetricsEvent.EventType, Integer>(HttpServerMetricsEvent.EventType.class);
        httpEventTypeVsInvalidInvocations = new EnumMap<HttpServerMetricsEvent.EventType, List<String>>(HttpServerMetricsEvent.EventType.class);
    }

    public EnumMap<ServerMetricsEvent.EventType, Integer> getEventTypeVsInvocations() {
        return eventTypeVsInvocations;
    }

    public EnumMap<ServerMetricsEvent.EventType, List<String>> getEventTypeVsInvalidInvocations() {
        return eventTypeVsInvalidInvocations;
    }

    public EnumMap<HttpServerMetricsEvent.EventType, Integer> getHttpEeventTypeVsInvocations() {
        return httpEeventTypeVsInvocations;
    }

    public EnumMap<HttpServerMetricsEvent.EventType, List<String>> getHttpEventTypeVsInvalidInvocations() {
        return httpEventTypeVsInvalidInvocations;
    }

    @Override
    public void onEvent(ServerMetricsEvent<?> event, long duration, TimeUnit timeUnit,
                        Throwable throwable, Object value) {

        boolean isError = false;
        boolean isTimed = false;
        Class<?> optionalDataType = null;

        ServerMetricsEvent.EventType type = null;
        if (ServerMetricsEvent.EventType.class == event.getType().getClass()) {
            type = (ServerMetricsEvent.EventType) event.getType();
            isError = type.isError();
            isTimed = type.isTimed();
            optionalDataType = type.getOptionalDataType();

            Integer existing = eventTypeVsInvocations.get(type);
            if (null == existing) {
                eventTypeVsInvocations.put(type, 1);
            } else {
                eventTypeVsInvocations.put(type, existing + 1);
            }
        }

        HttpServerMetricsEvent.EventType httpEventType = null;
        if (HttpServerMetricsEvent.EventType.class == event.getType().getClass()) {
            httpEventType = (HttpServerMetricsEvent.EventType) event.getType();
            isError = httpEventType.isError();
            isTimed = httpEventType.isTimed();
            optionalDataType = httpEventType.getOptionalDataType();

            Integer existingHttp = httpEeventTypeVsInvocations.get(httpEventType);
            if (null == existingHttp) {
                httpEeventTypeVsInvocations.put(httpEventType, 1);
            } else {
                httpEeventTypeVsInvocations.put(httpEventType, existingHttp + 1);
            }
        }

        List<String> errors = eventTypeVsInvalidInvocations.get(type);
        if (null == errors) {
            errors = new ArrayList<String>();
        }

        if (isError && NO_ERROR == throwable) {
            errors.add("No error passed.");
        } else if (!isError && NO_ERROR != throwable) {
            errors.add("Error passed for non-error event.");
        }

        if (isTimed) {
            if (NO_DURATION == duration) {
                errors.add("No duration provided for timed event.");
            }

            if (NO_TIME_UNIT == timeUnit) {
                errors.add("No time unit provided for timed event.");
            }
        } else {
            if (NO_DURATION != duration) {
                errors.add("Duration provided for non-timed event.");
            }
            if (NO_TIME_UNIT != timeUnit) {
                errors.add("Timeunit provided for non-timed event.");
            }
        }

        if (optionalDataType != null && optionalDataType != Void.class) {
            if (NO_VALUE == value) {
                errors.add("No value provided for the event.");
            } else if (value.getClass() != type.getOptionalDataType()) {
                errors.add("Invalid value provided. Expected: " + type.getOptionalDataType() + ", got: "
                           + value.getClass());
            }
        }

        if (!errors.isEmpty()) {
            if (null != type) {
                eventTypeVsInvalidInvocations.put(type, errors);
            } else if(null != httpEventType) {
                httpEventTypeVsInvalidInvocations.put(httpEventType, errors);
            }
        }
    }
}
