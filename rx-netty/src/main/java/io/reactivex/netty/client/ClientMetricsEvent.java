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
package io.reactivex.netty.client;

import io.reactivex.netty.metrics.AbstractMetricsEvent;

/**
 * @author Nitesh Kant
 */
@SuppressWarnings("rawtypes")
public class ClientMetricsEvent<T extends Enum> extends AbstractMetricsEvent<T> {

    public enum EventType implements MetricEventType {

        /* Connection specific events. */
        ConnectStart(false, false, Void.class),
        ConnectSuccess(true, false, Void.class),
        ConnectFailed(true, true, Void.class),
        ConnectionCloseStart(false, false, Void.class),
        ConnectionCloseSuccess(true, false, Void.class),
        ConnectionCloseFailed(true, true, Void.class),

        /*Pool specific events, any underlying connection events will not be different for pooled connections*/
        PoolAcquireStart(false, false, Void.class),
        PoolAcquireSuccess(true, false, Void.class),
        PoolAcquireFailed(true, true, Void.class),
        PooledConnectionReuse(true, false, Void.class),
        PooledConnectionEviction(false, false, Void.class),
        PoolReleaseStart(false, false, Void.class),
        PoolReleaseSuccess(true, false, Void.class),
        PoolReleaseFailed(true, true, Void.class),

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

    public static final ClientMetricsEvent<EventType> CONNECT_START = from(EventType.ConnectStart);
    public static final ClientMetricsEvent<EventType> CONNECT_SUCCESS = from(EventType.ConnectSuccess);
    public static final ClientMetricsEvent<EventType> CONNECT_FAILED = from(EventType.ConnectFailed);

    public static final ClientMetricsEvent<EventType> CONNECTION_CLOSE_START = from(EventType.ConnectionCloseStart);
    public static final ClientMetricsEvent<EventType> CONNECTION_CLOSE_SUCCESS = from(EventType.ConnectionCloseSuccess);
    public static final ClientMetricsEvent<EventType> CONNECTION_CLOSE_FAILED = from(EventType.ConnectionCloseFailed);

    public static final ClientMetricsEvent<EventType> POOL_ACQUIRE_START = from(EventType.PoolAcquireStart);
    public static final ClientMetricsEvent<EventType> POOL_ACQUIRE_SUCCESS = from(EventType.PoolAcquireSuccess);
    public static final ClientMetricsEvent<EventType> POOL_ACQUIRE_FAILED = from(EventType.PoolAcquireFailed);
    public static final ClientMetricsEvent<EventType> POOL_RELEASE_START = from(EventType.PoolReleaseStart);
    public static final ClientMetricsEvent<EventType> POOL_RELEASE_SUCCESS = from(EventType.PoolReleaseSuccess);
    public static final ClientMetricsEvent<EventType> POOL_RELEASE_FAILED = from(EventType.PoolReleaseFailed);
    public static final ClientMetricsEvent<EventType> POOLED_CONNECTION_REUSE = from(EventType.PooledConnectionReuse);
    public static final ClientMetricsEvent<EventType> POOLED_CONNECTION_EVICTION = from(EventType.PooledConnectionEviction);

    public static final ClientMetricsEvent<EventType> WRITE_START = from(EventType.WriteStart);
    public static final ClientMetricsEvent<EventType> WRITE_SUCCESS = from(EventType.WriteSuccess);
    public static final ClientMetricsEvent<EventType> WRITE_FAILED = from(EventType.WriteFailed);
    public static final ClientMetricsEvent<EventType> FLUSH_START = from(EventType.FlushStart);
    public static final ClientMetricsEvent<EventType> FLUSH_SUCCESS = from(EventType.FlushSuccess);
    public static final ClientMetricsEvent<EventType> FLUSH_FAILED = from(EventType.FlushFailed);

    public static final ClientMetricsEvent<EventType> BYTES_READ = from(EventType.BytesRead);


    /*Always refer to as constants*/protected ClientMetricsEvent(T name, boolean isTimed, boolean isError) {
        super(name, isTimed, isError);
    }

    private static ClientMetricsEvent<EventType> from(EventType type) {
        return new ClientMetricsEvent<EventType>(type, type.isTimed(), type.isError());
    }
}
