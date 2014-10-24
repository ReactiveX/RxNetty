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
package io.reactivex.netty.server;

import io.reactivex.netty.metrics.AbstractMetricsEvent;

/**
 * @author Nitesh Kant
 */
@SuppressWarnings("rawtypes")
public class ServerMetricsEvent<T extends Enum> extends AbstractMetricsEvent<T> {

    public enum EventType implements MetricEventType {

        NewClientConnected(false, false, Void.class),
        ConnectionHandlingStart(true, false, Void.class),
        ConnectionHandlingSuccess(true, false, Void.class),
        ConnectionHandlingFailed(true, true, Void.class),
        ConnectionCloseStart(false, false, Void.class),
        ConnectionCloseSuccess(true, false, Void.class),
        ConnectionCloseFailed(true, true, Void.class),

        /* Write events on underlying connection, this has no associated protocol, so it is raw bytes written. */
        WriteStart(false, false, Long.class),
        WriteSuccess(true, false, Long.class),
        WriteFailed(true, true, Integer.class),
        FlushStart(false, false, Void.class),
        FlushSuccess(true, false, Void.class),
        FlushFailed(true, true, Void.class),

        /* Read events on the connection, this has no associated protocol, so it depicts raw bytes read. */
        BytesRead(false, false, Integer.class)
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

    public static final ServerMetricsEvent<EventType> NEW_CLIENT_CONNECTED = from(EventType.NewClientConnected);
    public static final ServerMetricsEvent<EventType> CONNECTION_HANDLING_START = from(EventType.ConnectionHandlingStart);
    public static final ServerMetricsEvent<EventType> CONNECTION_HANDLING_SUCCESS = from(EventType.ConnectionHandlingSuccess);
    public static final ServerMetricsEvent<EventType> CONNECTION_HANDLING_FAILED = from(EventType.ConnectionHandlingFailed);

    public static final ServerMetricsEvent<EventType> CONNECTION_CLOSE_START = from(EventType.ConnectionCloseStart);
    public static final ServerMetricsEvent<EventType> CONNECTION_CLOSE_SUCCESS = from(EventType.ConnectionCloseSuccess);
    public static final ServerMetricsEvent<EventType> CONNECTION_CLOSE_FAILED = from(EventType.ConnectionCloseFailed);

    public static final ServerMetricsEvent<EventType> WRITE_START = from(EventType.WriteStart);
    public static final ServerMetricsEvent<EventType> WRITE_SUCCESS = from(EventType.WriteSuccess);
    public static final ServerMetricsEvent<EventType> WRITE_FAILED = from(EventType.WriteFailed);
    public static final ServerMetricsEvent<EventType> FLUSH_START = from(EventType.FlushStart);
    public static final ServerMetricsEvent<EventType> FLUSH_SUCCESS = from(EventType.FlushSuccess);
    public static final ServerMetricsEvent<EventType> FLUSH_FAILED = from(EventType.FlushFailed);

    public static final ServerMetricsEvent<EventType> BYTES_READ = from(EventType.BytesRead);


    /*Always refer to as constants*/protected ServerMetricsEvent(T type, boolean isTimed, boolean isError) {
        super(type, isTimed, isError);
    }

    private static ServerMetricsEvent<EventType> from(EventType type) {
        return new ServerMetricsEvent<EventType>(type, type.isTimed(), type.isError());
    }
}
