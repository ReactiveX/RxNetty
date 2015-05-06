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

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketFrameAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketFrameDecoder;
import io.netty.handler.codec.http.websocketx.WebSocketFrameEncoder;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.metrics.Clock;
import io.reactivex.netty.metrics.MetricEventsSubject;
import io.reactivex.netty.protocol.http.websocket.WebSocketClientMetricsHandlers.ClientReadMetricsHandler;
import io.reactivex.netty.protocol.http.websocket.WebSocketClientMetricsHandlers.ClientWriteMetricsHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link WebSocketClientHandler} orchestrates WebSocket handshake process and reconfigures
 * pipeline after the handshake is complete.
 *
 * @author Tomasz Bak
 */
public class WebSocketClientHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketClientHandler.class);

    private final WebSocketClientHandshaker handshaker;
    private final int maxFramePayloadLength;
    private final boolean messageAggregation;
    private final MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject;
    private ChannelPromise handshakeFuture;
    private long handshakeStartTime;

    public WebSocketClientHandler(WebSocketClientHandshaker handshaker,
                                  int maxFramePayloadLength,
                                  boolean messageAggregation,
                                  MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject) {
        this.handshaker = handshaker;
        this.maxFramePayloadLength = maxFramePayloadLength;
        this.messageAggregation = messageAggregation;
        this.eventsSubject = eventsSubject;
    }

    public void addHandshakeFinishedListener(ChannelFutureListener listener) {
        handshakeFuture.addListener(listener);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        handshakeFuture = ctx.newPromise();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        handshakeStartTime = Clock.newStartTimeMillis();
        eventsSubject.onEvent(WebSocketClientMetricsEvent.HANDSHAKE_START);
        handshaker.handshake(ctx.channel());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel ch = ctx.channel();
        if (!handshaker.isHandshakeComplete()) {
            finishHandshake(ctx, (FullHttpResponse) msg, ch);
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    private void finishHandshake(ChannelHandlerContext ctx, FullHttpResponse msg, Channel ch) {
        try {
            handshaker.finishHandshake(ch, msg);
        } catch (WebSocketHandshakeException e) {
            eventsSubject.onEvent(WebSocketClientMetricsEvent.HANDSHAKE_FAILURE, Clock.onEndMillis(handshakeStartTime));
            handshakeFuture.setFailure(e);
            ctx.close();
            return;
        }
        eventsSubject.onEvent(WebSocketClientMetricsEvent.HANDSHAKE_SUCCESS, Clock.onEndMillis(handshakeStartTime));

        ChannelPipeline p = ctx.pipeline();
        ChannelHandlerContext nettyDecoderCtx = p.context(WebSocketFrameDecoder.class);
        p.addAfter(nettyDecoderCtx.name(), "websocket-read-metrics", new ClientReadMetricsHandler(eventsSubject));
        ChannelHandlerContext nettyEncoderCtx = p.context(WebSocketFrameEncoder.class);
        p.addAfter(nettyEncoderCtx.name(), "websocket-write-metrics", new ClientWriteMetricsHandler(eventsSubject));
        if (messageAggregation) {
            p.addAfter("websocket-read-metrics", "websocket-frame-aggregator", new WebSocketFrameAggregator(maxFramePayloadLength));
        }
        HttpObjectAggregator aggregator = p.get(HttpObjectAggregator.class);
        if (aggregator != null) {
            p.remove(aggregator);
        }

        handshakeFuture.setSuccess();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (!handshakeFuture.isDone()) {
            handshakeFuture.setFailure(cause);
        }

        logger.error("Exception caught, closing the channel.", cause);
        ctx.close();
    }
}
