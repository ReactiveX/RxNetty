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
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.reactivex.netty.metrics.Clock;
import io.reactivex.netty.metrics.MetricEventsSubject;
import rx.Observable;
import rx.Subscriber;
import rx.internal.operators.NotificationLite;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * An extension of {@link Observable} to provide a gathering flush semantics via {@link ChannelOperations#flush()}.
 *
 * <h2>New writes</h2>
 *
 * Any new write {@link ChannelFuture} must be added using {@link #add(ChannelFuture)}
 *
 * <h2>Flush</h2>
 *
 * Every subscription to this {@link Observable} flushes all writes done before the subscription.
 *
 * <h2>Thread safety</h2>
 *
 * This class is thread-safe; allowing multiple threads writing (calling {@link #add(ChannelFuture)}) and subscribing
 * to the same instance. However, in order to get a predictable write-flush behavior, external synchronization would be
 * required to make sure that {@link #subscribe()} is called after all writes (intended to be flushed via subscribe)
 * have been called.
 *
 * @author Nitesh Kant
 */
public final class FlushObservable extends Observable<Void> {

    private final OnSubscribeFunc onSubscribeFunc;

    private FlushObservable(final OnSubscribeFunc onSubscribeFunc) {
        super(onSubscribeFunc);
        this.onSubscribeFunc = onSubscribeFunc;
    }

    public void add(ChannelFuture future) {
        onSubscribeFunc.nextSubscriberState.addFuture(future);
    }

    public void cancelPendingFutures(boolean mayInterruptIfRunning) {
        onSubscribeFunc.nextSubscriberState.cancelPendingFutures(mayInterruptIfRunning);
    }

    public static FlushObservable create(final MetricEventsSubject<?> eventsSubject,
                                         final ChannelMetricEventProvider metricEventProvider, Channel nettyChannel) {
        return new FlushObservable(new OnSubscribeFunc(eventsSubject, metricEventProvider, nettyChannel));
    }

    private static class OnSubscribeFunc implements OnSubscribe<Void> {

        /** Field updater for state. */
        @SuppressWarnings("rawtypes")
        private static final AtomicReferenceFieldUpdater<OnSubscribeFunc, State> STATE_UPDATER
                = AtomicReferenceFieldUpdater.newUpdater(OnSubscribeFunc.class, State.class, "nextSubscriberState");
        private volatile State nextSubscriberState; // updated by the atomic updater, so required to be volatile.

        @SuppressWarnings("rawtypes")
        private final MetricEventsSubject eventsSubject;
        private final ChannelMetricEventProvider metricEventProvider;
        private final Channel nettyChannel;

        private OnSubscribeFunc(MetricEventsSubject<?> eventsSubject, ChannelMetricEventProvider metricEventProvider,
                                Channel nettyChannel) {
            this.eventsSubject = eventsSubject;
            this.metricEventProvider = metricEventProvider;
            this.nettyChannel = nettyChannel;
            nextSubscriberState = new State(eventsSubject, metricEventProvider);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void call(Subscriber<? super Void> subscriber) {
            final long startTimeMillis = Clock.newStartTimeMillis();
            final State thisSubState = STATE_UPDATER.getAndSet(this, new State(eventsSubject, metricEventProvider));
            eventsSubject.onEvent(metricEventProvider.getFlushStartEvent());

            thisSubState.subscribe(startTimeMillis, subscriber);

            /**
             * Do flush() after CAS'ing the state. {@link #addFuture(ChannelFuture)} adds the future to the state.
             * So, we need to make sure that no writes happen (future add) after we have flushed. Since, the CAS makes
             * sure that nobody gets the handle of the state after a subscription, this subscriber will not wait for
             * unflushed writes. Failure to do this, will result in this subscriber waiting for the next
             * flush (write that happened after flush & before CAS)
             */
            nettyChannel.flush();
        }
    }

    private static class State {

        private final Object guard = new Object();
        private Subscriber<? super Void> subscriber; /* Guarded by guard */
        private long startTimeMillis; /* Guarded by guard */
        private Object terminalNotification;/* Guarded by guard */
        private int listeningCount; /*Guarded by guard */

        private static final NotificationLite<Void> notificationLiteSingleton = NotificationLite.instance();

        private final ConcurrentLinkedQueue<ChannelFuture> pendingFutures = new ConcurrentLinkedQueue<ChannelFuture>();
        @SuppressWarnings("rawtypes")
        private final MetricEventsSubject eventsSubject;
        private final ChannelMetricEventProvider metricEventProvider;

        private State(MetricEventsSubject<?> eventsSubject, ChannelMetricEventProvider metricEventProvider) {
            this.eventsSubject = eventsSubject;
            this.metricEventProvider = metricEventProvider;
        }

        private ChannelFutureListener newListener() {
            return new FutureListener();
        }

        private void subscribe(long startTimeMillis, final Subscriber<? super Void> subscriber) {

            boolean subscribeValid = false; // Keeping the synch block small and callback outside the block.
            Object terminalNotificationIfSet = null; // Keeping the synch block small and callback outside the block.
            int listeningCountAtSubscribe = 0;
            synchronized (guard) {
                if (null == this.subscriber) {
                    subscribeValid = true;
                    this.subscriber = subscriber;
                    this.startTimeMillis = startTimeMillis;
                    terminalNotificationIfSet = terminalNotification;
                    listeningCountAtSubscribe = listeningCount;
                }
            }

            /* Keep the synchronized block small & do callbacks outside sync. */
            if (!subscribeValid) {
                subscriber.onError(new IllegalStateException("Only one subscriber allowed.")); // One subscriber per state.
            } else if (null != terminalNotificationIfSet) {
                sendTerminalNotification(terminalNotificationIfSet, subscriber, startTimeMillis);
            } else if (0 == listeningCountAtSubscribe) {
                /*
                 * If no futures were added, then we will never get a terminal notification.
                 */
                sendTerminalNotification(notificationLiteSingleton.completed(), subscriber, startTimeMillis);
            }
        }

        public void addFuture(final ChannelFuture future) {
            pendingFutures.add(future);
            synchronized (guard) {
                listeningCount++;
            }
            future.addListener(newListener());
        }

        public void cancelPendingFutures(boolean mayInterruptIfRunning) {
            for (Iterator<ChannelFuture> iterator = pendingFutures.iterator(); iterator.hasNext(); ) {
                ChannelFuture pendingFuture = iterator.next();
                iterator.remove();
                pendingFuture.cancel(mayInterruptIfRunning);
            }
        }

        @SuppressWarnings("unchecked")
        private void sendTerminalNotification(Object terminalNotification, Subscriber<? super Void> subscriber,
                                              long startTimeMillis) {
            /* No non-final state should be used inside this method as this is called outside the sync block.*/
            if (notificationLiteSingleton.isError(terminalNotification)) {
                eventsSubject.onEvent(metricEventProvider.getFlushFailedEvent(), startTimeMillis);
            } else {
                eventsSubject.onEvent(metricEventProvider.getFlushSuccessEvent(), startTimeMillis);
            }
            notificationLiteSingleton.accept(subscriber, terminalNotification);
        }

        private class FutureListener implements ChannelFutureListener {

            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                pendingFutures.remove(future);
                Subscriber<? super Void> subscriberIfExists = null;// Keeping the synch block small and callback outside the block.
                Object terminalNotificationIfSetNow = null;// Keeping the synch block small and callback outside the block.
                long startTimeMillisIfSubFound = 0;

                synchronized (guard) {
                    listeningCount--;

                    if (null != subscriber) {
                        subscriberIfExists = subscriber;
                        startTimeMillisIfSubFound = startTimeMillis;
                    }

                    /**
                     * If any one of the future fails, fail the subscriber.
                     * If all complete (listen count == 0), complete the subscriber.
                     */
                    if (!future.isSuccess()) {
                        terminalNotification = notificationLiteSingleton.error(future.cause());
                        terminalNotificationIfSetNow = terminalNotification;
                    } else if (0 == listeningCount) {
                        terminalNotification = notificationLiteSingleton.completed();
                        terminalNotificationIfSetNow = terminalNotification;
                    }
                }

                if (null != subscriberIfExists && null != terminalNotificationIfSetNow) {
                    // Keeping the synch block small and invoking callbacks outside the sync block.
                    sendTerminalNotification(terminalNotificationIfSetNow, subscriberIfExists,
                                             startTimeMillisIfSubFound);
                }
            }
        }
    }

}
