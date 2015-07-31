package io.reactivex.netty.util;

import rx.Subscriber;
import rx.annotations.Experimental;
import rx.exceptions.Exceptions;
import rx.exceptions.MissingBackpressureException;
import rx.functions.Action0;
import rx.internal.util.BackpressureDrainManager;
import rx.internal.util.BackpressureDrainManager.BackpressureQueueCallback;
import rx.subjects.Subject;
import rx.subscriptions.Subscriptions;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * An {@code Observable} that only supports a single active subscriber and buffers messages on back pressure and when
 * there is no active subscription.
 *
 * <h2>Add new messages</h2>
 *
 * New messages can be added to this subject via
 * <ul>
 <li>{@link #onNext(Object)}: This throws an error if there is a buffer overflow.</li>
 <li>{@link #offerNext(Object)}: This returns {@code false} if there is a buffer overflow.</li>
 </ul>
 *
 * <h2>Backpressure</h2>
 *
 * This subject supports backpressure from the only concurrent subscriber it can have at any time. The buffer limits
 * that are specified while creating the subject is the maximum buffer that is allowed during backpressure.
 *
 * @param <T> The type of objects accepted by this subject.
 */
@Experimental
public class UnicastBufferingSubject<T> extends Subject<T, T> {

    private final State<T> state;

    protected UnicastBufferingSubject(OnSubscribe<T> onSubscribe, State<T> state) {
        super(onSubscribe);
        this.state = state;
    }

    public static <T> UnicastBufferingSubject<T> create(long bufferSize) {
        final State<T> state = new State<>(bufferSize);
        return new UnicastBufferingSubject<>(new OnSubscribe<T>() {
            @Override
            public void call(Subscriber<? super T> subscriber) {
                state.registerSubscriber(subscriber);
            }
        }, state);
    }

    public boolean isTerminated() {
        synchronized (state) {
            if (null != state.producer) {
                return state.producer.isTerminated();
            } else {
                return state.terminatedBeforeSubscribe;
            }
        }
    }

    @Override
    public boolean hasObservers() {
        return null != state.subscriber;
    }

    @Override
    public void onCompleted() {
        BackpressureDrainManager p = null;
        /* If a subscriber is active, send the completion to the subscriber, else store it to be delivered to the
         * buffered producer post subscription.*/
        synchronized (state) {
            if (null != state.producer) {
                p = state.producer;
            } else {
                state.terminatedBeforeSubscribe = true;
                state.errorBeforeSubscribe = null;
            }
        }

        /*Send callbacks outside the sync block*/
        if (null != p) {
            p.terminateAndDrain();
        }
    }

    @Override
    public void onError(Throwable e) {
        BackpressureDrainManager p = null;
        /* If a subscriber is active, send the completion to the subscriber, else store it to be delivered to the
         * buffered producer post subscription.*/
        synchronized (state) {
            if (null != state.producer) {
                p = state.producer;
            } else {
                state.terminatedBeforeSubscribe = true;
                state.errorBeforeSubscribe = e;
            }
        }

        /*Send callbacks outside the sync block*/
        if (null != p) {
            p.terminateAndDrain(e);
        }
    }

    @Override
    public void onNext(T t) {
        try {
            addNext(t);
        } catch (MissingBackpressureException e) {
            throw Exceptions.propagate(e);
        }
    }

    private void addNext(T next) throws MissingBackpressureException {
        if (isTerminated()) {
            throw new IllegalStateException("Observable is already completed.");
        }

        /*Check for overflow*/
        while (true) {
            final long currentSize = state.currentSize.get();
            final long newSize = currentSize + 1;
            if (newSize > state.maxBufferedCount) {
                throw new MissingBackpressureException("Max buffer limit exceeded. Current size: " + currentSize);
            }

            if (state.currentSize.compareAndSet(currentSize, newSize)) {
                break;
            }
        }

        state.nexts.add(next);

        BackpressureDrainManager p = null;
        /*Drain the producer, if a subscriber is active.*/
        synchronized (state) {
            if (null != state.producer) {
                p = state.producer;
            }
        }
        if (null != p) {
            p.drain();
        }
    }

    /**
     * Offers the passed item to this subject. Same as {@link #onNext(Object)} just that this method does not throw an
     * exception in case of buffer overflow, instead returns a {@code false}.
     *
     * @param next Next item to offer.
     *
     * @return {@code true} if the item was accepted, {@code false} if the subject is already terminated or the buffer
     * is full.
     */
    public boolean offerNext(T next) {
        try {
            addNext(next);
            return true;
        } catch (MissingBackpressureException e) {
            return false;
        }
    }

    private static final class State<T> {

        private final ConcurrentLinkedQueue<T> nexts;

        private final BackpressureQueueCallbackImpl queueCallback;
        private final AtomicLong currentSize = new AtomicLong();

        private final long maxBufferedCount;

        private volatile Subscriber<? super T> subscriber;
        private volatile BackpressureDrainManager producer;
        private volatile Throwable errorBeforeSubscribe;
        private volatile boolean terminatedBeforeSubscribe;

        private State(long maxBufferedCount) {
            this.maxBufferedCount = maxBufferedCount;
            nexts = new ConcurrentLinkedQueue<>();
            queueCallback = new BackpressureQueueCallbackImpl();
        }

        public void registerSubscriber(final Subscriber<? super T> subscriber) {

            boolean _shdSubscribe = false;
            boolean _terminated = false;
            Throwable _terminalError = null;
            BackpressureDrainManager p = null;
            synchronized (this) {
                if (null == this.subscriber) {
                    this.subscriber = subscriber;
                    _shdSubscribe = true;
                    _terminated = terminatedBeforeSubscribe;
                    _terminalError = errorBeforeSubscribe;
                    p = new BackpressureDrainManager(queueCallback);
                    producer = p;
                }
            }

            if (_shdSubscribe) {
                subscriber.add(Subscriptions.create(new Action0() {
                    @Override
                    public void call() {
                        synchronized (State.this) {
                            State.this.subscriber = null;
                            State.this.producer = null;
                            /**
                             * Why not clear the terminate-before-subscribe state?
                             * It can be so that there are multiple subscribers and the first subscriber did not
                             * completely consume the events and hence on unsubscribe clears the terminal state.
                             * The new subscriber will never get the terminal state in this case.
                             */
                        }
                    }
                }));
                subscriber.setProducer(p);
                if (_terminated) {
                    p.terminateAndDrain(_terminalError);
                }
            } else {
                subscriber.onError(new IllegalStateException("Only one subscriber is allowed."));
            }
        }

        /**
         * Shared {@link BackpressureQueueCallback} for all producers (subscribers) as there is no state in this
         * callback.
         */
        private class BackpressureQueueCallbackImpl implements BackpressureQueueCallback {

            @Override
            public Object peek() {
                return nexts.peek();
            }

            @Override
            public Object poll() {
                T poll = nexts.poll();
                if (null != poll) {
                    currentSize.decrementAndGet();
                }
                return poll;
            }

            @Override
            public boolean accept(Object next) {
                @SuppressWarnings("unchecked")
                T t = (T) next;
                subscriber.onNext(t);
                return false;
            }

            @Override
            public void complete(Throwable exception) {
                if (null == exception) {
                    subscriber.onCompleted();
                } else {
                    subscriber.onError(exception);
                }
            }
        }
    }
}
