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
package io.reactivex.netty.protocol.http.server;

import io.reactivex.netty.server.ServerMetricsEvent;

/**
 * @author Nitesh Kant
 */
@SuppressWarnings("rawtypes")
public class HttpServerMetricsEvent<T extends Enum> extends ServerMetricsEvent<T> {

    public enum EventType implements MetricEventType {

        NewRequestReceived(false, false, Void.class),

        RequestHandlingStart(true, false, Void.class),

        RequestHeadersReceived(false, false, Void.class),
        RequestContentReceived(false, false, Void.class),
        RequestReceiveComplete(true, false, Void.class),

        ResponseHeadersWriteStart(false, false, Void.class),
        ResponseHeadersWriteSuccess(true, false, Void.class),
        ResponseHeadersWriteFailed(true, true, Void.class),
        ResponseContentWriteStart(false, false, Void.class),
        ResponseContentWriteSuccess(true, false, Void.class),
        ResponseContentWriteFailed(true, true, Void.class),

        RequestHandlingSuccess(true, false, Void.class),
        RequestHandlingFailed(true, true, Void.class),
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

    public static final HttpServerMetricsEvent<EventType> NEW_REQUEST_RECEIVED = from(EventType.NewRequestReceived);
    public static final HttpServerMetricsEvent<EventType> REQUEST_HANDLING_START = from(EventType.RequestHandlingStart);
    public static final HttpServerMetricsEvent<EventType> REQUEST_HEADERS_RECEIVED = from(EventType.RequestHeadersReceived);
    public static final HttpServerMetricsEvent<EventType> REQUEST_CONTENT_RECEIVED = from(EventType.RequestContentReceived);
    public static final HttpServerMetricsEvent<EventType> REQUEST_RECEIVE_COMPLETE = from(EventType.RequestReceiveComplete);

    public static final HttpServerMetricsEvent<EventType> RESPONSE_HEADERS_WRITE_START = from(EventType.ResponseHeadersWriteStart);
    public static final HttpServerMetricsEvent<EventType> RESPONSE_HEADERS_WRITE_SUCCESS = from(EventType.ResponseHeadersWriteSuccess);
    public static final HttpServerMetricsEvent<EventType> RESPONSE_HEADERS_WRITE_FAILED = from(EventType.ResponseHeadersWriteFailed);
    public static final HttpServerMetricsEvent<EventType> RESPONSE_CONTENT_WRITE_START = from(EventType.ResponseContentWriteStart);
    public static final HttpServerMetricsEvent<EventType> RESPONSE_CONTENT_WRITE_SUCCESS = from(EventType.ResponseContentWriteSuccess);
    public static final HttpServerMetricsEvent<EventType> RESPONSE_CONTENT_WRITE_FAILED = from(EventType.ResponseContentWriteFailed);

    public static final HttpServerMetricsEvent<EventType> REQUEST_HANDLING_SUCCESS = from(EventType.RequestHandlingSuccess);
    public static final HttpServerMetricsEvent<EventType> REQUEST_HANDLING_FAILED = from(EventType.RequestHandlingFailed);


    /*Always refer to as constants*/protected HttpServerMetricsEvent(T type, boolean isTimed, boolean isError) {
        super(type, isTimed, isError);
    }

    private static HttpServerMetricsEvent<EventType> from(EventType type) {
        return new HttpServerMetricsEvent<EventType>(type, type.isTimed(), type.isError());
    }
}
