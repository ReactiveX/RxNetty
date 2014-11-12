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
package io.reactivex.netty.metrics;

import rx.Subscription;
import rx.exceptions.Exceptions;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A subject which is both a {@link MetricEventsListener} and {@link MetricEventsPublisher}.
 * Any invocation of {@link io.reactivex.netty.metrics.MetricEventsListener} methods invoke on this will in turn call
 * all the underlying listeners. If any of the listener throws a fatal exception (as determined by
 * {@link rx.exceptions.Exceptions#throwIfFatal(Throwable)}) no other listener is invoked, otherwise all the listeners
 * are invoked. In case any of the listener threw a non-fatal exception, eventually, the invoked method on this subject
 * will throw a {@link ListenerInvocationException} with all the non-fatal exceptions thrown by all listeners for this
 * call.
 *
 * @author Nitesh Kant
 */
public class MetricEventsSubject<E extends MetricsEvent<?>> implements MetricEventsPublisher<E>, MetricEventsListener<E> {

    private final CopyOnWriteArrayList<SafeListener<? extends E>> listeners;

    public MetricEventsSubject() {
        listeners = new CopyOnWriteArrayList<SafeListener<? extends E>>();
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onEvent(E event, long duration, TimeUnit timeUnit, Throwable throwable, Object value) {
        ListenerInvocationException exception = null;
        for (SafeListener listener : listeners) {
            try {
                listener.onEvent(event, duration, timeUnit, throwable, value);
            } catch (Throwable e) {
                exception = handleListenerError(exception, listener, e);
            }
        }
        throwIfErrorOccured(exception);
    }

    public void onEvent(E event, long durationInMillis, Throwable throwable, Object value) {
        onEvent(event, durationInMillis, TimeUnit.MILLISECONDS, throwable, value);
    }

    public void onEvent(E event) {
        onEvent(event, NO_DURATION, NO_TIME_UNIT, NO_ERROR, NO_VALUE);
    }

    public void onEvent(E event, Throwable throwable) {
        onEvent(event, NO_DURATION, NO_TIME_UNIT, throwable, NO_VALUE);
    }

    public void onEvent(E event, long duration, TimeUnit timeUnit) {
        onEvent(event, duration, timeUnit, NO_ERROR, NO_VALUE);
    }

    public void onEvent(E event, long duration, TimeUnit timeUnit, Throwable throwable) {
        onEvent(event, duration, timeUnit, throwable, NO_VALUE);
    }

    public void onEvent(E event, long duration, TimeUnit timeUnit, Object value) {
        onEvent(event, duration, timeUnit, NO_ERROR, value);
    }

    public void onEvent(E event, long durationInMillis) {
        onEvent(event, durationInMillis, TimeUnit.MILLISECONDS, NO_ERROR, NO_VALUE);
    }

    public void onEvent(E event, long durationInMillis, Throwable throwable) {
        onEvent(event, durationInMillis, TimeUnit.MILLISECONDS, throwable, NO_VALUE);
    }

    public void onEvent(E event, long durationInMillis, Object value) {
        onEvent(event, durationInMillis, TimeUnit.MILLISECONDS, NO_ERROR, value);
    }

    public void onEvent(E event, Object value) {
        onEvent(event, NO_DURATION, NO_TIME_UNIT, NO_ERROR, value);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onCompleted() {
        ListenerInvocationException exception = null;
        for (SafeListener listener : listeners) {
            try {
                listener.onCompleted();
            } catch (Throwable e) {
                exception = handleListenerError(exception, listener, e);
            }
        }
        throwIfErrorOccured(exception);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onSubscribe() {
        ListenerInvocationException exception = null;
        for (SafeListener listener : listeners) {
            try {
                listener.onSubscribe();
            } catch (Throwable e) {
                exception = handleListenerError(exception, listener, e);
            }
        }
        throwIfErrorOccured(exception);
    }

    @Override
    public Subscription subscribe(final MetricEventsListener<? extends E> listener) {
        Subscription subscription = Subscriptions.create(new Action0() {
            @Override
            public void call() {
                listeners.remove(listener);
            }
        });
        listeners.add(new SafeListener<E>(listener, subscription));
        listener.onSubscribe();
        return subscription;
    }

    protected ListenerInvocationException handleListenerError(ListenerInvocationException exception,
                                                              SafeListener<? extends E> listener, Throwable e) {
        Exceptions.throwIfFatal(e);
        if (null == exception) {
            exception = new ListenerInvocationException();
        }
        exception.addException(listener.delegate, e);
        return exception;
    }

    protected void throwIfErrorOccured(ListenerInvocationException exception) {
        if (null != exception) {
            exception.finish();
            throw exception;
        }
    }

    private static class SafeListener<E extends MetricsEvent<?>> implements MetricEventsListener<E> {

        @SuppressWarnings("rawtypes") private final MetricEventsListener delegate;
        private final Subscription subscription;
        private final AtomicBoolean isDone;

        public SafeListener(MetricEventsListener<? extends E> delegate, Subscription subscription) {
            this.delegate = delegate;
            this.subscription = subscription;
            isDone = new AtomicBoolean();
        }

        @Override
        @SuppressWarnings("unchecked")
        public void onEvent(E event, long duration, TimeUnit timeUnit, Throwable throwable, Object value) {
            if (!isDone.get()) {
                delegate.onEvent(event, duration, timeUnit, throwable, value);
            }
        }

        @Override
        public void onCompleted() {
            if (isDone.compareAndSet(false, true)) {
                try {
                    delegate.onCompleted();
                } finally {
                    subscription.unsubscribe();
                }
            }
        }

        @Override
        public void onSubscribe() {
            if (!isDone.get()) {
                delegate.onSubscribe();
            }
        }
    }
}
