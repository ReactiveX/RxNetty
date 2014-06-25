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

import io.reactivex.netty.client.ClientMetricsEvent;

import java.util.concurrent.TimeUnit;

/**
 * A convenience implementation for {@link MetricEventsListener} for receiving {@link io.reactivex.netty.client.ClientMetricsEvent}. This
 * implementation receives the events and provides convenience methods representing those events with clear arguments
 * that are expected with that event type.
 *
 * @author Nitesh Kant
 */
public abstract class ClientMetricEventsListener<T extends ClientMetricsEvent<?>> implements MetricEventsListener<T> {

    @Override
    public void onEvent(T event, long duration, TimeUnit timeUnit, Throwable throwable, Object value) {
        switch ((ClientMetricsEvent.EventType) event.getType()) {
            case ConnectStart:
                onConnectStart();
                break;
            case ConnectSuccess:
                onConnectSuccess(duration, timeUnit);
                break;
            case ConnectFailed:
                onConnectFailed(duration, timeUnit, throwable);
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
            case PoolAcquireStart:
                onPoolAcquireStart();
                break;
            case PoolAcquireSuccess:
                onPoolAcquireSuccess(duration, timeUnit);
                break;
            case PoolAcquireFailed:
                onPoolAcquireFailed(duration, timeUnit, throwable);
                break;
            case PooledConnectionReuse:
                onPooledConnectionReuse(duration, timeUnit);
                break;
            case PooledConnectionEviction:
                onPooledConnectionEviction();
                break;
            case PoolReleaseStart:
                onPoolReleaseStart();
                break;
            case PoolReleaseSuccess:
                onPoolReleaseSuccess(duration, timeUnit);
                break;
            case PoolReleaseFailed:
                onPoolReleaseFailed(duration, timeUnit, throwable);
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

    @SuppressWarnings("unused")protected void onByteRead(long bytesRead) { }

    @SuppressWarnings("unused") protected void onFlushFailed(long duration, TimeUnit timeUnit, Throwable throwable) {}

    @SuppressWarnings("unused") protected void onFlushSuccess(long duration, TimeUnit timeUnit) {}

    @SuppressWarnings("unused") protected void onFlushStart() {}

    @SuppressWarnings("unused") protected void onWriteFailed(long duration, TimeUnit timeUnit, Throwable throwable) {}

    @SuppressWarnings("unused") protected void onWriteSuccess(long duration, TimeUnit timeUnit, long bytesWritten) {}

    @SuppressWarnings("unused") protected void onWriteStart() {}

    @SuppressWarnings("unused") protected void onPoolReleaseFailed(long duration, TimeUnit timeUnit, Throwable throwable) {}

    @SuppressWarnings("unused") protected void onPoolReleaseSuccess(long duration, TimeUnit timeUnit) {}

    @SuppressWarnings("unused") protected void onPoolReleaseStart() {}

    @SuppressWarnings("unused") protected void onPooledConnectionEviction() {}

    @SuppressWarnings("unused") protected void onPooledConnectionReuse(long duration, TimeUnit timeUnit) {}

    @SuppressWarnings("unused") protected void onPoolAcquireFailed(long duration, TimeUnit timeUnit, Throwable throwable) {}

    @SuppressWarnings("unused") protected void onPoolAcquireSuccess(long duration, TimeUnit timeUnit) {}

    @SuppressWarnings("unused") protected void onPoolAcquireStart() {}

    @SuppressWarnings("unused") protected void onConnectionCloseFailed(long duration, TimeUnit timeUnit, Throwable throwable) {}

    @SuppressWarnings("unused") protected void onConnectionCloseSuccess(long duration, TimeUnit timeUnit) {}

    @SuppressWarnings("unused") protected void onConnectionCloseStart() {}

    @SuppressWarnings("unused") protected void onConnectFailed(long duration, TimeUnit timeUnit, Throwable throwable) {}

    @SuppressWarnings("unused") protected void onConnectSuccess(long duration, TimeUnit timeUnit) {}

    @SuppressWarnings("unused") protected void onConnectStart() {}

    @Override
    public void onCompleted() {
        // No Op
    }

    @Override
    public void onSubscribe() {
        // No Op
    }
}
