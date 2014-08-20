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

package io.reactivex.netty.protocol.http.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.ReferenceCountUtil;
import io.reactivex.netty.metrics.Clock;
import io.reactivex.netty.metrics.MetricEventsSubject;
import io.reactivex.netty.protocol.http.UnicastContentSubject;
import io.reactivex.netty.server.ServerMetricsEvent;

import java.util.concurrent.TimeUnit;

/**
 * A channel handler for {@link HttpServer} to convert netty's http request/response objects to {@link HttpServer}'s
 * request/response objects. It handles the following message types:
 *
 * <h2>Reading Objects</h2>
 * <ul>
 <li>{@link HttpRequest}: Converts it to {@link HttpServerRequest } </li>
 <li>{@link HttpContent}: Converts it to the content of the previously generated
{@link HttpServerRequest }</li>
 <li>{@link FullHttpRequest}: Converts it to a {@link HttpServerRequest } with pre-populated content observable.</li>
 <li>Any other object: Assumes that it is a transformed HTTP content & pass it through to the content observable.</li>
 </ul>
 *
 * <h2>Writing Objects</h2>
 * <ul>
 <li>{@link HttpServerResponse}: Converts it to a {@link HttpResponse}</li>
 <li>{@link ByteBuf} to an {@link HttpContent}</li>
 <li>Pass through any other message type.</li>
 </ul>
 *
 * @author Nitesh Kant
 */
public class ServerRequestResponseConverter extends ChannelDuplexHandler {

    private final MetricEventsSubject<ServerMetricsEvent<?>> eventsSubject;
    private final long requestContentSubscriptionTimeoutMs;
    @SuppressWarnings("rawtypes") private HttpServerRequest rxRequest;

    public ServerRequestResponseConverter(MetricEventsSubject<ServerMetricsEvent<?>> eventsSubject,
                                          long requestContentSubscriptionTimeoutMs) {
        this.eventsSubject = eventsSubject;
        this.requestContentSubscriptionTimeoutMs = requestContentSubscriptionTimeoutMs;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Class<?> recievedMsgClass = msg.getClass();

        @SuppressWarnings("rawtypes") UnicastContentSubject contentSubject =
                UnicastContentSubject.create(requestContentSubscriptionTimeoutMs, TimeUnit.MILLISECONDS);

        if (HttpRequest.class.isAssignableFrom(recievedMsgClass)) {
            eventsSubject.onEvent(HttpServerMetricsEvent.REQUEST_HEADERS_RECEIVED);

            @SuppressWarnings({"rawtypes", "unchecked"})
            HttpServerRequest rxRequest = new HttpServerRequest((HttpRequest) msg, contentSubject);
            this.rxRequest = rxRequest;

            super.channelRead(ctx, rxRequest);
        }

        if (HttpContent.class.isAssignableFrom(recievedMsgClass)) {// This will be executed if the incoming message is a FullHttpRequest or only HttpContent.
            ByteBuf content = ((ByteBufHolder) msg).content();
            eventsSubject.onEvent(HttpServerMetricsEvent.REQUEST_CONTENT_RECEIVED);
            invokeContentOnNext(content, contentSubject);
            if (LastHttpContent.class.isAssignableFrom(recievedMsgClass)) {
                long durationInMs = rxRequest.onProcessingEnd();
                eventsSubject.onEvent(HttpServerMetricsEvent.REQUEST_RECEIVE_COMPLETE, durationInMs);
                contentSubject.onCompleted();
            }
        } else {
            invokeContentOnNext(msg, contentSubject);
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        Class<?> recievedMsgClass = msg.getClass();

        final long startTimeMillis = Clock.newStartTimeMillis();

        if (HttpServerResponse.class.isAssignableFrom(recievedMsgClass)) {
            @SuppressWarnings("rawtypes")
            HttpServerResponse rxResponse = (HttpServerResponse) msg;
            eventsSubject.onEvent(HttpServerMetricsEvent.RESPONSE_HEADERS_WRITE_START);
            addWriteCompleteEvents(promise, startTimeMillis, HttpServerMetricsEvent.RESPONSE_HEADERS_WRITE_SUCCESS,
                                   HttpServerMetricsEvent.RESPONSE_HEADERS_WRITE_FAILED);
            super.write(ctx, rxResponse.getNettyResponse(), promise);
        } else if (ByteBuf.class.isAssignableFrom(recievedMsgClass)) {
            eventsSubject.onEvent(HttpServerMetricsEvent.RESPONSE_CONTENT_WRITE_START);
            addWriteCompleteEvents(promise, startTimeMillis, HttpServerMetricsEvent.RESPONSE_CONTENT_WRITE_SUCCESS,
                                   HttpServerMetricsEvent.RESPONSE_CONTENT_WRITE_FAILED);
            HttpContent content = new DefaultHttpContent((ByteBuf) msg);
            super.write(ctx, content, promise);
        } else {
            super.write(ctx, msg, promise); // pass through, since we do not understand this message.
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        super.channelReadComplete(ctx);
        ctx.pipeline().flush(); // If there is nothing to flush, this is a short-circuit in netty.
    }

    private void addWriteCompleteEvents(ChannelPromise promise, final long startTimeMillis,
                                        final HttpServerMetricsEvent<HttpServerMetricsEvent.EventType> successEvent,
                                        final HttpServerMetricsEvent<HttpServerMetricsEvent.EventType> failureEvent) {
        promise.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    eventsSubject.onEvent(successEvent, Clock.onEndMillis(startTimeMillis));
                } else {
                    eventsSubject.onEvent(failureEvent, Clock.onEndMillis(startTimeMillis), future.cause());
                }
            }
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void invokeContentOnNext(Object nextObject, UnicastContentSubject contentSubject) {
        try {
            contentSubject.onNext(nextObject);
        } catch (ClassCastException e) {
            contentSubject.onError(e);
        } finally {
            ReferenceCountUtil.release(nextObject);
        }
    }
}
