/*
 * Copyright 2016 Netflix, Inc.
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
 *
 */
package io.reactivex.netty.protocol.http.client.internal;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.AttributeKey;
import io.reactivex.netty.client.ClientConnectionToChannelBridge;
import io.reactivex.netty.client.ClientConnectionToChannelBridge.ConnectionReuseEvent;
import io.reactivex.netty.client.ClientConnectionToChannelBridge.PooledConnectionReleaseEvent;
import io.reactivex.netty.client.pool.PooledConnection;
import io.reactivex.netty.events.Clock;
import io.reactivex.netty.events.EventAttributeKeys;
import io.reactivex.netty.events.EventPublisher;
import io.reactivex.netty.protocol.http.client.events.HttpClientEventsListener;
import io.reactivex.netty.protocol.http.internal.AbstractHttpConnectionBridge;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import static java.util.concurrent.TimeUnit.*;

public class HttpClientToConnectionBridge<C> extends AbstractHttpConnectionBridge<C> {

    /**
     * This attribute stores the value of any dynamic idle timeout value sent via an HTTP keep alive header.
     * This follows the proposal specified here: http://tools.ietf.org/id/draft-thomson-hybi-http-timeout-01.html
     * The attribute can be extracted from an HTTP response header using the helper method
     * {@link HttpClientResponseImpl#getKeepAliveTimeoutSeconds()}
     */
    public static final AttributeKey<Long> KEEP_ALIVE_TIMEOUT_MILLIS_ATTR =
            PooledConnection.DYNAMIC_CONN_KEEP_ALIVE_TIMEOUT_MS;

    private HttpClientEventsListener eventsListener;
    private EventPublisher eventPublisher;
    private String hostHeader;
    private long requestWriteCompletionTimeNanos;

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        eventsListener = ctx.channel().attr(HttpChannelProvider.HTTP_CLIENT_EVENT_LISTENER).get();
        eventPublisher = ctx.channel().attr(EventAttributeKeys.EVENT_PUBLISHER).get();
        super.handlerAdded(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        SocketAddress remoteAddr = ctx.channel().remoteAddress();
        if (remoteAddr instanceof InetSocketAddress) {
            InetSocketAddress inetSock = (InetSocketAddress) remoteAddr;
            String hostString = inetSock.getHostString(); // Don't use hostname that does a DNS lookup.
            hostHeader = hostString + ':' + inetSock.getPort();
        }
        super.channelActive(ctx);
    }

    @Override
    protected void beforeOutboundHeaderWrite(HttpMessage httpMsg, ChannelPromise promise, long startTimeNanos) {
        /*Reset on every request write, we do not currently support pipelining, otherwise, this should be stored in a
        queue.*/
        requestWriteCompletionTimeNanos = -1;
        if (null != hostHeader) {
            if (!httpMsg.headers().contains(HttpHeaderNames.HOST)) {
                httpMsg.headers().set(HttpHeaderNames.HOST, hostHeader);
            }
        }
        if (eventPublisher.publishingEnabled()) {
            eventsListener.onRequestWriteStart();
        }
    }

    @Override
    protected void onOutboundLastContentWrite(LastHttpContent msg, ChannelPromise promise,
                                              final long headerWriteStartTimeNanos) {
        if (eventPublisher.publishingEnabled()) {
            promise.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (eventPublisher.publishingEnabled()) {
                        requestWriteCompletionTimeNanos = Clock.newStartTimeNanos();
                        if (future.isSuccess()) {
                            eventsListener.onRequestWriteComplete(Clock.onEndNanos(headerWriteStartTimeNanos),
                                                                  NANOSECONDS);
                        } else {
                            eventsListener.onRequestWriteFailed(Clock.onEndNanos(headerWriteStartTimeNanos),
                                                                NANOSECONDS, future.cause());
                        }
                    }
                }
            });
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof ConnectionReuseEvent) {
            resetSubscriptionState(connectionInputSubscriber);
            connectionInputSubscriber = null;
        } else if (PooledConnectionReleaseEvent.INSTANCE == evt) {
            onPooledConnectionRelease(connectionInputSubscriber);
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    protected void onClosedBeforeReceiveComplete(Channel channel) {
        if (channel.isActive()) {
            /*
             * If the close is triggerred by the user, the channel will be active.
             * If the response, isn't complete, then the connection can not be used.
             */
            channel.attr(ClientConnectionToChannelBridge.DISCARD_CONNECTION).set(true);
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
            long duration = -1;
            if (requestWriteCompletionTimeNanos != -1) {
                duration = Clock.onEndNanos(requestWriteCompletionTimeNanos);
            }
            eventsListener.onResponseHeadersReceived(nettyResponse.status().code(), duration, NANOSECONDS);
        }

        final HttpClientResponseImpl<C> rxResponse = HttpClientResponseImpl.unsafeCreate(nettyResponse);
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
            eventsListener.onResponseContentReceived();
        }
    }

    @Override
    protected void onContentReceiveComplete(long receiveStartTimeNanos) {
        connectionInputSubscriber.onCompleted(); /*Unsubscribe from the input and hence close/release connection*/
        if (eventPublisher.publishingEnabled()) {
            long headerWriteStart = getHeaderWriteStartTimeNanos();
            eventsListener.onResponseReceiveComplete(Clock.onEndNanos(receiveStartTimeNanos), NANOSECONDS);
            eventsListener.onRequestProcessingComplete(Clock.onEndNanos(headerWriteStart), NANOSECONDS);
        }
    }

    private void onPooledConnectionRelease(ConnectionInputSubscriber connectionInputSubscriber) {
        onChannelClose(connectionInputSubscriber);
    }
}
