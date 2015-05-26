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
package io.reactivex.netty.protocol.http.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.AttributeKey;
import io.reactivex.netty.events.Clock;
import io.reactivex.netty.protocol.http.client.events.HttpClientEventPublisher;
import io.reactivex.netty.protocol.http.client.internal.HttpClientResponseImpl;
import io.reactivex.netty.protocol.http.internal.AbstractHttpConnectionBridge;
import io.reactivex.netty.protocol.tcp.client.ClientConnectionToChannelBridge;
import io.reactivex.netty.protocol.tcp.client.ClientConnectionToChannelBridge.ConnectionResueEvent;
import io.reactivex.netty.protocol.tcp.client.ClientConnectionToChannelBridge.PooledConnectionReleaseEvent;
import rx.Subscriber;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

import static java.util.concurrent.TimeUnit.*;

public class HttpClientToConnectionBridge<C> extends AbstractHttpConnectionBridge<C> {

    /**
     * This attribute stores the value of any dynamic idle timeout value sent via an HTTP keep alive header.
     * This follows the proposal specified here: http://tools.ietf.org/id/draft-thomson-hybi-http-timeout-01.html
     * The attribute can be extracted from an HTTP response header using the helper method
     * {@link io.reactivex.netty.protocol.http.client.internal.HttpClientResponseImpl#getKeepAliveTimeoutSeconds()}
     */
    public static final AttributeKey<Long> KEEP_ALIVE_TIMEOUT_MILLIS_ATTR = AttributeKey.valueOf("rxnetty_http_conn_keep_alive_timeout_millis");

    private final HttpClientEventPublisher eventPublisher;

    public HttpClientToConnectionBridge(HttpClientEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    protected void onOutboundHeaderWrite(HttpMessage httpMsg, ChannelPromise promise, long startTimeMillis) {
        if (eventPublisher.publishingEnabled()) {
            eventPublisher.onRequestWriteStart();
        }
    }

    @Override
    protected void onOutboundLastContentWrite(LastHttpContent msg, ChannelPromise promise,
                                              final long headerWriteStartTimeMillis) {
        if (eventPublisher.publishingEnabled()) {
            promise.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (eventPublisher.publishingEnabled()) {
                        if (future.isSuccess()) {
                            eventPublisher.onRequestWriteComplete(Clock.onEndMillis(headerWriteStartTimeMillis),
                                                                  MILLISECONDS);
                        } else {
                            eventPublisher.onRequestWriteFailed(Clock.onEndMillis(headerWriteStartTimeMillis),
                                                                MILLISECONDS, future.cause());
                        }
                    }
                }
            });
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof ConnectionResueEvent) {
            resetSubscriptionState(connectionInputSubscriber);
            connectionInputSubscriber = null;
        } else if (PooledConnectionReleaseEvent.INSTANCE == evt) {
            onPooledConnectionRelease(connectionInputSubscriber);
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    protected void onClosedBeforeReceiveComplete(ConnectionInputSubscriber connectionInputSubscriber) {
        if (connectionInputSubscriber.getChannel().isActive()) {
            /*
             * If the close is triggerred by the user, the channel will be active.
             * If the response, isn't complete, then the connection can not be used.
             */
            connectionInputSubscriber.getChannel().attr(ClientConnectionToChannelBridge.DISCARD_CONNECTION).set(true);
        }
    }

    @Override
    protected boolean isInboundHeader(Object nextItem) {
        return nextItem instanceof HttpResponse;
    }

    @Override
    protected boolean isOutboundHeader(Object nextItem) {
        return nextItem instanceof HttpRequest;
    }

    @Override
    protected Object newHttpObject(Object nextItem, Channel channel) {
        final HttpResponse nettyResponse = (HttpResponse) nextItem;

        if (eventPublisher.publishingEnabled()) {
            eventPublisher.onResponseHeadersReceived(nettyResponse.status().code());
        }

        final HttpClientResponseImpl<C> rxResponse = new HttpClientResponseImpl<>(nettyResponse, channel);
        Long keepAliveTimeoutSeconds = rxResponse.getKeepAliveTimeoutSeconds();
        if (null != keepAliveTimeoutSeconds) {
            channel.attr(KEEP_ALIVE_TIMEOUT_MILLIS_ATTR).set(keepAliveTimeoutSeconds * 1000);
        }

        if (!rxResponse.isKeepAlive()) {
            channel.attr(ClientConnectionToChannelBridge.DISCARD_CONNECTION).set(true); /*Discard connection when done with this response.*/
        }

        return rxResponse;
    }

    @Override
    protected void onContentReceived() {
        if (eventPublisher.publishingEnabled()) {
            eventPublisher.onResponseContentReceived();
        }
    }

    @Override
    protected void onContentReceiveComplete(long receiveStartTimeMillis) {
        connectionInputSubscriber.onCompleted(); /*Unsubscribe from the input and hence close/release connection*/
        if (eventPublisher.publishingEnabled()) {
            eventPublisher.onResponseReceiveComplete(Clock.onEndMillis(receiveStartTimeMillis), MILLISECONDS);
        }
    }

    @Override
    protected void onNewContentSubscriber(final ConnectionInputSubscriber inputSubscriber, Subscriber<? super C> newSub) {
        newSub.add(Subscriptions.create(new Action0() {
            @Override
            public void call() {
                inputSubscriber.unsubscribe();
            }
        }));
    }

    private void onPooledConnectionRelease(ConnectionInputSubscriber connectionInputSubscriber) {
        onChannelClose(connectionInputSubscriber);
    }
}
