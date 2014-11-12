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
import io.reactivex.netty.protocol.http.client.HttpClientMetricsEvent;

import java.util.concurrent.TimeUnit;

/**
 * A convenience implementation for {@link MetricEventsListener} for receiving {@link HttpClientMetricsEvent}. This
 * implementation receives the events and provides convenience methods representing those events with clear arguments
 * that are expected with that event type.
 *
 * @author Nitesh Kant
 */
public abstract class HttpClientMetricEventsListener extends ClientMetricEventsListener<ClientMetricsEvent<?>> {

    @Override
    public void onEvent(ClientMetricsEvent<?> event, long duration, TimeUnit timeUnit, Throwable throwable,
                        Object value) {
        if (event.getType() instanceof ClientMetricsEvent.EventType) {
            super.onEvent(event, duration, timeUnit, throwable, value);
        } else {
            switch ((HttpClientMetricsEvent.EventType) event.getType()) {
                case RequestSubmitted:
                    onRequestSubmitted();
                    break;
                case RequestContentSourceError:
                    onRequestContentSourceError(throwable);
                    break;
                case RequestHeadersWriteStart:
                    onRequestHeadersWriteStart();
                    break;
                case RequestHeadersWriteSuccess:
                    onRequestHeadersWriteSuccess(duration, timeUnit);
                    break;
                case RequestHeadersWriteFailed:
                    onRequestHeadersWriteFailed(duration, timeUnit, throwable);
                    break;
                case RequestContentWriteStart:
                    onRequestContentWriteStart();
                    break;
                case RequestContentWriteSuccess:
                    onRequestContentWriteSuccess(duration, timeUnit);
                    break;
                case RequestContentWriteFailed:
                    onRequestContentWriteFailed(duration, timeUnit, throwable);
                    break;
                case RequestWriteComplete:
                    onRequestWriteComplete(duration, timeUnit);
                    break;
                case RequestWriteFailed:
                    onRequestWriteFailed(duration, timeUnit, throwable);
                    break;
                case ResponseHeadersReceived:
                    onResponseHeadersReceived(duration, timeUnit);
                    break;
                case ResponseContentReceived:
                    onResponseContentReceived(duration, timeUnit);
                    break;
                case ResponseReceiveComplete:
                    onResponseReceiveComplete(duration, timeUnit);
                    break;
                case ResponseFailed:
                    onResponseFailed(duration, timeUnit, throwable);
                    break;
                case RequestProcessingComplete:
                    onRequestProcessingComplete(duration, timeUnit);
                    break;
            }
        }
    }

    @SuppressWarnings("unused") protected void onRequestContentSourceError(Throwable throwable) {}

    @SuppressWarnings("unused") protected void onRequestWriteFailed(long duration, TimeUnit timeUnit, Throwable throwable) {}

    @SuppressWarnings("unused") protected void onResponseFailed(long duration, TimeUnit timeUnit, Throwable throwable) {}

    @SuppressWarnings("unused") protected void onResponseReceiveComplete(long duration, TimeUnit timeUnit) {}

    @SuppressWarnings("unused") protected void onResponseContentReceived(long duration, TimeUnit timeUnit) {}

    @SuppressWarnings("unused") protected void onResponseHeadersReceived(long duration, TimeUnit timeUnit) {}

    @SuppressWarnings("unused") protected void onRequestWriteComplete(long duration, TimeUnit timeUnit) {}

    @SuppressWarnings("unused") protected void onRequestContentWriteFailed(long duration, TimeUnit timeUnit, Throwable throwable) {}

    @SuppressWarnings("unused") protected void onRequestContentWriteSuccess(long duration, TimeUnit timeUnit) {}

    @SuppressWarnings("unused") protected void onRequestContentWriteStart() {}

    @SuppressWarnings("unused") protected void onRequestHeadersWriteFailed(long duration, TimeUnit timeUnit, Throwable throwable) {}

    @SuppressWarnings("unused") protected void onRequestHeadersWriteSuccess(long duration, TimeUnit timeUnit) {}

    @SuppressWarnings("unused") protected void onRequestHeadersWriteStart() {}

    @SuppressWarnings("unused") protected void onRequestSubmitted() {}

    @SuppressWarnings("unused") protected void onRequestProcessingComplete(long duration, TimeUnit timeUnit) {}
}
