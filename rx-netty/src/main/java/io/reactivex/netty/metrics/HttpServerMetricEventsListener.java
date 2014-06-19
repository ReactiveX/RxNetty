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

import java.util.concurrent.TimeUnit;

/**
 * A convenience implementation for {@link MetricEventsListener} for receiving {@link HttpServerMetricsEvent}. This
 * implementation receives the events and provides convenience methods representing those events with clear arguments
 * that are expected with that event type.
 *
 * @author Nitesh Kant
 */
public abstract class HttpServerMetricEventsListener extends ServerMetricEventsListener<HttpServerMetricsEvent<?>> {

    @Override
    public void onEvent(HttpServerMetricsEvent<?> event, long duration, TimeUnit timeUnit, Throwable throwable,
                        Object value) {
        if (event.getType() instanceof ServerMetricsEvent.EventType) {
            super.onEvent(event, duration, timeUnit, throwable, value);
        } else {
            switch ((HttpServerMetricsEvent.EventType) event.getType()) {
                case NewRequestReceived:
                    onNewRequestReceived();
                    break;
                case RequestHandlingStart:
                    onRequestHandlingStart(duration, timeUnit);
                    break;
                case RequestHeadersReceived:
                    onRequestHeadersReceived(duration, timeUnit);
                    break;
                case RequestContentReceived:
                    onRequestContentReceived(duration, timeUnit);
                    break;
                case RequestReceiveComplete:
                    onRequestReceiveComplete(duration, timeUnit);
                    break;
                case ResponseHeadersWriteStart:
                    onResponseHeadersWriteStart(duration, timeUnit);
                    break;
                case ResponseHeadersWriteSuccess:
                    onResponseHeadersWriteSuccess(duration, timeUnit);
                    break;
                case ResponseHeadersWriteFailed:
                    onResponseHeadersWriteFailed(duration, timeUnit, throwable);
                    break;
                case ResponseContentWriteStart:
                    onResponseContentWriteStart(duration, timeUnit);
                    break;
                case ResponseContentWriteSuccess:
                    onResponseContentWriteSuccess(duration, timeUnit);
                    break;
                case ResponseContentWriteFailed:
                    onResponseContentWriteFailed(duration, timeUnit, throwable);
                    break;
                case ResponseWriteComplete:
                    onResponseWriteComplete(duration, timeUnit);
                    break;
                case RequestHandlingSuccess:
                    onRequestHandlingSuccess(duration, timeUnit);
                    break;
                case RequestHandlingFailed:
                    onRequestHandlingFailed(duration, timeUnit, throwable);
                    break;
            }
        }
    }

    @SuppressWarnings("unused")protected void onRequestHandlingFailed(long duration, TimeUnit timeUnit, Throwable throwable) {}

    @SuppressWarnings("unused")protected void onRequestHandlingSuccess(long duration, TimeUnit timeUnit) {}

    @SuppressWarnings("unused")protected void onResponseWriteComplete(long duration, TimeUnit timeUnit) {}

    @SuppressWarnings("unused")protected void onResponseContentWriteFailed(long duration, TimeUnit timeUnit, Throwable throwable) {}

    @SuppressWarnings("unused")protected void onResponseContentWriteSuccess(long duration, TimeUnit timeUnit) {}

    @SuppressWarnings("unused")protected void onResponseContentWriteStart(long duration, TimeUnit timeUnit) {}

    @SuppressWarnings("unused")protected void onResponseHeadersWriteFailed(long duration, TimeUnit timeUnit, Throwable throwable) {}

    @SuppressWarnings("unused")protected void onResponseHeadersWriteSuccess(long duration, TimeUnit timeUnit) {}

    @SuppressWarnings("unused")protected void onResponseHeadersWriteStart(long duration, TimeUnit timeUnit) {}

    @SuppressWarnings("unused")protected void onRequestReceiveComplete(long duration, TimeUnit timeUnit) {}

    @SuppressWarnings("unused")protected void onRequestContentReceived(long duration, TimeUnit timeUnit) {}

    @SuppressWarnings("unused")protected void onRequestHeadersReceived(long duration, TimeUnit timeUnit) {}

    @SuppressWarnings("unused")protected void onRequestHandlingStart(long duration, TimeUnit timeUnit) { }

    protected void onNewRequestReceived() {}

}
