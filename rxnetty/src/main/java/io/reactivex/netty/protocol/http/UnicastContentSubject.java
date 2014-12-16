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

package io.reactivex.netty.protocol.http;

import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.internal.operators.NotificationLite;
import rx.observers.SerializedObserver;
import rx.observers.Subscribers;
import rx.schedulers.Schedulers;
import rx.subjects.Subject;
import rx.subscriptions.Subscriptions;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * A {@link Subject} implementation to be used by {@link HttpClientResponse} and {@link HttpServerRequest}.
 *
 * <h2>Unicast</h2>
 * This implementation allows a single subscription as this buffers the content till subscription to solve the issue
 * described in <a href="https://github.com/Netflix/RxNetty/issues/206">this github issue</a>.
 * If we allow multiple subscription and still maintain it as a cold observable i.e. which re-runs the stream on every
 * subscription, we loose the control on the scope of this {@link Observable} as any code can hold the reference to
 * this {@link Observable} at any point in time and hence subscribe to it at any time. This will eventually increase
 * memory consumption as we will hold on to the content buffers for more time than required.
 *
 * <h2>Multicast option</h2>
 * If at all it is required to allow multiple subscriptions to this {@link Subject} one should use a
 * {@link Observable#publish()} operator.
 *
 * <h2>Buffering</h2>
 * This subject will buffer all content till the first (one and only) subscriber arrives.
 * In cases, when there are no subscriptions, this buffer may be held till infinity and hence can cause a memory leak in
 * case of netty's {@link ByteBuf} which needs to be released explicitly.
 * In order to avoid this leak, this subject provides a "no subscription timeout" which disposes this subject (calling
 * {@link #disposeIfNotSubscribed()} if it does not get a subscription in the configured timeout duration.
 *
 * The buffer is only utilized if there are any items emitted to this subject before a subscription arrives. After a
 * subscription arrives, this subject becomes a pass through i.e. it does not buffer before sending the notifications.
 *
 * This implementation is inspired by
 * <a href="https://github.com/Netflix/RxJava/blob/master/rxjava-core/src/main/java/rx/internal/operators/BufferUntilSubscriber.java">RxJava's BufferUntilSubscriber</a>
 * @author Nitesh Kant
 */
public final class UnicastContentSubject<T> extends Subject<T, T> {

    private final State<T> state;
    private volatile Observable<Long> timeoutScheduler;

    private UnicastContentSubject(State<T> state) {
        super(new OnSubscribeAction<T>(state));
        this.state = state;
        timeoutScheduler = Observable.empty(); // No timeout.
    }

    private UnicastContentSubject(final State<T> state, long noSubscriptionTimeout, TimeUnit timeUnit,
                                  Scheduler scheduler) {
        super(new OnSubscribeAction<T>(state));
        this.state = state;
        timeoutScheduler = Observable.interval(noSubscriptionTimeout, timeUnit, scheduler).take(1); // Started when content arrives.
    }

    /**
     * Creates a new {@link UnicastContentSubject} without a no subscription timeout.
     * <b>This can cause a memory leak in case no one ever subscribes to this subject.</b> See
     * {@link UnicastContentSubject} for details.
     *
     * @param onUnsubscribe An action to be invoked when the sole subscriber to this {@link Subject} unsubscribes.
     * @param <T> The type emitted and received by this subject.
     *
     * @return The new instance of {@link UnicastContentSubject}
     */
    public static <T> UnicastContentSubject<T> createWithoutNoSubscriptionTimeout(Action0 onUnsubscribe) {
        State<T> state = new State<T>(onUnsubscribe);
        return new UnicastContentSubject<T>(state);
    }

    /**
     * Creates a new {@link UnicastContentSubject} without a no subscription timeout.
     * <b>This can cause a memory leak in case no one ever subscribes to this subject.</b> See
     * {@link UnicastContentSubject} for details.
     *
     * @param <T> The type emitted and received by this subject.
     *
     * @return The new instance of {@link UnicastContentSubject}
     */
    public static <T> UnicastContentSubject<T> createWithoutNoSubscriptionTimeout() {
        return createWithoutNoSubscriptionTimeout(null);
    }

    public static <T> UnicastContentSubject<T> create(long noSubscriptionTimeout, TimeUnit timeUnit) {
        return create(noSubscriptionTimeout, timeUnit, (Action0)null);
    }

    public static <T> UnicastContentSubject<T> create(long noSubscriptionTimeout, TimeUnit timeUnit,
                                                      Action0 onUnsubscribe) {
        return create(noSubscriptionTimeout, timeUnit, Schedulers.computation(), onUnsubscribe);
    }

    public static <T> UnicastContentSubject<T> create(long noSubscriptionTimeout, TimeUnit timeUnit,
                                                      Scheduler timeoutScheduler) {
        return create(noSubscriptionTimeout, timeUnit, timeoutScheduler, null);
    }

    public static <T> UnicastContentSubject<T> create(long noSubscriptionTimeout, TimeUnit timeUnit,
                                                      Scheduler timeoutScheduler, Action0 onUnsubscribe) {
        State<T> state = new State<T>(onUnsubscribe);
        return new UnicastContentSubject<T>(state, noSubscriptionTimeout, timeUnit, timeoutScheduler);
    }

    /**
     * This will eagerly dispose this {@link Subject} without waiting for the no subscription timeout period,
     * if configured.
     *
     * This must be invoked when the caller is sure that no one will subscribe to this subject. Any subscriber after
     * this call will receive an error that the subject is disposed.
     *
     * @return {@code true} if the subject was disposed by this call (if and only if there was no subscription).
     */
    public boolean disposeIfNotSubscribed() {
        if (state.casState(State.STATES.UNSUBSCRIBED, State.STATES.DISPOSED)) {
            Subscriber<T> noOpSub = Subscribers.empty();

            state.buffer.sendAllNotifications(noOpSub);
            state.setObserverRef(noOpSub); // All future notifications are not sent anywhere.
            if (null != state.onUnsubscribe) {
                state.onUnsubscribe.call(); // Since this is an inline/sync call, if this throws an error, it gets thrown to the caller.
            }
            state.buffer.sendAllNotifications(state.observerRef);
            return true;
        }
        return false;
    }

    public void updateTimeoutIfNotScheduled(long noSubscriptionTimeout, TimeUnit timeUnit) {
        if (0 == state.timeoutScheduled) {
            timeoutScheduler = Observable.interval(noSubscriptionTimeout, timeUnit).take(1);
        }
    }

    /** The common state. */
    private static final class State<T> {

        private final Action0 onUnsubscribe;

        public State() {
            this(null);
        }

        public State(Action0 onUnsubscribe) {
            this.onUnsubscribe = onUnsubscribe;
        }

        /**
         * Following are the only possible state transitions:
         * UNSUBSCRIBED -> SUBSCRIBED
         * UNSUBSCRIBED -> DISPOSED
         */
        private enum STATES {
            UNSUBSCRIBED /*Initial*/, SUBSCRIBED /*Terminal state*/, DISPOSED/*Terminal state*/
        }

        private volatile int state = STATES.UNSUBSCRIBED.ordinal(); /*Values are the ordinals of STATES enum*/

        /** Following Observers are associated with the states:
         * UNSUBSCRIBED => {@link BufferedObserver}
         * SUBSCRIBED => actual observer
         * DISPOSED => {@link Subscribers#empty()}
         */
        private volatile Observer<? super T> observerRef = new BufferedObserver();

        @SuppressWarnings("unused")private volatile int timeoutScheduled; // Boolean

        /**
         * The only buffer associated with this state. All notifications go to this buffer if no one has subscribed and
         * the {@link UnicastContentSubject} instance is not disposed.
         */
        private final ByteBufAwareBuffer<T> buffer = new ByteBufAwareBuffer<T>();

        /** Field updater for observerRef. */
        @SuppressWarnings("rawtypes")
        private static final AtomicReferenceFieldUpdater<State, Observer> OBSERVER_UPDATER
                = AtomicReferenceFieldUpdater.newUpdater(State.class, Observer.class, "observerRef");

        /** Field updater for state. */
        @SuppressWarnings("rawtypes")
        private static final AtomicIntegerFieldUpdater<State> STATE_UPDATER
                = AtomicIntegerFieldUpdater.newUpdater(State.class, "state");

        /** Field updater for timeoutScheduled. */
        @SuppressWarnings("rawtypes")
        private static final AtomicIntegerFieldUpdater<State> TIMEOUT_SCHEDULED_UPDATER
                = AtomicIntegerFieldUpdater.newUpdater(State.class, "timeoutScheduled");

        public boolean casState(STATES expected, STATES next) {
            return STATE_UPDATER.compareAndSet(this, expected.ordinal(), next.ordinal());
        }

        public void setObserverRef(Observer<? super T> o) { // Guarded by casState()
            observerRef = new SerializedObserver<T>(o);
        }

        public boolean casObserverRef(Observer<? super T> expected, Observer<? super T> next) {
            return OBSERVER_UPDATER.compareAndSet(this, expected, next);
        }

        public boolean casTimeoutScheduled() {
            return TIMEOUT_SCHEDULED_UPDATER.compareAndSet(this, 0, 1);
        }

        /**
         * The default subscriber when the enclosing state is created.
         */
        private final class BufferedObserver extends Subscriber<T> {

            private final NotificationLite<Object> nl = NotificationLite.instance();

            @Override
            public void onCompleted() {
                buffer.add(nl.completed());
            }

            @Override
            public void onError(Throwable e) {
                buffer.add(nl.error(e));
            }

            @Override
            public void onNext(T t) {
                buffer.add(nl.next(t));
            }
        }
    }

    private static final class OnSubscribeAction<T> implements OnSubscribe<T> {

        private final State<T> state;

        public OnSubscribeAction(State<T> state) {
            this.state = state;
        }

        @Override
        public void call(final Subscriber<? super T> subscriber) {
            if (state.casState(State.STATES.UNSUBSCRIBED, State.STATES.SUBSCRIBED)) {

                // drain queued notifications before subscription
                state.buffer.sendAllNotifications(subscriber);

                state.setObserverRef(subscriber);
                subscriber.add(Subscriptions.create(new Action0() {
                    @Override
                    public void call() {
                        if (null != state.onUnsubscribe) {
                            state.onUnsubscribe.call();
                        }
                        state.setObserverRef(Subscribers.empty());
                    }
                }));
                state.buffer.sendAllNotifications(state.observerRef);
            } else if(State.STATES.SUBSCRIBED.ordinal() == state.state) {
                subscriber.onError(new IllegalStateException("Content can only have one subscription. Use Observable.publish() if you want to multicast."));
            } else if(State.STATES.DISPOSED.ordinal() == state.state) {
                subscriber.onError(new IllegalStateException("Content stream is already disposed."));
            }
        }

    }

    @Override
    public void onCompleted() {
        state.observerRef.onCompleted();
    }

    @Override
    public void onError(Throwable e) {
        state.observerRef.onError(e);
    }

    @Override
    public void onNext(T t) {
        state.observerRef.onNext(t);
        if (state.casTimeoutScheduled()) {// Schedule timeout once.
            timeoutScheduler.subscribe(new Action1<Long>() { // Schedule timeout after the first content arrives.
                @Override
                public void call(Long aLong) {
                    disposeIfNotSubscribed();
                }
            });
        }
    }

    private static final class ByteBufAwareBuffer<T> {

        private final ConcurrentLinkedQueue<Object> actual = new ConcurrentLinkedQueue<Object>();
        private final NotificationLite<T> nl = NotificationLite.instance();

        private void add(Object toAdd) {
            ReferenceCountUtil.retain(toAdd); // Released when the notification is sent.
            actual.add(toAdd);
        }

        public void sendAllNotifications(Observer<? super T> observer) {
            Object notification; // Can be onComplete notification, onError notification or just the actual "T".
            while ((notification = actual.poll()) != null) {
                try {
                    nl.accept(observer, notification);
                } finally {
                    ReferenceCountUtil.release(notification); // If it is the actual T for onNext and is a ByteBuf, it will be released.
                }
            }
        }
    }

    @Override
    public boolean hasObservers() {
        return state.observerRef != null;
    }
}
