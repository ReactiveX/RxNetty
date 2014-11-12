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

import io.reactivex.netty.server.ServerMetricsEvent;

import java.util.concurrent.TimeUnit;

/**
 * A convenience implementation for {@link MetricEventsListener} for receiving {@link ServerMetricsEvent}. This
 * implementation receives the events and provides convenience methods representing those events with clear arguments
 * that are expected with that event type.
 *
 * @author Nitesh Kant
 */
public abstract class ServerMetricEventsListener<T extends ServerMetricsEvent<?>> implements MetricEventsListener<T> {

    @Override
    public void onEvent(T event, long duration, TimeUnit timeUnit, Throwable throwable, Object value) {
        switch ((ServerMetricsEvent.EventType) event.getType()) {
            case NewClientConnected:
                onNewClientConnected();
                break;
            case ConnectionHandlingStart:
                onConnectionHandlingStart(duration, timeUnit);
                break;
            case ConnectionHandlingSuccess:
                onConnectionHandlingSuccess(duration, timeUnit);
                break;
            case ConnectionHandlingFailed:
                onConnectionHandlingFailed(duration, timeUnit, throwable);
                break;
            case ConnectionCloseStart:
                onConnectionCloseStart();
                break;
            case ConnectionCloseSuccess:
                onConnectionCloseSuccess(duration, timeUnit);
                break;
            case ConnectionCloseFailed:
                onConnectionCloseFailed(duration, timeUnit, throwable);
                break;
            case WriteStart:
                onWriteStart();
                break;
            case WriteSuccess:
                onWriteSuccess(duration, timeUnit, ((Number) value).longValue());
                break;
            case WriteFailed:
                onWriteFailed(duration, timeUnit, throwable);
                break;
            case FlushStart:
                onFlushStart();
                break;
            case FlushSuccess:
                onFlushSuccess(duration, timeUnit);
                break;
            case FlushFailed:
                onFlushFailed(duration, timeUnit, throwable);
                break;
            case BytesRead:
                onByteRead(((Number) value).longValue());
                break;
        }
    }

    @SuppressWarnings("unused") protected void onConnectionHandlingFailed(long duration, TimeUnit timeUnit, Throwable throwable) { }

    @SuppressWarnings("unused") protected void onConnectionHandlingSuccess(long duration, TimeUnit timeUnit) {}

    @SuppressWarnings("unused") protected void onConnectionHandlingStart(long duration, TimeUnit timeUnit) { }

    protected void onConnectionCloseStart() { }

    @SuppressWarnings("unused") protected void onConnectionCloseSuccess(long duration, TimeUnit timeUnit) { }

    @SuppressWarnings("unused") protected void onConnectionCloseFailed(long duration, TimeUnit timeUnit, Throwable throwable) { }

    protected void onNewClientConnected() { }

    @SuppressWarnings("unused")protected void onByteRead(long bytesRead) { }

    @SuppressWarnings("unused") protected void onFlushFailed(long duration, TimeUnit timeUnit, Throwable throwable) {}

    @SuppressWarnings("unused") protected void onFlushSuccess(long duration, TimeUnit timeUnit) {}

    protected void onFlushStart() {}

    @SuppressWarnings("unused") protected void onWriteFailed(long duration, TimeUnit timeUnit, Throwable throwable) {}

    @SuppressWarnings("unused") protected void onWriteSuccess(long duration, TimeUnit timeUnit, long bytesWritten) {}

    protected void onWriteStart() {}

    @Override
    public void onCompleted() {
        // No Op
    }

    @Override
    public void onSubscribe() {
        // No Op
    }
}
