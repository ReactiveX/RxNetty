package io.reactivex.netty.protocol.text.sse;

/**
 * This class has been moved to {@link io.reactivex.netty.protocol.http.sse.ServerSentEvent}. This proxy is for
 * backwards compatibility and will be removed in 0.3.13.
 */
@Deprecated
public class ServerSentEvent extends io.reactivex.netty.protocol.http.sse.ServerSentEvent {

    public ServerSentEvent(String eventId, String eventType, String eventData) {
        this(eventId, eventType, eventData, true);
    }

    public ServerSentEvent(String eventId, String eventType, String eventData, boolean splitMode) {
        super(eventId, eventType, eventData, splitMode);
    }
}
