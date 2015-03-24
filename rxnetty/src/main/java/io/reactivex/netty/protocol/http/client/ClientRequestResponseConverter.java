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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.DecoderResult;
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
import io.reactivex.netty.channel.AbstractConnectionEvent;
import io.reactivex.netty.channel.NewRxConnectionEvent;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.client.ConnectionReuseEvent;
import io.reactivex.netty.client.PooledConnectionReleasedEvent;
import io.reactivex.netty.metrics.Clock;
import io.reactivex.netty.metrics.MetricEventsSubject;
import io.reactivex.netty.protocol.http.UnicastContentSubject;
import io.reactivex.netty.util.MultipleFutureListener;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Action0;

import java.io.IOException;

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

    private ResponseState responseState; /*State associated with this handler. Since a handler instance is ALWAYS invoked by the same thread, this need not be thread-safe*/

    public static final IOException CONN_CLOSE_BEFORE_RESPONSE = new IOException("Connection closed by peer before sending a response.");

    public ClientRequestResponseConverter(MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject) {
        this.eventsSubject = eventsSubject;
        responseState = new ResponseState();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Class<?> recievedMsgClass = msg.getClass();

        /**
         *  Issue: https://github.com/Netflix/RxNetty/issues/129
         *  The state changes in a different method userEventTriggered() when the connection is reused. If the
         *  connection reuse event is generated as part of execution of this method (for the specific issue, as part of
         *  super.channelRead(ctx, rxResponse); below) it will so happen that we invoke onComplete (below code when the
         *  first response completes) on the new subject as opposed to the old response subject.
         */
        final ResponseState stateToUse = responseState;

        if (HttpResponse.class.isAssignableFrom(recievedMsgClass)) {
            stateToUse.responseReceiveStartTimeMillis = Clock.newStartTimeMillis();
            eventsSubject.onEvent(HttpClientMetricsEvent.RESPONSE_HEADER_RECEIVED);
            HttpResponse response = (HttpResponse) msg;
            DecoderResult decoderResult = response.getDecoderResult();
            if (decoderResult.isFailure()) {
                ctx.channel().attr(DISCARD_CONNECTION).set(true); // Netty rejects all data after decode failure.
                                                                  // Issue: https://github.com/netty/netty/issues/3362
                stateToUse.sendOnError(decoderResult.cause());
            } else {
                @SuppressWarnings({"rawtypes", "unchecked"})
                HttpClientResponse rxResponse = new HttpClientResponse(response, stateToUse.contentSubject);
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
        }

        if (HttpContent.class.isAssignableFrom(recievedMsgClass)) {// This will be executed if the incoming message is a FullHttpResponse or only HttpContent.
            eventsSubject.onEvent(HttpClientMetricsEvent.RESPONSE_CONTENT_RECEIVED);
            ByteBuf content = ((ByteBufHolder) msg).content();
            if (LastHttpContent.class.isAssignableFrom(recievedMsgClass)) {
                stateToUse.responseReceiveComplete();
                if (content.isReadable()) {
                    invokeContentOnNext(content, stateToUse);
                } else {
                    // CompositeByteBuf and possibly other implementations may still need to be
                    // released event if they are not readable.
                    ReferenceCountUtil.release(content);
                }
                stateToUse.sendOnComplete();
            } else {
                invokeContentOnNext(content, stateToUse);
            }

        } else if(!HttpResponse.class.isAssignableFrom(recievedMsgClass)){
            invokeContentOnNext(msg, stateToUse);
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        Class<?> recievedMsgClass = msg.getClass();

        final ResponseState stateToUse = responseState;

        if (HttpClientRequest.class.isAssignableFrom(recievedMsgClass)) {
            HttpClientRequest<?> rxRequest = (HttpClientRequest<?>) msg;
            MultipleFutureListener allWritesListener = new MultipleFutureListener(promise);

            Observable<?> contentSource = null;

            switch (rxRequest.getContentSourceType()) {
                case Raw:
                    if (!rxRequest.getHeaders().isContentLengthSet()) {
                        rxRequest.getHeaders().add(HttpHeaders.Names.TRANSFER_ENCODING, HttpHeaders.Values.CHUNKED);
                    }
                    contentSource = rxRequest.getRawContentSource();
                    break;
                case Typed:
                    if (!rxRequest.getHeaders().isContentLengthSet()) {
                        rxRequest.getHeaders().add(HttpHeaders.Names.TRANSFER_ENCODING, HttpHeaders.Values.CHUNKED);
                    }
                    contentSource = rxRequest.getContentSource();
                    break;
                case Absent:
                    if (!rxRequest.getHeaders().isContentLengthSet() && rxRequest.getMethod() != HttpMethod.GET) {
                        rxRequest.getHeaders().set(HttpHeaders.Names.CONTENT_LENGTH, 0);
                    }
                    break;
            }

            writeHttpHeaders(ctx, rxRequest, allWritesListener); // In all cases, write headers first.

            if (null != contentSource) { // If content present then write Last Content after all content is written.
                if (!rxRequest.getHeaders().isContentLengthSet()) {
                    rxRequest.getHeaders().add(HttpHeaders.Names.TRANSFER_ENCODING, HttpHeaders.Values.CHUNKED);
                }
                writeContent(ctx, allWritesListener, contentSource, promise, rxRequest, stateToUse);
            } else { // If no content then write Last Content immediately.
                // In order for netty's codec to understand that HTTP request writing is over, we always have to write the
                // LastHttpContent irrespective of whether it is chunked or not.
                writeLastHttpContent(ctx, allWritesListener, rxRequest, stateToUse);
            }

        } else {
            ctx.write(msg, promise); // pass through, since we do not understand this message.
        }
    }

    protected void writeLastHttpContent(ChannelHandlerContext ctx, MultipleFutureListener allWritesListener,
                                        HttpClientRequest<?> rxRequest,
                                        final ResponseState responseState) {
        writeAContentChunk(ctx, allWritesListener, new DefaultLastHttpContent())
                .addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        responseState.nowWaitingForResponse();
                    }
                });
        rxRequest.onWriteComplete();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof ConnectionReuseEvent) {
            responseState = new ResponseState(); // Reset the state on reuse.
            responseState.setConnection((AbstractConnectionEvent<?>) evt);
        } else if (evt instanceof NewRxConnectionEvent) {
            responseState.setConnection((AbstractConnectionEvent<?>) evt);
        } else if (evt instanceof PooledConnectionReleasedEvent) {
            responseState.onConnectionClose();
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        responseState.onConnectionClose();
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        responseState.sendOnError(cause);
        super.exceptionCaught(ctx, cause);
    }

    @SuppressWarnings("unchecked")
    private static void invokeContentOnNext(Object nextObject, ResponseState stateToUse) {
        try {
            stateToUse.contentSubject.onNext(nextObject);
        } catch (ClassCastException e) {
            stateToUse.contentSubject.onError(e);
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

    private void writeContent(final ChannelHandlerContext ctx, final MultipleFutureListener allWritesListener,
                              final Observable<?> contentSource, final ChannelPromise promise,
                              final HttpClientRequest<?> rxRequest, final ResponseState responseState) {
        contentSource.subscribe(new Subscriber<Object>() {
            @Override
            public void onCompleted() {
                writeLastHttpContent(ctx, allWritesListener, rxRequest, responseState);
            }

            @Override
            public void onError(Throwable e) {
                eventsSubject.onEvent(HttpClientMetricsEvent.REQUEST_CONTENT_SOURCE_ERROR, e);
                promise.tryFailure(e);
                rxRequest.onWriteComplete();
            }

            @Override
            public void onNext(Object chunk) {
                writeAContentChunk(ctx, allWritesListener, chunk);
            }
        });
    }

    private ChannelFuture writeAContentChunk(ChannelHandlerContext ctx, MultipleFutureListener allWritesListener,
                                             Object chunk) {
        eventsSubject.onEvent(HttpClientMetricsEvent.REQUEST_CONTENT_WRITE_START);
        final long startTimeMillis = Clock.newStartTimeMillis();
        ChannelFuture writeFuture = ctx.write(chunk);
        addWriteCompleteEvents(writeFuture, startTimeMillis, HttpClientMetricsEvent.REQUEST_CONTENT_WRITE_SUCCESS,
                               HttpClientMetricsEvent.REQUEST_CONTENT_WRITE_FAILED);
        allWritesListener.listen(writeFuture);
        return writeFuture;
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

    private enum ResponseStateProcessingStage { Created, WaitingForResponse, ResponseReceived, Finished }

    /**
     * All state for this handler. At any point we need to invoke any method outside of this handler, this state should
     * be stored in a local variable and used after the external call finishes. Failure to do so will cause race
     * conditions in us using different state before and after the method call specifically if the external call ends
     * up generating a user generated event and triggering {@link #userEventTriggered(ChannelHandlerContext, Object)}
     * which in turn changes this state.
     *
     * Issue: https://github.com/Netflix/RxNetty/issues/129
     */
    private final class ResponseState {

        private ResponseStateProcessingStage stage = ResponseStateProcessingStage.Created;
        @SuppressWarnings("rawtypes") private final UnicastContentSubject contentSubject; // The type of this subject can change at runtime because a user can convert the content at runtime.
        @SuppressWarnings("rawtypes") private Observer connInputObsrvr;
        @SuppressWarnings("rawtypes") private ObservableConnection connection;

        private long responseReceiveStartTimeMillis; // Reset every time we receive a header.

        private ResponseState() {
            contentSubject = UnicastContentSubject.createWithoutNoSubscriptionTimeout(new Action0() {
                @Override
                public void call() {
                    if (ResponseStateProcessingStage.Finished != stage && null != connection) {
                        if (ResponseStateProcessingStage.WaitingForResponse == stage) {// If the response was not completed, the connection is not usable any further.
                            // It is important to do this here before closing the connection. Not doing so will return
                            // the pooled connection to the pool, before discarding the connection. This will cause,
                            // reuse of connections which are supposed to be discarded.
                            connection.getChannel().attr(DISCARD_CONNECTION).set(true);
                        }
                        connection.close();
                    }
                }
            });// Timeout handling is done dynamically by the client.
        }

        private void sendOnError(Throwable error) {
            responseReceiveComplete();
            eventsSubject.onEvent(HttpClientMetricsEvent.RESPONSE_FAILED,
                                  Clock.onEndMillis(responseReceiveStartTimeMillis));

            contentSubject.onError(error);
            connInputObsrvr.onError(error);
            connection.close(); // Close before sending on complete results in more predictable behavior for clients
                                // listening for response complete and expecting connection close.
        }

        private void sendOnComplete() {
            eventsSubject.onEvent(HttpClientMetricsEvent.RESPONSE_RECEIVE_COMPLETE,
                                  Clock.onEndMillis(responseReceiveStartTimeMillis));

            connection.close(); // Close before sending on complete results in more predictable behavior for clients
            // listening for response complete and expecting connection close.
            contentSubject.onCompleted();
            connInputObsrvr.onCompleted();
        }

        private void setConnection(AbstractConnectionEvent<?> connectionEvent) {
            connection = connectionEvent.getConnection();
            connInputObsrvr = connectionEvent.getConnectedObserver();
        }

        private void nowWaitingForResponse() {
            stage = ResponseStateProcessingStage.WaitingForResponse;
        }

        private void responseReceiveComplete() {
            stage = ResponseStateProcessingStage.ResponseReceived;
        }

        private void onConnectionClose() {
            if (ResponseStateProcessingStage.WaitingForResponse == stage) {
                if (null != connection) {
                    // If the response was not completed, the connection is not usable any further.
                    connection.getChannel().attr(DISCARD_CONNECTION).set(true);
                }
                if (null != connInputObsrvr) {
                    connInputObsrvr.onError(CONN_CLOSE_BEFORE_RESPONSE);
                }
            }
            stage = ResponseStateProcessingStage.Finished;
        }
    }
}
