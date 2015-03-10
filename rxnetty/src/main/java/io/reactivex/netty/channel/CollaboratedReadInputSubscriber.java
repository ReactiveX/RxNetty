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
package io.reactivex.netty.channel;

import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.util.ReferenceCountUtil;
import io.reactivex.netty.channel.AbstractConnectionToChannelBridge.DrainInputSubscriberBuffer;
import rx.Producer;
import rx.Subscriber;
import rx.annotations.Beta;
import rx.exceptions.MissingBackpressureException;
import rx.functions.Action0;
import rx.internal.operators.NotificationLite;
import rx.internal.util.RxRingBuffer;
import rx.subscriptions.Subscriptions;

import java.util.concurrent.atomic.AtomicLongFieldUpdater;

/**
 * A subscriber for {@link Connection} input.
 *
 * <h2> Thread safety </h2>
 *
 * This class assumes sequential invocation of:
 * <ul>
 <li>{@link #onCompleted()}</li>
 <li>{@link #onNext(Object)}</li>
 <li>{@link #onError(Throwable)}</li>
 <li>{@link #drain()}</li>
 </ul>
 *
 * The following methods can be called concurrently with any other method:
 * <ul>
 <li>{@link #unsubscribe()}</li>
 <li>{@link #request(long)}</li>
 </ul>
 *
 * <h2>Backpressure</h2>
 *
 * This subscriber is backpressure enabled and uses the following strategy:
 * <ul>
 <li>If the requested items from downstream are more than the items emitted, then invoke the original
 {@link Subscriber}</li>
 <li>If the requested items from downstream are less than the items emitted, then use an {@link RxRingBuffer} to store
 the items which can not be sent to the original {@link Subscriber}.</li>
 <li>Whenever an item is buffered, turn off {@link ChannelConfig#isAutoRead()}.</li>
 <li>For every call to {@link Producer#request(long)} trigger a buffer drain, if buffered.</li>
 <li>For every call to {@link Producer#request(long)} if the downstream subscriber requires more data and channel does
 not have {@link ChannelConfig#isAutoRead()} set to {@code true}, then trigger a {@link Channel#read()}</li>
 <li>The data producer of this subscriber (one calling {@link #onNext(Object)}) must make sure to call
 {@link Channel#read()} on receiving {@link ChannelInboundHandler#channelReadComplete(ChannelHandlerContext)} event and
 if {@link #shouldReadMore()} returns {@code true} </li>
 <li>If the {@link RxRingBuffer} fills up at any point in time, send a {@link MissingBackpressureException} to the
 original {@link Subscriber}</li>
 </ul>
 *
 * @author Nitesh Kant
 */
@Beta
public abstract class CollaboratedReadInputSubscriber<I> extends Subscriber<I> {

    @SuppressWarnings("rawtypes")
    /*Updater for requested*/
    private static final AtomicLongFieldUpdater<CollaboratedReadInputSubscriber> REQUEST_UPDATER =
            AtomicLongFieldUpdater.newUpdater(CollaboratedReadInputSubscriber.class, "requested");
    private final Channel channel;
    private volatile long requested; // Updated by REQUEST_UPDATER, required to be volatile.

    /**
     * This will always be accessed by a single thread.
     */
    private RxRingBuffer ringBuffer;
    /**
     * This is to protect agains duplicate terminal notifications, this will not necessarily mean original subsciber is
     * unsubscribed.
     */
    private boolean terminated;
    private final NotificationLite<I> nl = NotificationLite.instance();

    private volatile boolean draining; // Single threaded access

    private final Subscriber<? super I> original;
    private final Runnable fireDrainOnRequestMoreTask;


    protected CollaboratedReadInputSubscriber(final Channel channel, Subscriber<? super I> op) {
        super(op);
        this.channel = channel;

        fireDrainOnRequestMoreTask = new Runnable() {
            @Override
            public void run() {
                /*
                 * The below code has to be run on the eventloop as there is a race between if(registered) and fire
                 * event. On unregistration, the pipeline is cleared and hence the event will never propagate to the
                 * handler.
                 */
                if (channel.isRegistered()) {
                    channel.pipeline().fireUserEventTriggered(DrainInputSubscriberBuffer.INSTANCE);
                } else {
                    drain();
                }

                /*
                 * Since, this operation has to be after the drain (to check if drain caused was sufficient for the
                 * subscriber), it is done in this task.
                 */
                if (!channel.config().isAutoRead() && shouldReadMore()) {
                    /*If the draining exhausted requested, then don't trigger read.*/
                    channel.read();
                }
            }
        };

        original = op;
        original.add(Subscriptions.create(new Action0() {
            @Override
            public void call() {
                /* When unsubscribed, drain the buffer to discard buffered items */
                channel.eventLoop().execute(fireDrainOnRequestMoreTask);
            }
        }));

        op.setProducer(new Producer() {
            @Override
            public void request(long n) {
                if (Long.MAX_VALUE == requested) {
                    // No backpressure.
                    return;
                }

                if (Long.MAX_VALUE == n) {
                    // Now turning off backpressure
                    REQUEST_UPDATER.set(CollaboratedReadInputSubscriber.this, Long.MAX_VALUE);
                } else {
                    // add n to field but check for overflow
                    while (true) {
                        long current = REQUEST_UPDATER.get(CollaboratedReadInputSubscriber.this);
                        long next = current + n;
                        // check for overflow
                        if (next < 0) {
                            next = Long.MAX_VALUE;
                        }
                        if (REQUEST_UPDATER.compareAndSet(CollaboratedReadInputSubscriber.this, current, next)) {
                            break;
                        }
                    }
                }

                /*
                 * Executing on the eventloop as it needs to check whether the channel is registered or not,
                 * which introduces a race-condition.
                 */
                channel.eventLoop().execute(fireDrainOnRequestMoreTask);
            }
        });
    }

    @Override
    public void onCompleted() {
        if (terminated) {
            return;
        }
        terminated = true;
        if (null != ringBuffer) {
            ringBuffer.onCompleted();
            drain(); // Drains the terminal event too.
        } else {
            original.onCompleted();
        }
    }

    @Override
    public void onError(Throwable e) {
        if (terminated) {
            return;
        }
        terminated = true;
        if (null != ringBuffer) {
            ringBuffer.onError(e);
            drain(); // Drains the terminal event too.
        } else {
            original.onError(e);
        }
    }

    @Override
    public void onNext(I item) {
        if (terminated) {
            ReferenceCountUtil.release(item); // discard item if no one is subscribed.
        } else if (requested > 0) {
            drain();
            invokeOnNext(item);
        } else {
            if (channel.config().isAutoRead()) {
                /*
                 * If auto-read was on then turn it off on buffer start as there is no reason to read and buffer data.
                 * If, it is desired to not turn off auto-read then the downstream subscriber should request larger
                 * number of items.
                 */
                channel.config().setAutoRead(false);
            }
            if (null == ringBuffer) {
                ringBuffer = RxRingBuffer.getSpscInstance();
            }
            try {
                ringBuffer.onNext(nl.next(item));
            } catch (MissingBackpressureException e) {
                /**
                 * If the queue blows up, there is no need to send any items to the subscriber.
                 * A subscriber will unsubscribe on error, which will discard the left over messages.
                 */
                original.onError(e);
            }
        }
    }

    /**
     * Drains the buffer, if any. This should NOT be called concurrently with {@link #onNext(Object)},
     * {@link #onError(Throwable)} and {@link #onCompleted()}
     */
    public void drain() {
        if (draining /*Do not drain on re-entrant.*/ || null == ringBuffer || ringBuffer.isUnsubscribed()) {
            return;
        }

        if (original.isUnsubscribed()) {
            /* Clear the buffer & release items*/
            Object nextNotification;
            while ((nextNotification = ringBuffer.poll()) != null) {
                ReferenceCountUtil.release(nextNotification);
            }
            ringBuffer.unsubscribe();
            return; // Since, the subscriber is unsubscribed, there isn't anything else to do.
        }

        draining = true;
        try {
            /* Clear the buffer & send notifications*/
            Object nextNotification;
            while ((requested > 0 || isTerminalNotificationFromBuffer(ringBuffer.peek()))
                     && (nextNotification = ringBuffer.poll()) != null) {
                /*If the subscriber is still requesting more or the next notification is terminal*/
                ringBuffer.accept(nextNotification, original);
                /*Decrement requested if this is an onNext and backpressure is requested.*/
                if (!isTerminalNotificationFromBuffer(nextNotification)
                            && REQUEST_UPDATER.get(this) != Long.MAX_VALUE) {
                    REQUEST_UPDATER.decrementAndGet(this);
                }
            }
        } finally {
            draining = false;
        }
    }

    private boolean isTerminalNotificationFromBuffer(Object nextNotification) {
        return ringBuffer.isCompleted(nextNotification) || ringBuffer.isError(nextNotification);
    }

    public boolean shouldReadMore() {
        return !terminated && REQUEST_UPDATER.get(this) > 0;
    }

    /*Visible for testing*/ long getRequested() {
        return requested;
    }

    /*Visible for testing*/ int getItemsCountInBuffer() {
        return null != ringBuffer ? ringBuffer.count() : 0;
    }

    private void invokeOnNext(I item) {
        original.onNext(item);
        if (REQUEST_UPDATER.get(this) != Long.MAX_VALUE) {
            REQUEST_UPDATER.decrementAndGet(this);
        }
    }
}
