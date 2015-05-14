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
package io.reactivex.netty.protocol.http.serverNew;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.reactivex.netty.metrics.Clock;
import io.reactivex.netty.metrics.MetricEventsSubject;
import io.reactivex.netty.protocol.http.internal.AbstractHttpConnectionBridge;
import io.reactivex.netty.protocol.http.internal.HttpContentSubscriberEvent;
import io.reactivex.netty.server.ServerMetricsEvent;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

import java.util.ArrayDeque;
import java.util.Queue;

import static io.reactivex.netty.protocol.http.server.HttpServerMetricsEvent.*;

public class HttpServerToConnectionBridge<C> extends AbstractHttpConnectionBridge<C> {

    private final MetricEventsSubject<ServerMetricsEvent<?>> eventsSubject;

    private volatile boolean activeContentSubscriberExists;

    private final Object contentSubGuard = new Object();
    private Queue<HttpContentSubscriberEvent<?>> pendingContentSubs; /*Guarded by contentSubGuard*/

    public HttpServerToConnectionBridge(MetricEventsSubject<ServerMetricsEvent<?>> eventsSubject) {
        this.eventsSubject = eventsSubject;
    }

    @Override
    protected void onOutboundHeaderWrite(HttpMessage httpMsg, ChannelPromise promise, final long startTimeMillis) {
        eventsSubject.onEvent(RESPONSE_WRITE_START);
    }

    @Override
    protected void onOutboundLastContentWrite(LastHttpContent msg, ChannelPromise promise,
                                              final long headerWriteStartTime) {
        promise.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    eventsSubject.onEvent(RESPONSE_WRITE_SUCCESS, Clock.onEndMillis(headerWriteStartTime));
                } else {
                    eventsSubject.onEvent(RESPONSE_WRITE_FAILED, Clock.onEndMillis(headerWriteStartTime),
                                          future.cause());
                }
            }
        });
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
    protected Object newHttpObject(Object nextItem, Channel channel) {
        eventsSubject.onEvent(REQUEST_HEADERS_RECEIVED);
        return new HttpServerRequestImpl<>((HttpRequest) nextItem, channel);
    }

    @Override
    protected void onContentReceived() {
        eventsSubject.onEvent(REQUEST_CONTENT_RECEIVED);
    }

    @Override
    protected void onContentReceiveComplete(long receiveStartTimeMillis) {
        eventsSubject.onEvent(REQUEST_RECEIVE_COMPLETE,
                              Clock.onEndMillis(receiveStartTimeMillis));
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        super.channelReadComplete(ctx);
        ctx.flush(); /*This is a no-op if there is nothing to flush but supports HttpServerResponse.flushOnlyOnReadComplete()*/
    }
}
