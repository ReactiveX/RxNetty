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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.WebSocketFrameAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketFrameDecoder;
import io.netty.handler.codec.http.websocketx.WebSocketFrameEncoder;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.CharsetUtil;
import io.reactivex.netty.metrics.MetricEventsSubject;
import io.reactivex.netty.protocol.http.websocket.WebSocketServerMetricsHandlers.ServerReadMetricsHandler;
import io.reactivex.netty.protocol.http.websocket.WebSocketServerMetricsHandlers.ServerWriteMetricsHandler;
import io.reactivex.netty.server.ServerMetricsEvent;

import static io.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static io.netty.handler.codec.http.HttpHeaders.setContentLength;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * {@link WebSocketServerHandler} orchestrates WebSocket handshake process and reconfigures
 * pipeline after the handshake is complete.
 *
 * @author Tomasz Bak
 */
public class WebSocketServerHandler extends SimpleChannelInboundHandler<Object> {

    private final WebSocketServerHandshakerFactory handshakeHandlerFactory;
    private ChannelPromise handshakeFuture;
    private final int maxFramePayloadLength;
    private final boolean messageAggregator;
    private final MetricEventsSubject<ServerMetricsEvent<?>> eventsSubject;

    public WebSocketServerHandler(WebSocketServerHandshakerFactory handshakeHandlerFactory,
                                  int maxFramePayloadLength,
                                  boolean messageAggregator,
                                  MetricEventsSubject<ServerMetricsEvent<?>> eventsSubject) {
        this.handshakeHandlerFactory = handshakeHandlerFactory;
        this.maxFramePayloadLength = maxFramePayloadLength;
        this.messageAggregator = messageAggregator;
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
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            ChannelFuture upgradeFuture = handleHttpRequest(ctx, (FullHttpRequest) msg);
            if (upgradeFuture != null) {
                updatePipeline(ctx);
                upgradeFuture.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (future.isSuccess()) {
                            handshakeFuture.setSuccess();
                            eventsSubject.onEvent(WebSocketServerMetricsEvent.HANDSHAKE_PROCESSED);
                        } else {
                            handshakeFuture.setFailure(future.cause());
                            eventsSubject.onEvent(WebSocketServerMetricsEvent.HANDSHAKE_FAILURE);
                        }
                    }
                });
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    private void updatePipeline(ChannelHandlerContext ctx) {
        ChannelPipeline p = ctx.pipeline();
        ChannelHandlerContext nettyEncoderCtx = p.context(WebSocketFrameEncoder.class);
        p.addAfter(nettyEncoderCtx.name(), "websocket-write-metrics", new ServerWriteMetricsHandler(eventsSubject));
        ChannelHandlerContext nettyDecoderCtx = p.context(WebSocketFrameDecoder.class);
        p.addAfter(nettyDecoderCtx.name(), "websocket-read-metrics", new ServerReadMetricsHandler(eventsSubject));
        if (messageAggregator) {
            p.addAfter("websocket-read-metrics", "websocket-frame-aggregator", new WebSocketFrameAggregator(maxFramePayloadLength));
        }
        p.remove(this);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    private ChannelFuture handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) {
        // Handle a bad request.
        if (!req.getDecoderResult().isSuccess()) {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST));
            eventsSubject.onEvent(WebSocketServerMetricsEvent.HANDSHAKE_FAILURE);
            return null;
        }

        // Allow only GET methods.
        if (req.getMethod() != GET) {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, FORBIDDEN));
            eventsSubject.onEvent(WebSocketServerMetricsEvent.HANDSHAKE_FAILURE);
            return null;
        }

        // Handshake
        WebSocketServerHandshaker handshaker = handshakeHandlerFactory.newHandshaker(req);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
            return null;
        }
        return handshaker.handshake(ctx.channel(), req);
    }

    private static void sendHttpResponse(
            ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res) {
        // Generate an error page if response getStatus code is not OK (200).
        if (res.getStatus().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(res.getStatus().toString(), CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
            setContentLength(res, res.content().readableBytes());
        }

        // Send the response and close the connection if necessary.
        ChannelFuture f = ctx.channel().writeAndFlush(res);
        if (!isKeepAlive(req) || res.getStatus().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }
}
