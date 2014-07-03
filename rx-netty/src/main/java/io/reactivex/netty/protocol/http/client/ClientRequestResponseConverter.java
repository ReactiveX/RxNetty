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

package io.reactivex.netty.protocol.http.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.client.ConnectionReuseEvent;
import io.reactivex.netty.metrics.Clock;
import io.reactivex.netty.metrics.MetricEventsSubject;
import io.reactivex.netty.protocol.http.MultipleFutureListener;
import io.reactivex.netty.serialization.ContentTransformer;
import rx.subjects.PublishSubject;

/**
 * A channel handler for {@link HttpClient} to convert netty's http request/response objects to {@link HttpClient}'s
 * request/response objects. It handles the following message types:
 *
 * <h2>Reading Objects</h2>
 * <ul>
 <li>{@link HttpResponse}: Converts it to {@link HttpClientResponse} </li>
 <li>{@link HttpContent}: Converts it to the content of the previously generated
{@link HttpClientResponse}</li>
 <li>{@link FullHttpResponse}: Converts it to a {@link HttpClientResponse} with pre-populated content observable.</li>
 <li>Any other object: Assumes that it is a transformed HTTP content & pass it through to the content observable.</li>
 </ul>
 *
 * <h2>Writing Objects</h2>
 * <ul>
 <li>{@link HttpClientRequest}: Converts it to a {@link HttpRequest}</li>
 <li>{@link ByteBuf} to an {@link HttpContent}</li>
 <li>Pass through any other message type.</li>
 </ul>
 *
 * @author Nitesh Kant
 */
public class ClientRequestResponseConverter extends ChannelDuplexHandler {

    /**
     * This attribute stores the value of any dynamic idle timeout value sent via an HTTP keep alive header.
     * This follows the proposal specified here: http://tools.ietf.org/id/draft-thomson-hybi-http-timeout-01.html
     * The attribute can be extracted from an HTTP response header using the helper method
     * {@link HttpClientResponse#getKeepAliveTimeoutSeconds()}
     */
    public static final AttributeKey<Long> KEEP_ALIVE_TIMEOUT_MILLIS_ATTR = AttributeKey.valueOf("rxnetty_http_conn_keep_alive_timeout_millis");
    public static final AttributeKey<Boolean> DISCARD_CONNECTION = AttributeKey.valueOf("rxnetty_http_discard_connection");
    private final MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject;

    @SuppressWarnings("rawtypes") private PublishSubject contentSubject; // The type of this subject can change at runtime because a user can convert the content at runtime.
    private long responseReceiveStartTimeMillis; // Reset every time we receive a header.

    public ClientRequestResponseConverter(MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject) {
        this.eventsSubject = eventsSubject;
        contentSubject = PublishSubject.create();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Class<?> recievedMsgClass = msg.getClass();

        /**
         *  Issue: https://github.com/Netflix/RxNetty/issues/129
         *  The contentSubject changes in a different method userEventTriggered() when the connection is reused. If the
         *  connection reuse event is generated as part of execution of this method (for the specific issue, as part of
         *  super.channelRead(ctx, rxResponse); below) it will so happen that we invoke onComplete (below code when the
         *  first response completes) on the new subject as opposed to the old response subject.
         */
        @SuppressWarnings("rawtypes") final PublishSubject subjectToUse = contentSubject;

        if (HttpResponse.class.isAssignableFrom(recievedMsgClass)) {
            responseReceiveStartTimeMillis = Clock.newStartTimeMillis();
            eventsSubject.onEvent(HttpClientMetricsEvent.RESPONSE_HEADER_RECEIVED);
            @SuppressWarnings({"rawtypes", "unchecked"})
            HttpResponse response = (HttpResponse) msg;

            @SuppressWarnings({"rawtypes", "unchecked"})
            HttpClientResponse rxResponse = new HttpClientResponse(response, subjectToUse);
            Long keepAliveTimeoutSeconds = rxResponse.getKeepAliveTimeoutSeconds();
            if (null != keepAliveTimeoutSeconds) {
                ctx.channel().attr(KEEP_ALIVE_TIMEOUT_MILLIS_ATTR).set(keepAliveTimeoutSeconds * 1000);
            }

            if (!rxResponse.getHeaders().isKeepAlive()) {
                ctx.channel().attr(DISCARD_CONNECTION).set(true);
            }
            super.channelRead(ctx, rxResponse); // For FullHttpResponse, this assumes that after this call returns,
                                                // someone has subscribed to the content observable, if not the content will be lost.
        }

        if (HttpContent.class.isAssignableFrom(recievedMsgClass)) {// This will be executed if the incoming message is a FullHttpResponse or only HttpContent.
            eventsSubject.onEvent(HttpClientMetricsEvent.RESPONSE_CONTENT_RECEIVED);
            ByteBuf content = ((ByteBufHolder) msg).content();
            if (content.isReadable()) {
                invokeContentOnNext(content);
            }
            if (LastHttpContent.class.isAssignableFrom(recievedMsgClass)) {
                eventsSubject.onEvent(HttpClientMetricsEvent.RESPONSE_RECEIVE_COMPLETE,
                                      Clock.onEndMillis(responseReceiveStartTimeMillis));
                subjectToUse.onCompleted();
            }
        } else if(!HttpResponse.class.isAssignableFrom(recievedMsgClass)){
            invokeContentOnNext(msg);
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        Class<?> recievedMsgClass = msg.getClass();

        if (HttpClientRequest.class.isAssignableFrom(recievedMsgClass)) {
            HttpClientRequest<?> rxRequest = (HttpClientRequest<?>) msg;
            MultipleFutureListener allWritesListener = new MultipleFutureListener(promise);
            if (rxRequest.hasContentSource()) {
                if (!rxRequest.getHeaders().isContentLengthSet()) {
                    rxRequest.getHeaders().add(HttpHeaders.Names.TRANSFER_ENCODING, HttpHeaders.Values.CHUNKED);
                }
                writeHttpHeaders(ctx, rxRequest, allWritesListener);
                ContentSource<?> contentSource;
                if (rxRequest.hasRawContentSource()) {
                    contentSource = rxRequest.getRawContentSource();
                    @SuppressWarnings("rawtypes")
                    RawContentSource<?> rawContentSource = (RawContentSource) contentSource;
                    while (rawContentSource.hasNext()) {
                        @SuppressWarnings("rawtypes")
                        ContentTransformer transformer = rawContentSource.getTransformer();
                        @SuppressWarnings("unchecked")
                        ByteBuf byteBuf = transformer.transform(rawContentSource.next(), ctx.alloc());
                        writeContent(ctx, allWritesListener, byteBuf);
                    }
                } else {
                    contentSource = rxRequest.getContentSource();
                    while (contentSource.hasNext()) {
                        writeContent(ctx, allWritesListener, contentSource.next());
                    }
                }
            } else {
                if (!rxRequest.getHeaders().isContentLengthSet() && rxRequest.getMethod() != HttpMethod.GET) {
                    rxRequest.getHeaders().set(HttpHeaders.Names.CONTENT_LENGTH, 0);
                }
                writeHttpHeaders(ctx, rxRequest, allWritesListener);
            }

            // In order for netty's codec to understand that HTTP request writing is over, we always have to write the
            // LastHttpContent irrespective of whether it is chunked or not.
            writeContent(ctx, allWritesListener, new DefaultLastHttpContent());
        } else {
            ctx.write(msg, promise); // pass through, since we do not understand this message.
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof ConnectionReuseEvent) {
            contentSubject = PublishSubject.create(); // Reset the subject on reuse.
        }
        super.userEventTriggered(ctx, evt);
    }

    @SuppressWarnings("unchecked")
    private void invokeContentOnNext(Object nextObject) {
        try {
            contentSubject.onNext(nextObject);
        } catch (ClassCastException e) {
            contentSubject.onError(e);
        } finally {
            ReferenceCountUtil.release(nextObject);
        }
    }

    private void writeHttpHeaders(ChannelHandlerContext ctx, HttpClientRequest<?> rxRequest,
                                  MultipleFutureListener allWritesListener) {
        final long startTimeMillis = Clock.newStartTimeMillis();
        eventsSubject.onEvent(HttpClientMetricsEvent.REQUEST_HEADERS_WRITE_START);
        ChannelFuture writeFuture = ctx.write(rxRequest.getNettyRequest());
        addWriteCompleteEvents(writeFuture, startTimeMillis, HttpClientMetricsEvent.REQUEST_HEADERS_WRITE_SUCCESS,
                               HttpClientMetricsEvent.REQUEST_HEADERS_WRITE_FAILED);
        allWritesListener.listen(writeFuture);
    }

    private void writeContent(ChannelHandlerContext ctx, MultipleFutureListener allWritesListener, Object msg) {
        eventsSubject.onEvent(HttpClientMetricsEvent.REQUEST_CONTENT_WRITE_START);
        final long startTimeMillis = Clock.newStartTimeMillis();
        ChannelFuture writeFuture = ctx.write(msg);
        addWriteCompleteEvents(writeFuture, startTimeMillis, HttpClientMetricsEvent.REQUEST_CONTENT_WRITE_SUCCESS,
                               HttpClientMetricsEvent.REQUEST_CONTENT_WRITE_FAILED);
        allWritesListener.listen(writeFuture);
    }


    private void addWriteCompleteEvents(ChannelFuture future, final long startTimeMillis,
                                        final HttpClientMetricsEvent<HttpClientMetricsEvent.EventType> successEvent,
                                        final HttpClientMetricsEvent<HttpClientMetricsEvent.EventType> failureEvent) {
        future.addListener(new ChannelFutureListener() {
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

}
