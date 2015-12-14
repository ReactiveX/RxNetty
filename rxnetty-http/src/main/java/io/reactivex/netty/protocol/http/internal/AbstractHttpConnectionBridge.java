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
 *
 */
package io.reactivex.netty.protocol.http.internal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.EmptyArrays;
import io.reactivex.netty.channel.ConnectionInputSubscriberEvent;
import io.reactivex.netty.channel.ConnectionInputSubscriberReplaceEvent;
import io.reactivex.netty.channel.SubscriberToChannelFutureBridge;
import io.reactivex.netty.events.Clock;
import io.reactivex.netty.protocol.http.internal.AbstractHttpConnectionBridge.State.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Producer;
import rx.Subscriber;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

import java.nio.channels.ClosedChannelException;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpHeaderValues.*;

public abstract class AbstractHttpConnectionBridge<C> extends ChannelDuplexHandler {

    private static final Logger logger = LoggerFactory.getLogger(AbstractHttpConnectionBridge.class);

    public static final AttributeKey<Boolean> CONNECTION_UPGRADED =
            AttributeKey.valueOf("rxnetty_http_upgraded_connection");

    @SuppressWarnings("ThrowableInstanceNeverThrown")
    private static final IllegalStateException ONLY_ONE_CONTENT_INPUT_SUB_ALLOWED =
            new IllegalStateException("Only one subscriber allowed for HTTP content.");
    @SuppressWarnings("ThrowableInstanceNeverThrown")
    private static final IllegalStateException LAZY_CONTENT_INPUT_SUB =
            new IllegalStateException("Channel is set to auto-read but the subscription was lazy.");
    @SuppressWarnings("ThrowableInstanceNeverThrown")
    private static final IllegalStateException CONTENT_ARRIVED_WITH_NO_SUB =
            new IllegalStateException("HTTP Content received but no subscriber was registered.");
    @SuppressWarnings("ThrowableInstanceNeverThrown")
    private static final ClosedChannelException CLOSED_CHANNEL_EXCEPTION = new ClosedChannelException();

    static {
        ONLY_ONE_CONTENT_INPUT_SUB_ALLOWED.setStackTrace(EmptyArrays.EMPTY_STACK_TRACE);
        LAZY_CONTENT_INPUT_SUB.setStackTrace(EmptyArrays.EMPTY_STACK_TRACE);
        CONTENT_ARRIVED_WITH_NO_SUB.setStackTrace(EmptyArrays.EMPTY_STACK_TRACE);
        CLOSED_CHANNEL_EXCEPTION.setStackTrace(EmptyArrays.EMPTY_STACK_TRACE);
    }

    protected ConnectionInputSubscriber connectionInputSubscriber;
    private final UnsafeEmptySubscriber<C> emptyContentSubscriber;
    private long headerWriteStartTimeNanos;

    protected AbstractHttpConnectionBridge() {
        emptyContentSubscriber = new UnsafeEmptySubscriber<>("Error while waiting for HTTP content.");
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        Object msgToWrite = msg;

        if (isOutboundHeader(msg)) {
            /*Reset on every header write, when we support pipelining, this should be a queue.*/
            headerWriteStartTimeNanos = Clock.newStartTimeNanos();
            HttpMessage httpMsg = (HttpMessage) msg;
            if (!HttpUtil.isContentLengthSet(httpMsg) && !HttpVersion.HTTP_1_0.equals(httpMsg.protocolVersion())) {
                // If there is no content length we need to specify the transfer encoding as chunked as we always
                // send data in multiple HttpContent.
                // On the other hand, if someone wants to not have chunked encoding, adding content-length will work
                // as expected.
                httpMsg.headers().set(TRANSFER_ENCODING, CHUNKED);
            }

            beforeOutboundHeaderWrite(httpMsg, promise, headerWriteStartTimeNanos);

        } else if (msg instanceof String) {
            msgToWrite = ctx.alloc().buffer().writeBytes(((String) msg).getBytes());
        } else if (msg instanceof byte[]) {
            msgToWrite = ctx.alloc().buffer().writeBytes((byte[]) msg);
        } else if (msg instanceof LastHttpContent) {
            onOutboundLastContentWrite((LastHttpContent) msg, promise, headerWriteStartTimeNanos);
        }

        super.write(ctx, msgToWrite, promise);
    }

    protected abstract void beforeOutboundHeaderWrite(HttpMessage httpMsg, ChannelPromise promise, long startTimeNanos);

    protected abstract void onOutboundLastContentWrite(LastHttpContent msg, ChannelPromise promise,
                                                       long headerWriteStartTimeNanos);

    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, Object evt) throws Exception {

        Object eventToPropagateFurther = evt;
        Boolean connUpgradedAttr = ctx.channel().attr(CONNECTION_UPGRADED).get();
        boolean connUpgraded = null != connUpgradedAttr ? connUpgradedAttr : false;

        if (evt instanceof ConnectionInputSubscriberEvent) {

            @SuppressWarnings({ "unchecked", "rawtypes" })
            ConnectionInputSubscriberEvent orig = (ConnectionInputSubscriberEvent) evt;

            if (!connUpgraded) {
                /*Local copy to refer from the channel close listener. As the instance level copy can change*/
                @SuppressWarnings("unchecked")
                final ConnectionInputSubscriber _connectionInputSubscriber = newConnectionInputSubscriber(orig,
                                                                                                          ctx.channel());

                connectionInputSubscriber = _connectionInputSubscriber;

                final SubscriberToChannelFutureBridge l = new SubscriberToChannelFutureBridge() {

                    @Override
                    protected void doOnSuccess(ChannelFuture future) {
                        onChannelClose(_connectionInputSubscriber);
                    }

                    @Override
                    protected void doOnFailure(ChannelFuture future, Throwable cause) {
                        onChannelClose(_connectionInputSubscriber);
                    }
                };

                l.bridge(ctx.channel().closeFuture(), _connectionInputSubscriber);

                @SuppressWarnings({ "unchecked", "rawtypes" })
                ConnectionInputSubscriberEvent newEvent = new ConnectionInputSubscriberEvent(_connectionInputSubscriber
                );
                eventToPropagateFurther = newEvent;
            } else {
                if (null != connectionInputSubscriber) {
                    connectionInputSubscriber.state.stage = Stage.Upgraded;
                }
                @SuppressWarnings({ "unchecked", "rawtypes" })
                ConnectionInputSubscriberReplaceEvent replaceEvt = new ConnectionInputSubscriberReplaceEvent<>(orig);
                eventToPropagateFurther = replaceEvt;
            }
        } else if (evt instanceof HttpContentSubscriberEvent) {
            newHttpContentSubscriber(evt, connectionInputSubscriber);
        }

        super.userEventTriggered(ctx, eventToPropagateFurther);
    }

    protected ConnectionInputSubscriber newConnectionInputSubscriber(ConnectionInputSubscriberEvent<?, ?> orig,
                                                                     Channel channel) {
        ConnectionInputSubscriber toReturn = new ConnectionInputSubscriber(orig.getSubscriber(), channel);
        toReturn.state.headerSub.add(Subscriptions.create(toReturn));
        return toReturn;
    }

    protected final void onChannelClose(ConnectionInputSubscriber connectionInputSubscriber) {
        /*
         * If any of the subscribers(header or content) are still subscribed and the channel is closed, it is an
         * error. If they are unsubscribed, this will be a no-op.
         */
        connectionInputSubscriber.onError(CLOSED_CHANNEL_EXCEPTION);
    }

    protected void onClosedBeforeReceiveComplete(Channel channel) {
        // No Op. Override to add behavior
    }

    protected void resetSubscriptionState(final ConnectionInputSubscriber connectionInputSubscriber) {
        connectionInputSubscriber.resetSubscribers();
    }

    protected abstract boolean isInboundHeader(Object nextItem);

    protected abstract boolean isOutboundHeader(Object nextItem);

    protected abstract Object newHttpObject(Object nextItem, Channel channel);

    protected abstract void onContentReceived();

    protected abstract void onContentReceiveComplete(long receiveStartTimeNanos);

    protected void onNewContentSubscriber(ConnectionInputSubscriber inputSubscriber, Subscriber<? super C> newSub) {
        // No Op.
    }

    protected long getHeaderWriteStartTimeNanos() {
        return headerWriteStartTimeNanos;
    }

    private void processNextItemInEventloop(Object nextItem, ConnectionInputSubscriber connectionInputSubscriber) {
        final State state = connectionInputSubscriber.state;
        final Channel channel = connectionInputSubscriber.channel;

        if (isInboundHeader(nextItem)) {
            state.headerReceived();
            Object newHttpObject = newHttpObject(nextItem, channel);
            connectionInputSubscriber.nextHeader(newHttpObject);
            /*Why not complete the header sub? It may be listening to multiple responses (pipelining)*/
            checkEagerSubscriptionIfConfigured(channel, state);

            final HttpObject httpObject = (HttpObject) nextItem;
            if (httpObject.decoderResult().isFailure()) {
                connectionInputSubscriber.onError(httpObject.decoderResult().cause());
                channel.close();// Netty rejects all data after decode failure, so closing connection
                                // Issue: https://github.com/netty/netty/issues/3362
            }
        }

        if (nextItem instanceof HttpContent) {
            onContentReceived();
            ByteBuf content = ((ByteBufHolder) nextItem).content();
            if (nextItem instanceof LastHttpContent) {
                state.contentComplete();
                /*
                 * Since, LastHttpContent is always received, even if the pipeline does not emit ByteBuf, if
                 * ByteBuf with the LastHttpContent is empty, only trailing headers are emitted. Otherwise,
                 * the content type should be a ByteBuf.
                 */
                if (content.isReadable()) {
                    connectionInputSubscriber.nextContent(content);
                } else {
                    /*Since, the content buffer, was not sent, release it*/
                    ReferenceCountUtil.release(content);
                }

                connectionInputSubscriber.contentComplete();
                onContentReceiveComplete(state.headerReceivedTimeNanos);
            } else {
                connectionInputSubscriber.nextContent(content);
            }
        } else if(!isInboundHeader(nextItem)){
            connectionInputSubscriber.nextContent(nextItem);
        }
    }

    private void newHttpContentSubscriber(final Object evt, final ConnectionInputSubscriber inputSubscriber) {
        @SuppressWarnings("unchecked")
        HttpContentSubscriberEvent<C> contentSubscriberEvent = (HttpContentSubscriberEvent<C>) evt;
        Subscriber<? super C> newSub = contentSubscriberEvent.getSubscriber();
        Throwable errorToRaise = null;

        if (null == inputSubscriber) {
            errorToRaise = new NullPointerException("Null Connection input subscriber.");
        } else {
            final State state = inputSubscriber.state;

            if (state.raiseErrorOnInputSubscription()) {
                errorToRaise = state.raiseErrorOnInputSubscription;
            } else if (isValidToEmit(state.contentSub)) {
                /*Allow only one concurrent input subscriber but allow concatenated subscribers*/
                if (!newSub.isUnsubscribed()) {
                    errorToRaise = ONLY_ONE_CONTENT_INPUT_SUB_ALLOWED;
                }
            } else if (state.stage == Stage.HeaderReceived) {
                inputSubscriber.setupContentSubscriber(newSub);
                onNewContentSubscriber(inputSubscriber, newSub);
            } else {
                errorToRaise = new IllegalStateException("Content subscription received without request start.");
            }
        }

        if (null != errorToRaise && isValidToEmit(newSub)) {
            newSub.onError(errorToRaise);
        }
    }

    private void checkEagerSubscriptionIfConfigured(Channel channel, final State state) {
        if (channel.config().isAutoRead()) {
            if (null == state.contentSub) {
                // If the channel is set to auto-read and there is no eager subscription then, we should raise errors
                // when a subscriber arrives.
                state.raiseErrorOnInputSubscription = LAZY_CONTENT_INPUT_SUB;
                state.contentSub = emptyContentSubscriber;
            }
        }
    }

    private static boolean isValidToEmit(Subscriber<?> subscriber) {
        return null != subscriber && !subscriber.isUnsubscribed();
    }

    /**
     * All state for this handler. At any point we need to invoke any method outside of this handler, this state should
     * be stored in a local variable and used after the external call finishes. Failure to do so will cause race
     * conditions in us using different state before and after the method call specifically if the external call ends
     * up generating a user generated event and triggering {@link #userEventTriggered(ChannelHandlerContext, Object)}
     * which in turn changes this state.
     *
     * Issue: https://github.com/Netflix/RxNetty/issues/129
     */
    protected static final class State {

        /*Visible for testing*/enum Stage {
            /*Strictly in the order in which the transitions would happen*/
            Created,
            HeaderReceived,
            ContentComplete,
            Upgraded
        }

        protected IllegalStateException raiseErrorOnInputSubscription;
        @SuppressWarnings("rawtypes") private Subscriber headerSub;
        @SuppressWarnings("rawtypes") private Subscriber contentSub;
        private long headerReceivedTimeNanos;

        private volatile Stage stage = Stage.Created;

        /*Visible for testing*/void headerReceived() {
            headerReceivedTimeNanos = Clock.newStartTimeNanos();
            stage = Stage.HeaderReceived;
        }

        private void contentComplete() {
            stage = Stage.ContentComplete;
        }

        public boolean raiseErrorOnInputSubscription() {
            return null != raiseErrorOnInputSubscription;
        }

        public boolean startButNotCompleted() {
            return stage == Stage.HeaderReceived;
        }

        public boolean receiveStarted() {
            return stage.ordinal() > Stage.Created.ordinal();
        }

        /*Visible for testing*/Subscriber<?> getHeaderSub() {
            return headerSub;
        }

        /*Visible for testing*/Subscriber<?> getContentSub() {
            return contentSub;
        }
    }

    protected class ConnectionInputSubscriber extends Subscriber<Object> implements Action0, Runnable {

        private final Channel channel;
        private final State state;
        private Producer producer;

        @SuppressWarnings("rawtypes")
        private ConnectionInputSubscriber(Subscriber subscriber, Channel channel) {
            state = new State();
            this.channel = channel;
            state.headerSub = subscriber;
        }

        @Override
        public void onCompleted() {
            // This means channel input has completed
            if (state.startButNotCompleted()) {
                onError(CLOSED_CHANNEL_EXCEPTION);
            } else {
                completeAllSubs();
            }
        }

        @Override
        public void onError(Throwable e) {
            // This means channel input has got an error & hence no other notifications will arrive.
            errorAllSubs(e);

            if (state.startButNotCompleted()) {
                onClosedBeforeReceiveComplete(channel);
            }
        }

        @Override
        public void onNext(final Object next) {
            if (channel.eventLoop().inEventLoop()) {
                processNextItemInEventloop(next, this);
            } else {
                channel.eventLoop().execute(new Runnable() {
                    @Override
                    public void run() {
                        processNextItemInEventloop(next, ConnectionInputSubscriber.this);
                    }
                });
            }
        }

        @Override
        public void setProducer(Producer producer) {
            this.producer = producer;
            state.headerSub.setProducer(producer); /*Content & trailer producers are set on subscription*/
        }

        public Channel getChannel() {
            return channel;
        }

        public void resetSubscribers() {
            completeAllSubs();
        }

        private void completeAllSubs() {
            if (isValidToEmit(state.headerSub)) {
                state.headerSub.onCompleted();
            }
            if (isValidToEmit(state.contentSub)) {
                state.contentSub.onCompleted();
            }
        }

        private void errorAllSubs(Throwable throwable) {
            if (isValidToEmit(state.headerSub)) {
                state.headerSub.onError(throwable);
            }
            if (isValidToEmit(state.contentSub)) {
                state.contentSub.onError(throwable);
            }
        }

        @SuppressWarnings("unchecked")
        private void nextContent(final Object nextObject) {
            if (isValidToEmit(state.contentSub)) {
                state.contentSub.onNext(nextObject);
            } else {
                contentArrivedWhenSubscriberNotValid();
                if (logger.isWarnEnabled()) {
                    logger.warn("Data received on channel, but no subscriber registered. Discarding data. Message class: "
                                + nextObject.getClass().getName() + ", channel: " + channel);
                }
                ReferenceCountUtil.release(nextObject);
            }
        }

        @SuppressWarnings("unchecked")
        private void nextHeader(final Object nextObject) {
            if (isValidToEmit(state.headerSub)) {
                state.headerSub.onNext(nextObject);
            }
        }

        private void setupContentSubscriber(Subscriber<? super C> newSub) {

            assert channel.eventLoop().inEventLoop();

            state.contentSub = newSub;
            state.contentSub.add(Subscriptions.create(this));
            state.contentSub.setProducer(producer); /*Content demand matches upstream demand*/
        }

        public void contentComplete() {
            assert channel.eventLoop().inEventLoop();

            if (isValidToEmit(state.contentSub)) {
                state.contentSub.onCompleted();
            } else {
                contentArrivedWhenSubscriberNotValid();
            }
        }

        private void contentArrivedWhenSubscriberNotValid() {
            if (null == state.contentSub) {
                /*
                 * Cases when auto-read is off and there is lazy subscription, due to mismatched request demands on the
                 * subscriber, it may so happen that we get content without a subscriber, in such cases, we should raise
                 * an error.
                 */
                state.raiseErrorOnInputSubscription = CONTENT_ARRIVED_WITH_NO_SUB;
            }
        }

        /*Visible for testing*/State getState() {
            return state;
        }

        @Override
        public void run() {
            if (state.contentSub != null) {
                if (state.contentSub.isUnsubscribed()) {
                    // Content sub exists and unsubscribed, so unsubscribe from input.
                    unsubscribe();
                } else if (state.headerSub.isUnsubscribed() && !state.receiveStarted()) {
                    // Header sub unsubscribed before request started, unsubscribe from input.
                    unsubscribe();
                }
            } else if (state.headerSub.isUnsubscribed() && !state.receiveStarted()) {
                // Header sub unsubscribed before request started, unsubscribe from input.
                unsubscribe();
            }
        }

        @Override
        public void call() {
            if (channel.eventLoop().inEventLoop()) {
                run();
            } else {
                channel.eventLoop().execute(ConnectionInputSubscriber.this);
            }
        }

    }
}
