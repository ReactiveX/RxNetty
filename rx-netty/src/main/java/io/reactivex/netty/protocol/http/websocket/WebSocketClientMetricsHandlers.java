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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.metrics.MetricEventsSubject;

/**
 * @author Tomasz Bak
 */
public interface WebSocketClientMetricsHandlers {

    class ClientReadMetricsHandler extends ChannelInboundHandlerAdapter {

        private final MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject;

        public ClientReadMetricsHandler(MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject) {
            this.eventsSubject = eventsSubject;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            eventsSubject.onEvent(WebSocketClientMetricsEvent.WEB_SOCKET_FRAME_READS);
            super.channelRead(ctx, msg);
        }
    }

    class ClientWriteMetricsHandler extends ChannelOutboundHandlerAdapter {

        private final MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject;

        public ClientWriteMetricsHandler(MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject) {
            this.eventsSubject = eventsSubject;
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            eventsSubject.onEvent(WebSocketClientMetricsEvent.WEB_SOCKET_FRAME_WRITES);
            super.write(ctx, msg, promise);
        }
    }
}
