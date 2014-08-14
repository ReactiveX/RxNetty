package io.reactivex.netty.protocol.http.websocket;

import io.reactivex.netty.client.ClientMetricsEvent;

/**
 * @author Tomasz Bak
 */
public class WebSocketClientMetricsEvent<T extends Enum<T>> extends ClientMetricsEvent<T> {

    public enum EventType implements MetricEventType {

        HandshakeStart(false, false, Void.class),
        HandshakeSuccess(true, false, Void.class),
        HandshakeFailure(true, true, Void.class),

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

    public static final WebSocketClientMetricsEvent<EventType> HANDSHAKE_START = from(EventType.HandshakeStart);
    public static final WebSocketClientMetricsEvent<EventType> HANDSHAKE_SUCCESS = from(EventType.HandshakeSuccess);
    public static final WebSocketClientMetricsEvent<EventType> HANDSHAKE_FAILURE = from(EventType.HandshakeFailure);

    public static final WebSocketClientMetricsEvent<EventType> WEB_SOCKET_FRAME_WRITES = from(EventType.WebSocketFrameWrites);

    public static final WebSocketClientMetricsEvent<EventType> WEB_SOCKET_FRAME_READS = from(EventType.WebSocketFrameReads);

    /*Always refer to as constants*/
    protected WebSocketClientMetricsEvent(T type, boolean isTimed, boolean isError) {
        super(type, isTimed, isError);
    }

    private static WebSocketClientMetricsEvent<EventType> from(EventType type) {
        return new WebSocketClientMetricsEvent<EventType>(type, type.isTimed(), type.isError());
    }

}