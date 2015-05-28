/*
 * Copyright 2015 Netflix, Inc.
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
package io.reactivex.netty.protocol.http.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.reactivex.netty.channel.ChannelOperations;
import io.reactivex.netty.events.Clock;
import io.reactivex.netty.protocol.http.internal.AbstractHttpConnectionBridge;
import io.reactivex.netty.protocol.http.internal.HttpContentSubscriberEvent;
import io.reactivex.netty.protocol.http.server.events.HttpServerEventPublisher;
import io.reactivex.netty.protocol.tcp.ConnectionInputSubscriberEvent;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

import java.util.ArrayDeque;
import java.util.Queue;

import static java.util.concurrent.TimeUnit.*;

public class HttpServerToConnectionBridge<C> extends AbstractHttpConnectionBridge<C> {

    private volatile boolean activeContentSubscriberExists;

    private final Object contentSubGuard = new Object();
    private Queue<HttpContentSubscriberEvent<?>> pendingContentSubs; /*Guarded by contentSubGuard*/
    private final HttpServerEventPublisher eventPublisher;
    private int lastSeenResponseCode;

    public HttpServerToConnectionBridge(HttpServerEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    protected void onOutboundHeaderWrite(HttpMessage httpMsg, ChannelPromise promise, final long startTimeMillis) {
        HttpResponse response = (HttpResponse) httpMsg;
        if (eventPublisher.publishingEnabled()) {
            eventPublisher.onResponseWriteStart();
        }
        lastSeenResponseCode = response.status().code();
    }

    @Override
    protected void onOutboundLastContentWrite(LastHttpContent msg, ChannelPromise promise,
                                              final long headerWriteStartTime) {
        final int _responseCode = lastSeenResponseCode;

        if (eventPublisher.publishingEnabled()) {
            promise.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (eventPublisher.publishingEnabled()) {
                        long endMillis = Clock.onEndMillis(headerWriteStartTime);
                        if (future.isSuccess()) {
                            eventPublisher.onResponseWriteSuccess(endMillis, MILLISECONDS, _responseCode);
                        } else {
                            eventPublisher.onResponseWriteFailed(endMillis, MILLISECONDS, future.cause());
                        }
                    }
                }
            });
        }
    }

    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof HttpContentSubscriberEvent) {

            final HttpContentSubscriberEvent<?> subscriberEvent = (HttpContentSubscriberEvent<?>) evt;
            subscriberEvent.getSubscriber().add(Subscriptions.create(new Action0() {
                @Override
                public void call() {
                    HttpContentSubscriberEvent<?> nextSub = null;
                    synchronized (contentSubGuard) {
                        if (null != pendingContentSubs) {
                            nextSub = pendingContentSubs.poll();
                        }
                    }

                    activeContentSubscriberExists = null != nextSub;
                    if (null != nextSub) {
                        ctx.fireUserEventTriggered(nextSub);
                    }
                }
            }));

            if (activeContentSubscriberExists) {
                synchronized (contentSubGuard) {
                    if (null == pendingContentSubs) {
                        pendingContentSubs = new ArrayDeque<>(); /*Guarded by contentSubGuard*/
                    }
                    pendingContentSubs.add(subscriberEvent);
                }
                return;
            }

            activeContentSubscriberExists = true;
        }

        // TODO: Handle trailers
        super.userEventTriggered(ctx, evt);
    }

    @Override
    protected boolean isInboundHeader(Object nextItem) {
        return nextItem instanceof HttpRequest;
    }

    @Override
    protected boolean isOutboundHeader(Object nextItem) {
        return nextItem instanceof HttpResponse;
    }

    @Override
    protected ConnectionInputSubscriber newConnectionInputSubscriber(ConnectionInputSubscriberEvent<?, ?> orig) {
        return new ConnectionInputSubscriber(orig.getSubscriber(), orig.getConnection(), true);
    }

    @Override
    protected Object newHttpObject(Object nextItem, Channel channel) {
        if (eventPublisher.publishingEnabled()) {
            eventPublisher.onRequestHeadersReceived();
        }
        return new HttpServerRequestImpl<>((HttpRequest) nextItem, channel);
    }

    @Override
    protected void onContentReceived() {
        if (eventPublisher.publishingEnabled()) {
            eventPublisher.onRequestContentReceived();
        }
    }

    @Override
    protected void onContentReceiveComplete(long receiveStartTimeMillis) {
        if (eventPublisher.publishingEnabled()) {
            eventPublisher.onRequestReceiveComplete(Clock.onEndMillis(receiveStartTimeMillis), MILLISECONDS);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        super.channelReadComplete(ctx);
        Boolean shouldFlush = ctx.channel().attr(ChannelOperations.FLUSH_ONLY_ON_READ_COMPLETE).get();
        if (null != shouldFlush && shouldFlush) {
            ctx.flush(); /*This is a no-op if there is nothing to flush but supports HttpServerResponse.flushOnlyOnReadComplete()*/
        }
    }
}
