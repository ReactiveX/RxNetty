package io.reactivex.netty.protocol.http.websocket;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.reactivex.netty.metrics.MetricEventsSubject;
import io.reactivex.netty.server.ServerMetricsEvent;

/**
 * @author Tomasz Bak
 */
public interface WebSocketServerMetricsHandlers {

    class ServerReadMetricsHandler extends ChannelInboundHandlerAdapter {

        private final MetricEventsSubject<ServerMetricsEvent<?>> eventsSubject;

        public ServerReadMetricsHandler(MetricEventsSubject<ServerMetricsEvent<?>> eventsSubject) {
            this.eventsSubject = eventsSubject;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            eventsSubject.onEvent(WebSocketServerMetricsEvent.WEB_SOCKET_FRAME_READS);
            super.channelRead(ctx, msg);
        }
    }

    class ServerWriteMetricsHandler extends ChannelOutboundHandlerAdapter {

        private final MetricEventsSubject<ServerMetricsEvent<?>> eventsSubject;

        public ServerWriteMetricsHandler(MetricEventsSubject<ServerMetricsEvent<?>> eventsSubject) {
            this.eventsSubject = eventsSubject;
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            eventsSubject.onEvent(WebSocketServerMetricsEvent.WEB_SOCKET_FRAME_WRITES);
            super.write(ctx, msg, promise);
        }
    }
}
