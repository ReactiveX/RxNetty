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

package io.reactivex.netty.protocol.http.client;

import io.reactivex.netty.client.ClientMetricsEvent;

/**
 * @author Nitesh Kant
 */
@SuppressWarnings("rawtypes")
public class HttpClientMetricsEvent<T extends Enum> extends ClientMetricsEvent<T> {

    public enum EventType implements MetricEventType {

        RequestSubmitted(false, false, Void.class),

        RequestContentSourceError(false, true, Void.class),
        RequestHeadersWriteStart(false, false, Void.class),
        RequestHeadersWriteSuccess(true, false, Void.class),
        RequestHeadersWriteFailed(true, true, Void.class),
        RequestContentWriteStart(false, false, Void.class),
        RequestContentWriteSuccess(true, false, Void.class),
        RequestContentWriteFailed(true, true, Void.class),

        RequestWriteComplete(true, false, Void.class),
        RequestWriteFailed(true, true, Void.class),

        ResponseHeadersReceived(false, false, Void.class),
        ResponseContentReceived(false, false, Void.class),
        ResponseReceiveComplete(true, false, Void.class),
        ResponseFailed(true, true, Void.class),
        RequestProcessingComplete(true, false, Void.class),
        ;

        private final boolean isTimed;
        private final boolean isError;
        private final Class<?> optionalDataType;

        EventType(boolean isTimed, boolean isError, Class<?> optionalDataType) {
            this.isTimed = isTimed;
            this.isError = isError;
            this.optionalDataType = optionalDataType;
        }

        @Override
        public boolean isTimed() {
            return isTimed;
        }

        @Override
        public boolean isError() {
            return isError;
        }

        @Override
        public Class<?> getOptionalDataType() {
            return optionalDataType;
        }
    }

    public static final HttpClientMetricsEvent<EventType> REQUEST_SUBMITTED = from(EventType.RequestSubmitted);
    public static final HttpClientMetricsEvent<EventType> REQUEST_CONTENT_SOURCE_ERROR = from(EventType.RequestContentSourceError);
    public static final HttpClientMetricsEvent<EventType> REQUEST_HEADERS_WRITE_START = from(EventType.RequestHeadersWriteStart);
    public static final HttpClientMetricsEvent<EventType> REQUEST_HEADERS_WRITE_SUCCESS = from(EventType.RequestHeadersWriteSuccess);
    public static final HttpClientMetricsEvent<EventType> REQUEST_HEADERS_WRITE_FAILED = from(EventType.RequestHeadersWriteFailed);
    public static final HttpClientMetricsEvent<EventType> REQUEST_CONTENT_WRITE_START = from(EventType.RequestContentWriteStart);
    public static final HttpClientMetricsEvent<EventType> REQUEST_CONTENT_WRITE_SUCCESS = from(EventType.RequestContentWriteSuccess);
    public static final HttpClientMetricsEvent<EventType> REQUEST_CONTENT_WRITE_FAILED = from(EventType.RequestContentWriteFailed);
    public static final HttpClientMetricsEvent<EventType> REQUEST_WRITE_COMPLETE = from(EventType.RequestWriteComplete);
    public static final HttpClientMetricsEvent<EventType> REQUEST_WRITE_FAILED = from(EventType.RequestWriteFailed);

    public static final HttpClientMetricsEvent<EventType> RESPONSE_HEADER_RECEIVED = from(EventType.ResponseHeadersReceived);
    public static final HttpClientMetricsEvent<EventType> RESPONSE_CONTENT_RECEIVED = from(EventType.ResponseContentReceived);
    public static final HttpClientMetricsEvent<EventType> RESPONSE_RECEIVE_COMPLETE = from(EventType.ResponseReceiveComplete);
    public static final HttpClientMetricsEvent<EventType> RESPONSE_FAILED = from(EventType.ResponseFailed);
    public static final HttpClientMetricsEvent<EventType> REQUEST_PROCESSING_COMPLETE = from(EventType.RequestProcessingComplete);

    public enum HttpEventType {

    }

    /*Always refer to as constants*/protected HttpClientMetricsEvent(T type, boolean isTimed, boolean isError) {
        super(type, isTimed, isError);
    }

    private static HttpClientMetricsEvent<EventType> from(EventType type) {
        return new HttpClientMetricsEvent<EventType>(type, type.isTimed(), type.isError());
    }

}
