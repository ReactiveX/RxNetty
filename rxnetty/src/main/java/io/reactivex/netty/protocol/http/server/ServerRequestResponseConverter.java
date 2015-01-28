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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.ReferenceCountUtil;
import io.reactivex.netty.metrics.Clock;
import io.reactivex.netty.metrics.MetricEventsSubject;
import io.reactivex.netty.protocol.http.UnicastContentSubject;
import io.reactivex.netty.server.ServerMetricsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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

    private static final Logger logger = LoggerFactory.getLogger(ServerRequestResponseConverter.class);

    public static final IOException CONN_CLOSE_BEFORE_REQUEST_COMPLETE = new IOException("Connection closed by peer before sending the entire request.");

    private final MetricEventsSubject<ServerMetricsEvent<?>> eventsSubject;
    private final long requestContentSubscriptionTimeoutMs;
    private RequestState currentRequestState;

    public ServerRequestResponseConverter(MetricEventsSubject<ServerMetricsEvent<?>> eventsSubject,
                                          long requestContentSubscriptionTimeoutMs) {
        this.eventsSubject = eventsSubject;
        this.requestContentSubscriptionTimeoutMs = requestContentSubscriptionTimeoutMs;
        currentRequestState = new RequestState();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Class<?> recievedMsgClass = msg.getClass();

        final RequestState stateToUse = currentRequestState;
        boolean isHttpRequest = false;

        if (HttpRequest.class.isAssignableFrom(recievedMsgClass)) {
            isHttpRequest = true;
            HttpRequest httpRequest = (HttpRequest) msg;
            DecoderResult decoderResult = httpRequest.getDecoderResult();
            if (decoderResult.isSuccess()) {
                eventsSubject.onEvent(HttpServerMetricsEvent.REQUEST_HEADERS_RECEIVED);
                stateToUse.createRxRequest(ctx, (HttpRequest) msg); // Update the state to use.
                stateToUse.onProcessingStart(Clock.newStartTimeMillis());
                super.channelRead(ctx, stateToUse.rxRequest);
            } else {
                logger.error("Invalid HTTP request recieved. Decoder error.", decoderResult.cause());
                // As per the spec, we should send 414/431 for URI too long and headers too long, but we do not have
                // enough info to decide which kind of failure has caused this error here.
                DefaultFullHttpResponse errResponse = new DefaultFullHttpResponse(httpRequest.getProtocolVersion(),
                                                                                  HttpResponseStatus.REQUEST_HEADER_FIELDS_TOO_LARGE);
                // Netty rejects all data after decode failure. So, closing connection here.
                // Issue: https://github.com/netty/netty/issues/3362
                errResponse.headers()
                           .set(Names.CONNECTION, HttpHeaders.Values.CLOSE)
                           .set(Names.CONTENT_LENGTH, 0);
                ctx.writeAndFlush(errResponse).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (!future.isSuccess()) {
                            logger.error("Failed to write response for invalid HTTP request.", future.cause());
                        }
                    }
                });
                return;
            }
        }

        if (HttpContent.class.isAssignableFrom(recievedMsgClass)) {// This will be executed if the incoming message is a FullHttpRequest or only HttpContent.
            ByteBuf content = ((ByteBufHolder) msg).content();
            eventsSubject.onEvent(HttpServerMetricsEvent.REQUEST_CONTENT_RECEIVED);
            invokeContentOnNext(content, stateToUse.contentSubject);
            if (LastHttpContent.class.isAssignableFrom(recievedMsgClass)) {
                stateToUse.onRequestComplete();
                currentRequestState = new RequestState(); // Reset the current state for the next request to arrive on this connection
            }
        } else if(!isHttpRequest) { // If it is not HttpContent and not HttpRequest then it is a custom user object that is just sent as content.
            invokeContentOnNext(msg, stateToUse.contentSubject);
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

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        currentRequestState.onConnectionClose();
        super.channelInactive(ctx);
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

    private final class RequestState {

        @SuppressWarnings("rawtypes") private HttpServerRequest rxRequest;
        @SuppressWarnings("rawtypes") private UnicastContentSubject contentSubject;
        private boolean isReadingRequest;

        @SuppressWarnings({"rawtypes", "unchecked"})
        private void createRxRequest(ChannelHandlerContext ctx, HttpRequest httpRequest) {
            contentSubject = UnicastContentSubject.create(requestContentSubscriptionTimeoutMs, TimeUnit.MILLISECONDS);
            rxRequest = new HttpServerRequest(ctx.channel(), httpRequest, contentSubject);
        }

        private void onProcessingStart(long startTimeMillis) {
            rxRequest.onProcessingStart(startTimeMillis);
            isReadingRequest = true;
        }

        private void onRequestComplete() {
            isReadingRequest = false;
            long durationInMs = rxRequest.onProcessingEnd();
            eventsSubject.onEvent(HttpServerMetricsEvent.REQUEST_RECEIVE_COMPLETE, durationInMs);
            contentSubject.onCompleted();
        }

        private void onConnectionClose() {
            if (isReadingRequest) {
                contentSubject.onError(CONN_CLOSE_BEFORE_REQUEST_COMPLETE);
            }
        }
    }
}
