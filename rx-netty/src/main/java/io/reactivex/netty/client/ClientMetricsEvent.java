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

/**
 * @author Nitesh Kant
 */
@SuppressWarnings("rawtypes")
public class ClientMetricsEvent<T extends Enum> extends AbstractMetricsEvent<T> {

    public enum EventType {
        ConnectStart(false, false),
        ConnectSuccess(true, false),
        ConnectFailed(true, true),
        ConnectionCloseStart(false, false),
        ConnectionCloseSuccess(true, false),
        ConnectionCloseFailed(true, true),

        PoolAcquireStart(false, false),
        PoolAcquireSuccess(true, false),
        PooledConnectionReuse(true, false),
        PooledConnectionEviction(false, false),
        PoolAcquireFailed(true, true),
        PoolReleaseStart(false, false),
        PoolReleaseSuccess(true, false),
        PoolReleaseFailed(true, true);

        private final boolean isTimed;
        private final boolean isError;

        EventType(boolean isTimed, boolean isError) {
            this.isTimed = isTimed;
            this.isError = isError;
        }

        public boolean isTimed() {
            return isTimed;
        }

        public boolean isError() {
            return isError;
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


    /*Always refer to as constants*/protected ClientMetricsEvent(T name, boolean isTimed, boolean isError) {
        super(name, isTimed, isError);
    }

    private static ClientMetricsEvent<EventType> from(EventType type) {
        return new ClientMetricsEvent<EventType>(type, type.isTimed(), type.isError());
    }
}
