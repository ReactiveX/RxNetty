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
package io.reactivex.netty.channel;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.EmptyArrays;
import io.reactivex.netty.channel.events.ConnectionEventListener;
import io.reactivex.netty.events.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Producer;
import rx.Subscriber;
import rx.exceptions.MissingBackpressureException;

import java.nio.channels.ClosedChannelException;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

/**
 * A bridge between a {@link Connection} instance and the associated {@link Channel}.
 *
 * All operations on {@link Connection} will pass through this bridge to an appropriate action on the {@link Channel}
 *
 * <h2>Lazy {@link Connection#getInput()} subscription</h2>
 *
 * Lazy subscriptions are allowed on {@link Connection#getInput()} if and only if the channel is configured to
 * not read data automatically (i.e. {@link ChannelOption#AUTO_READ} is set to {@code false}). Otherwise,
 * if {@link Connection#getInput()} is subscribed lazily, the subscriber always receives an error. The content
 * in this case is disposed upon reading.
 *
 * @param <R> Type read from the connection held by this handler.
 * @param <W> Type written to the connection held by this handler.
 */
public abstract class AbstractConnectionToChannelBridge<R, W> extends BackpressureManagingHandler {

    private static final Logger logger = LoggerFactory.getLogger(AbstractConnectionToChannelBridge.class);

    @SuppressWarnings("ThrowableInstanceNeverThrown")
    private static final IllegalStateException ONLY_ONE_CONN_SUB_ALLOWED =
            new IllegalStateException("Only one subscriber allowed for connection observable.");
    @SuppressWarnings("ThrowableInstanceNeverThrown")
    private static final IllegalStateException ONLY_ONE_CONN_INPUT_SUB_ALLOWED =
            new IllegalStateException("Only one subscriber allowed for connection input.");
    @SuppressWarnings("ThrowableInstanceNeverThrown")
    private static final IllegalStateException LAZY_CONN_INPUT_SUB =
            new IllegalStateException("Channel is set to auto-read but the subscription was lazy.");

    @SuppressWarnings("ThrowableInstanceNeverThrown")
    private static final ClosedChannelException CLOSED_CHANNEL_EXCEPTION = new ClosedChannelException();

    static {
        ONLY_ONE_CONN_INPUT_SUB_ALLOWED.setStackTrace(EmptyArrays.EMPTY_STACK_TRACE);
        ONLY_ONE_CONN_SUB_ALLOWED.setStackTrace(EmptyArrays.EMPTY_STACK_TRACE);
        LAZY_CONN_INPUT_SUB.setStackTrace(EmptyArrays.EMPTY_STACK_TRACE);
        CLOSED_CHANNEL_EXCEPTION.setStackTrace(EmptyArrays.EMPTY_STACK_TRACE);
    }

    private final AttributeKey<ConnectionEventListener> eventListenerAttributeKey;
    private final AttributeKey<EventPublisher> eventPublisherAttributeKey;

    protected ConnectionEventListener eventListener;
    protected EventPublisher eventPublisher;
    private Subscriber<? super Channel> newChannelSub;
    private ReadProducer<R> readProducer;
    private boolean raiseErrorOnInputSubscription;
    private boolean connectionEmitted;

    protected AbstractConnectionToChannelBridge(String thisHandlerName, ConnectionEventListener eventListener,
                                                EventPublisher eventPublisher) {
        super(thisHandlerName);
        if (null == eventListener) {
            throw new IllegalArgumentException("Event listener can not be null.");
        }
        if (null == eventPublisher) {
            throw new IllegalArgumentException("Event publisher can not be null.");
        }
        this.eventListener = eventListener;
        this.eventPublisher = eventPublisher;
        eventListenerAttributeKey = null;
        eventPublisherAttributeKey = null;
    }

    protected AbstractConnectionToChannelBridge(String thisHandlerName,
                                                AttributeKey<ConnectionEventListener> eventListenerAttributeKey,
                                                AttributeKey<EventPublisher> eventPublisherAttributeKey) {
        super(thisHandlerName);
        this.eventListenerAttributeKey = eventListenerAttributeKey;
        this.eventPublisherAttributeKey = eventPublisherAttributeKey;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        if (null == eventListener && null == eventPublisher) {
            eventListener = ctx.channel().attr(eventListenerAttributeKey).get();
            eventPublisher = ctx.channel().attr(eventPublisherAttributeKey).get();
        }

        if (null == eventPublisher) {
            logger.error("No Event publisher bound to the channel, closing channel.");
            ctx.channel().close();
            return;
        }

        if (eventPublisher.publishingEnabled() && null == eventListener) {
            logger.error("No Event listener bound to the channel and publising is enabled, closing channel.");
            ctx.channel().close();
            return;
        }

        ctx.pipeline().addFirst(new BytesInspector(eventPublisher, eventListener));

        super.handlerAdded(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (!connectionEmitted && isValidToEmit(newChannelSub)) {
            emitNewConnection(ctx.channel());
            connectionEmitted = true;
        }
        super.channelInactive(ctx);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {

        if (isValidToEmitToReadSubscriber(readProducer)) {
            /*If the subscriber is still active, then it expects data but the channel is closed.*/
            readProducer.sendOnError(CLOSED_CHANNEL_EXCEPTION);
        }

        super.channelUnregistered(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof EmitConnectionEvent) {
            if (!connectionEmitted) {
                emitNewConnection(ctx.channel());
                connectionEmitted = true;
            }
        } else if (evt instanceof ConnectionCreationFailedEvent) {
            if (isValidToEmit(newChannelSub)) {
                newChannelSub.onError(((ConnectionCreationFailedEvent)evt).getThrowable());
            }
        } else if (evt instanceof ChannelSubscriberEvent) {
            @SuppressWarnings("unchecked")
            final ChannelSubscriberEvent<R, W> channelSubscriberEvent = (ChannelSubscriberEvent<R, W>) evt;

            newConnectionSubscriber(channelSubscriberEvent);
        } else if (evt instanceof ConnectionInputSubscriberEvent) {
            @SuppressWarnings("unchecked")
            ConnectionInputSubscriberEvent<R, W> event = (ConnectionInputSubscriberEvent<R, W>) evt;

            newConnectionInputSubscriber(ctx.channel(), event.getSubscriber(), false);
        } else if (evt instanceof ConnectionInputSubscriberResetEvent) {
            resetConnectionInputSubscriber();
        } else if (evt instanceof ConnectionInputSubscriberReplaceEvent) {
            @SuppressWarnings("unchecked")
            ConnectionInputSubscriberReplaceEvent<R, W> event = (ConnectionInputSubscriberReplaceEvent<R, W>) evt;
            replaceConnectionInputSubscriber(ctx.channel(), event);
        }

        super.userEventTriggered(ctx, evt);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void newMessage(ChannelHandlerContext ctx, Object msg) {
        if (isValidToEmitToReadSubscriber(readProducer)) {
            try {
                readProducer.sendOnNext((R) msg);
            } catch (ClassCastException e) {
                ReferenceCountUtil.release(msg); // Since, this was not sent to the subscriber, release the msg.
                readProducer.sendOnError(e);
            }
        } else {
            if (logger.isWarnEnabled()) {
                logger.warn("Data received on channel, but no subscriber registered. Discarding data. Message class: "
                            + msg.getClass().getName() + ", channel: " + ctx.channel());
            }
            ReferenceCountUtil.release(msg); // No consumer of the message, so discard.
        }
    }

    @Override
    public boolean shouldReadMore(ChannelHandlerContext ctx) {
        return null != readProducer && readProducer.shouldReadMore(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (!connectionEmitted && isValidToEmit(newChannelSub)) {
            newChannelSub.onError(cause);
        } else if (isValidToEmitToReadSubscriber(readProducer)) {
            readProducer.sendOnError(cause);
        } else {
            logger.info("Exception in the pipeline and none of the subscribers are active.", cause);
        }
    }

    protected static boolean isValidToEmit(Subscriber<?> subscriber) {
        return null != subscriber && !subscriber.isUnsubscribed();
    }

    private static boolean isValidToEmitToReadSubscriber(ReadProducer<?> readProducer) {
        return null != readProducer && !readProducer.subscriber.isUnsubscribed();
    }

    protected boolean connectionInputSubscriberExists(Channel channel) {
        assert channel.eventLoop().inEventLoop();

        return null != readProducer && null != readProducer.subscriber && !readProducer.subscriber.isUnsubscribed();
    }

    protected void onNewReadSubscriber(Subscriber<? super R> subscriber) {
        // NOOP
    }

    protected final void checkEagerSubscriptionIfConfigured(Channel channel) {
        if (channel.config().isAutoRead() && null == readProducer) {
            // If the channel is set to auto-read and there is no eager subscription then, we should raise errors
            // when a subscriber arrives.
            raiseErrorOnInputSubscription = true;
            final Subscriber<? super R> discardAll = ConnectionInputSubscriberEvent.discardAllInput()
                                                                                   .getSubscriber();
            final ReadProducer<R> producer = new ReadProducer<>(discardAll, channel);
            discardAll.setProducer(producer);
            readProducer = producer;
        }
    }

    protected final Subscriber<? super Channel> getNewChannelSub() {
        return newChannelSub;
    }

    private void emitNewConnection(Channel channel) {
        if (isValidToEmit(newChannelSub)) {
            try {
                newChannelSub.onNext(channel);
                connectionEmitted = true;
                checkEagerSubscriptionIfConfigured(channel);
                newChannelSub.onCompleted();
            } catch (Exception e) {
                logger.error("Error emitting a new connection. Closing this channel.", e);
                channel.close();
            }
        } else {
            channel.close(); // Closing the connection if not sent to a subscriber.
        }
    }

    private void resetConnectionInputSubscriber() {
        final Subscriber<? super R> connInputSub = null == readProducer? null : readProducer.subscriber;
        if (isValidToEmit(connInputSub)) {
            connInputSub.onCompleted();
        }
        raiseErrorOnInputSubscription = false;
        readProducer = null; // A subsequent event should set it to the desired subscriber.
    }

    private void newConnectionInputSubscriber(final Channel channel, final Subscriber<? super R> subscriber,
                                              boolean replace) {
        final Subscriber<? super R> connInputSub = null == readProducer ? null : readProducer.subscriber;
        if (isValidToEmit(connInputSub)) {
            if (!replace) {
                /*Allow only once concurrent input subscriber but allow concatenated subscribers*/
                subscriber.onError(ONLY_ONE_CONN_INPUT_SUB_ALLOWED);
            } else {
                setNewReadProducer(channel, subscriber);
                connInputSub.onCompleted();
            }
        } else if (raiseErrorOnInputSubscription) {
            subscriber.onError(LAZY_CONN_INPUT_SUB);
        } else {
            setNewReadProducer(channel, subscriber);
        }
    }

    private void setNewReadProducer(Channel channel, Subscriber<? super R> subscriber) {
        final ReadProducer<R> producer = new ReadProducer<>(subscriber, channel);
        subscriber.setProducer(producer);
        onNewReadSubscriber(subscriber);
        readProducer = producer;
    }

    private void replaceConnectionInputSubscriber(Channel channel, ConnectionInputSubscriberReplaceEvent<R, W> event) {
        ConnectionInputSubscriberEvent<R, W> newSubEvent = event.getNewSubEvent();
        newConnectionInputSubscriber(channel, newSubEvent.getSubscriber(),
                                     true);
    }

    private void newConnectionSubscriber(ChannelSubscriberEvent<R, W> event) {
        if (null == newChannelSub) {
            newChannelSub = event.getSubscriber();
        } else {
            event.getSubscriber().onError(ONLY_ONE_CONN_SUB_ALLOWED);
        }
    }

    /*Visible for testing*/ static final class ReadProducer<T> extends RequestReadIfRequiredEvent implements Producer {

        @SuppressWarnings("rawtypes")
        private static final AtomicLongFieldUpdater<ReadProducer> REQUEST_UPDATER =
                AtomicLongFieldUpdater.newUpdater(ReadProducer.class, "requested");/*Updater for requested*/
        private volatile long requested; // Updated by REQUEST_UPDATER, required to be volatile.

        private final Subscriber<? super T> subscriber;
        private final Channel channel;

        /*Visible for testing*/ ReadProducer(Subscriber<? super T> subscriber, Channel channel) {
            this.subscriber = subscriber;
            this.channel = channel;
        }

        @Override
        public void request(long n) {
            if (Long.MAX_VALUE != requested) {
                if (Long.MAX_VALUE == n) {
                    // Now turning off backpressure
                    REQUEST_UPDATER.set(this, Long.MAX_VALUE);
                } else {
                    // add n to field but check for overflow
                    while (true) {
                        final long current = requested;
                        long next = current + n;
                        // check for overflow
                        if (next < 0) {
                            next = Long.MAX_VALUE;
                        }
                        if (REQUEST_UPDATER.compareAndSet(this, current, next)) {
                            break;
                        }
                    }
                }
            }

            if (!channel.config().isAutoRead()) {
                channel.pipeline().fireUserEventTriggered(this);
            }
        }

        public void sendOnError(Throwable throwable) {
            subscriber.onError(throwable);
        }

        public void sendOnComplete() {
            subscriber.onCompleted();
        }

        public void sendOnNext(T nextItem) {
            if (requested > 0) {
                if (REQUEST_UPDATER.get(this) != Long.MAX_VALUE) {
                    REQUEST_UPDATER.decrementAndGet(this);
                }
                subscriber.onNext(nextItem);
            } else {
                subscriber.onError(new MissingBackpressureException(
                        "Received more data on the channel than demanded by the subscriber."));
            }
        }

        @Override
        protected boolean shouldReadMore(ChannelHandlerContext ctx) {
            return !subscriber.isUnsubscribed() && REQUEST_UPDATER.get(this) > 0;
        }

        /*Visible for testing*/long getRequested() {
            return requested;
        }

        @Override
        public String toString() {
            return "ReadProducer{" + "requested=" + requested + '}';
        }
    }
}
