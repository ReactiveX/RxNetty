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
package io.reactivex.netty.protocol.http.websocket;

import io.reactivex.netty.server.ServerMetricsEvent;

/**
 * @author Tomasz Bak
 */
public class WebSocketServerMetricsEvent<T extends Enum<T>> extends ServerMetricsEvent<T> {

    public enum EventType implements MetricEventType {

        HandshakeProcessed(false, false, Void.class),
        HandshakeFailure(false, false, Void.class),

        WebSocketFrameWrites(false, false, Void.class),
        WebSocketFrameReads(false, false, Void.class);

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

    public static final WebSocketServerMetricsEvent<EventType> HANDSHAKE_PROCESSED = from(EventType.HandshakeProcessed);

    public static final WebSocketServerMetricsEvent<EventType> HANDSHAKE_FAILURE = from(EventType.HandshakeFailure);

    public static final WebSocketServerMetricsEvent<EventType> WEB_SOCKET_FRAME_WRITES = from(EventType.WebSocketFrameWrites);

    public static final WebSocketServerMetricsEvent<EventType> WEB_SOCKET_FRAME_READS = from(EventType.WebSocketFrameReads);

    /*Always refer to as constants*/
    protected WebSocketServerMetricsEvent(T type, boolean isTimed, boolean isError) {
        super(type, isTimed, isError);
    }

    private static WebSocketServerMetricsEvent<EventType> from(EventType type) {
        return new WebSocketServerMetricsEvent<EventType>(type, type.isTimed(), type.isError());
    }

}