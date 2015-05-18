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
package io.reactivex.netty.protocol.http.internal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.codec.http.HttpHeaders.Values;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.EmptyArrays;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.metrics.Clock;
import io.reactivex.netty.protocol.http.TrailingHeaders;
import io.reactivex.netty.protocol.tcp.ConnectionInputSubscriberEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Producer;
import rx.Subscriber;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

import java.nio.channels.ClosedChannelException;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

public abstract class AbstractHttpConnectionBridge<C> extends ChannelDuplexHandler {

    private static final Logger logger = LoggerFactory.getLogger(AbstractHttpConnectionBridge.class);

    private static final IllegalStateException ONLY_ONE_CONTENT_INPUT_SUB_ALLOWED =
            new IllegalStateException("Only one subscriber allowed for HTTP content.");
    private static final IllegalStateException LAZY_CONTENT_INPUT_SUB =
            new IllegalStateException("Channel is set to auto-read but the subscription was lazy.");
    private static final IllegalStateException CONTENT_ARRIVED_WITH_NO_SUB =
            new IllegalStateException("HTTP Content received but no subscriber was registered.");

    private static final IllegalStateException ONLY_ONE_TRAILER_INPUT_SUB_ALLOWED =
            new IllegalStateException("Only one subscriber allowed for HTTP trailing headers.");
    private static final IllegalStateException LAZY_TRAILER_SUB =
            new IllegalStateException("Channel is set to auto-read but the subscription was lazy.");
    private static final IllegalStateException TRAILER_ARRIVED_WITH_NO_SUB =
            new IllegalStateException("HTTP trailing headers received but no subscriber was registered.");

    private static final ClosedChannelException CLOSED_CHANNEL_EXCEPTION = new ClosedChannelException();

    static {
        ONLY_ONE_CONTENT_INPUT_SUB_ALLOWED.setStackTrace(EmptyArrays.EMPTY_STACK_TRACE);
        LAZY_CONTENT_INPUT_SUB.setStackTrace(EmptyArrays.EMPTY_STACK_TRACE);
        CONTENT_ARRIVED_WITH_NO_SUB.setStackTrace(EmptyArrays.EMPTY_STACK_TRACE);
        ONLY_ONE_TRAILER_INPUT_SUB_ALLOWED.setStackTrace(EmptyArrays.EMPTY_STACK_TRACE);
        LAZY_TRAILER_SUB.setStackTrace(EmptyArrays.EMPTY_STACK_TRACE);
        TRAILER_ARRIVED_WITH_NO_SUB.setStackTrace(EmptyArrays.EMPTY_STACK_TRACE);
        CLOSED_CHANNEL_EXCEPTION.setStackTrace(EmptyArrays.EMPTY_STACK_TRACE);
    }

    protected ConnectionInputSubscriber connectionInputSubscriber;
    private final UnsafeEmptySubscriber<C> emptyContentSubscriber;
    private final UnsafeEmptySubscriber<TrailingHeaders> emptyTrailerSubscriber;
    private long headerWriteStartTimeMillis;

    protected AbstractHttpConnectionBridge() {
        emptyContentSubscriber = new UnsafeEmptySubscriber<>("Error while waiting for HTTP content.");
        emptyTrailerSubscriber = new UnsafeEmptySubscriber<>("Error while waiting for HTTP trailing headers.");
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        Object msgToWrite = msg;

        if (isOutboundHeader(msg)) {
            headerWriteStartTimeMillis = Clock.newStartTimeMillis();
            HttpMessage httpMsg = (HttpMessage) msg;
            if (!HttpHeaders.isContentLengthSet(httpMsg)) {
                // If there is no content length we need to specify the transfer encoding as chunked as we always
                // send data in multiple HttpContent.
                // On the other hand, if someone wants to not have chunked encoding, adding content-length will work
                // as expected.
                httpMsg.headers().set(Names.TRANSFER_ENCODING, Values.CHUNKED);
            }

            onOutboundHeaderWrite(httpMsg, promise, headerWriteStartTimeMillis);

        } else if (msg instanceof String) {
            msgToWrite = ctx.alloc().buffer().writeBytes(((String) msg).getBytes());
        } else if (msg instanceof byte[]) {
            msgToWrite = ctx.alloc().buffer().writeBytes((byte[]) msg);
        } else if (msg instanceof LastHttpContent) {
            onOutboundLastContentWrite((LastHttpContent) msg, promise, headerWriteStartTimeMillis);
        }

        super.write(ctx, msgToWrite, promise);
    }

    protected abstract void onOutboundHeaderWrite(HttpMessage httpMsg, ChannelPromise promise, long startTimeMillis);

    protected abstract void onOutboundLastContentWrite(LastHttpContent msg, ChannelPromise promise,
                                                       long headerWriteStartTime);

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

        Object eventToPropagateFurther = evt;

        if (evt instanceof ConnectionInputSubscriberEvent) {
            @SuppressWarnings({"unchecked", "rawtypes"})
            ConnectionInputSubscriberEvent orig = (ConnectionInputSubscriberEvent) evt;
            /*Local copy to refer from the channel close listener. As the instance level copy can change*/
            final ConnectionInputSubscriber _connectionInputSubscriber =
                    new ConnectionInputSubscriber(orig.getSubscriber(), orig.getConnection());

            connectionInputSubscriber = _connectionInputSubscriber;
            ctx.channel().closeFuture().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    onChannelClose(_connectionInputSubscriber);
                }
            });

            @SuppressWarnings({"unchecked", "rawtypes"})
            ConnectionInputSubscriberEvent newEvent = new ConnectionInputSubscriberEvent(_connectionInputSubscriber,
                                                                                         orig.getConnection());
            eventToPropagateFurther = newEvent;
        } else if (evt instanceof HttpContentSubscriberEvent) {
            newHttpContentSubscriber(evt, connectionInputSubscriber);
        } else if (evt instanceof HttpTrailerSubscriberEvent) {
            newHttpTrailerEvent(evt, connectionInputSubscriber);
        }

        super.userEventTriggered(ctx, eventToPropagateFurther);
    }

    protected final void onChannelClose(ConnectionInputSubscriber connectionInputSubscriber) {
        /*
         * If any of the subscribers(header, content, trailer) are still subscribed and the channel is closed, it is an
         * error. If they are unsubscribed, this will be a no-op.
         */
        connectionInputSubscriber.onError(CLOSED_CHANNEL_EXCEPTION);
    }

    protected void onClosedBeforeReceiveComplete(ConnectionInputSubscriber connectionInputSubscriber) {
        // No Op. Override to add behavior
    }

    protected void resetSubscriptionState(final ConnectionInputSubscriber connectionInputSubscriber) {
        connectionInputSubscriber.resetSubscribers();
    }

    protected abstract boolean isInboundHeader(Object nextItem);

    protected abstract boolean isOutboundHeader(Object nextItem);

    protected abstract Object newHttpObject(Object nextItem, Channel channel);

    protected abstract void onContentReceived();

    protected abstract void onContentReceiveComplete(long receiveStartTimeMillis);

    protected void onNewContentSubscriber(ConnectionInputSubscriber inputSubscriber, Subscriber<? super C> newSub) {
        // No Op.
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

                LastHttpContent lastHttpContent = (LastHttpContent) nextItem;
                if (null != state.trailerSub && !lastHttpContent.trailingHeaders().isEmpty()) {
                    final TrailingHeaders trailer = new TrailingHeaders(lastHttpContent);
                    state.trailerSub.onNext(trailer);
                }

                connectionInputSubscriber.contentComplete();
                onContentReceiveComplete(state.headerReceivedTimeMillis);
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

        if (null == inputSubscriber) {
            newSub.onError(new IllegalStateException("Received an HTTP Content subscriber without HTTP message."));
        } else {
            final State state = inputSubscriber.state;

            if (state.raiseErrorOnInputSubscription()) {
                newSub.onError(state.raiseErrorOnInputSubscription);
            } else if (isValidToEmit(state.contentSub)) {
                /*Allow only once concurrent input subscriber but allow concatenated subscribers*/
                if (!newSub.isUnsubscribed()) {
                    newSub.onError(ONLY_ONE_CONTENT_INPUT_SUB_ALLOWED);
                }
            } else {
                inputSubscriber.setupContentSubscriber(newSub);
                onNewContentSubscriber(inputSubscriber, newSub);
            }
        }
    }

    private void newHttpTrailerEvent(final Object evt, final ConnectionInputSubscriber inputSubscriber) {
        HttpTrailerSubscriberEvent contentSubscriberEvent = (HttpTrailerSubscriberEvent) evt;
        Subscriber<? super TrailingHeaders> newSub = contentSubscriberEvent.getSubscriber();

        if (null == inputSubscriber) {
            newSub.onError(new IllegalStateException("Received an HTTP trailer subscriber without HTTP message."));
        } else {
            final State state = inputSubscriber.state;
            if (state.raiseErrorOnTrailerSubscription()) {
                newSub.onError(state.raiseErrorOnTrailerSubscription);
            } else if (null == state.trailerSub) {
                connectionInputSubscriber.setupTrailerSubscriber(newSub);
            } else {
                if (!newSub.isUnsubscribed()) {
                    newSub.onError(ONLY_ONE_TRAILER_INPUT_SUB_ALLOWED);
                }
            }
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
            if (null == state.trailerSub) {
                // If the channel is set to auto-read and there is no eager subscription then, we should raise errors
                // when a subscriber arrives.
                state.raiseErrorOnTrailerSubscription = LAZY_TRAILER_SUB;
                state.trailerSub = emptyTrailerSubscriber;
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
            ContentComplete
        }

        protected IllegalStateException raiseErrorOnInputSubscription;
        protected IllegalStateException raiseErrorOnTrailerSubscription;
        @SuppressWarnings("rawtypes") private Subscriber headerSub;
        @SuppressWarnings("rawtypes") private Subscriber contentSub;
        protected Subscriber<? super TrailingHeaders> trailerSub;
        private long headerReceivedTimeMillis;

        private volatile Stage stage = Stage.Created;

        private void headerReceived() {
            headerReceivedTimeMillis = Clock.newStartTimeMillis();
            stage = Stage.HeaderReceived;
        }

        private void contentComplete() {
            stage = Stage.ContentComplete;
        }

        public boolean raiseErrorOnInputSubscription() {
            return null != raiseErrorOnInputSubscription;
        }

        public boolean raiseErrorOnTrailerSubscription() {
            return null != raiseErrorOnTrailerSubscription;
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

        /*Visible for testing*/Subscriber<? super TrailingHeaders> getTrailerSub() {
            return trailerSub;
        }

        /*Visible for testing*/long getHeaderReceivedTimeMillis() {
            return headerReceivedTimeMillis;
        }

        /*Visible for testing*/Stage getStage() {
            return stage;
        }

        /*Visible for testing*/IllegalStateException getRaiseErrorOnInputSubscription() {
            return raiseErrorOnInputSubscription;
        }

        /*Visible for testing*/IllegalStateException getRaiseErrorOnTrailerSubscription() {
            return raiseErrorOnTrailerSubscription;
        }
    }

    /**
     * A subscriber that can be reused if and only if not wrapped in a {@link rx.observers.SafeSubscriber}.
     */
    protected static final class UnsafeEmptySubscriber<T> extends Subscriber<T> {

        private final String msg;

        protected UnsafeEmptySubscriber(String msg) {
            this.msg = msg;
        }

        @Override
        public void onCompleted() {
        }

        @Override
        public void onError(Throwable e) {
            logger.error(msg, e);
        }

        @Override
        public void onNext(T o) {
            ReferenceCountUtil.release(o);
        }
    }

    /*Visible for testing*/static class TrailerProducer implements Producer {

        private final Producer delegate;
        @SuppressWarnings("unused")
        private volatile int requestedUp; /*Updated and used via the updater*/
        /*Updater for requested*/
        private static final AtomicIntegerFieldUpdater<TrailerProducer>
                REQUESTED_UP_UPDATER =
                AtomicIntegerFieldUpdater.newUpdater(TrailerProducer.class, "requestedUp");

        private TrailerProducer(Producer delegate) {
            this.delegate = delegate;
        }

        @Override
        public void request(long n) {
            if (!REQUESTED_UP_UPDATER.compareAndSet(this, 0, 1)) {
                /*
                 * Since, the trailer will always be 1 for a http message, this just makes sure the trailer
                 * subscriber never requests more than 1
                 */
                delegate.request(1);
            }
        }

        /*Visible for testing*/ Producer getDelegateProducer() {
            return delegate;
        }
    }

    protected class ConnectionInputSubscriber extends Subscriber<Object> {

        private final Channel channel;
        private final State state;
        private Producer producer;

        @SuppressWarnings("rawtypes")
        public ConnectionInputSubscriber(Subscriber subscriber, Connection connection) {
            state = new State();
            state.headerSub = subscriber;
            state.headerSub.add(Subscriptions.create(new Action0() {
                @Override
                public void call() {
                    if (!state.receiveStarted()) {
                        unsubscribe(); // If the receive has not yet started, unsubscribe from input, which closes connection
                    }
                }
            }));
            channel = connection.unsafeNettyChannel();
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
                onClosedBeforeReceiveComplete(this);
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
            if (isValidToEmit(state.trailerSub)) {
                state.trailerSub.onCompleted();
            }
        }

        private void errorAllSubs(Throwable throwable) {
            if (isValidToEmit(state.headerSub)) {
                state.headerSub.onError(throwable);
            }
            if (isValidToEmit(state.contentSub)) {
                state.contentSub.onError(throwable);
            }
            if (isValidToEmit(state.trailerSub)) {
                state.trailerSub.onError(throwable);
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
                                + nextObject.getClass().getName());
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
            state.contentSub.setProducer(producer); /*Content demand matches upstream demand*/
        }

        private void setupTrailerSubscriber(Subscriber<? super TrailingHeaders> newSub) {
            assert channel.eventLoop().inEventLoop();

            state.trailerSub = newSub;
            state.trailerSub.setProducer(new TrailerProducer(producer));
        }

        public void contentComplete() {
            assert channel.eventLoop().inEventLoop();

            if (isValidToEmit(state.contentSub)) {
                state.contentSub.onCompleted();
            } else {
                contentArrivedWhenSubscriberNotValid();
            }

            if (null == state.trailerSub) {
                /*
                 * Cases when auto-read is off and there is lazy subscription, due to mismatched request demands on the
                 * subscriber, it may so happen that we get content without a subscriber, in such cases, we should raise
                 * an error.
                 */
                state.raiseErrorOnTrailerSubscription = TRAILER_ARRIVED_WITH_NO_SUB;
            } else {
                state.trailerSub.onCompleted();
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
    }
}
