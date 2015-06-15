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
package io.reactivex.netty.events;

import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.tcp.client.ConnectionObservable;
import io.reactivex.netty.protocol.tcp.client.events.TcpClientEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Subscription;
import rx.annotations.Beta;
import rx.exceptions.Exceptions;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Action2;
import rx.functions.Action3;
import rx.functions.Action4;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * A holder for storing {@link EventListener} providing utility methods for any {@link EventSource} implementation that
 * requires storing and invoking listeners.
 *
 * @param <T> Type of listener to store.
 */
public final class ListenersHolder<T extends EventListener> implements EventSource<T>, EventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(ListenersHolder.class);

    private final CopyOnWriteArrayList<ListenerHolder<T>> listeners;

    public ListenersHolder() {
        listeners = new CopyOnWriteArrayList<>();
    }

    public ListenersHolder(ListenersHolder<T> toCopy) {

        listeners = new CopyOnWriteArrayList<>(toCopy.listeners);

        for (final ListenerHolder<T> holder : listeners) {
            // Add the subscription to the existing holder, so that on unsubscribe, it is also removed from this list.
            holder.subscription.add(Subscriptions.create(new Action0() {
                @Override
                public void call() {
                    listeners.remove(holder);
                }
            }));
        }
    }

    @Override
    public Subscription subscribe(final T listener) {
        final CompositeSubscription cs = new CompositeSubscription();

        ListenerHolder.configureRemoval(cs, listener, listeners);

        final ListenerHolder<T> holder = new ListenerHolder<>(listener, cs);
        listeners.add(holder);
        return cs;
    }

    @Override
    public boolean publishingEnabled() {
        return !RxNetty.isEventPublishingDisabled() && !listeners.isEmpty();
    }

    public void dispose() {
        ListenerInvocationException exception = null;
        for (ListenerHolder<T> listener : listeners) {
            try {
                listener.onCompleted();
            } catch (Throwable e) {
                exception = handleListenerError(exception, listener, e);
            }
        }

        if (null != exception) {
            exception.finish();
            throw exception;
        }
    }

    /**
     * Invoke listeners with an action expressed by the passed {@code invocationAction}. This method does the necessary
     * validations required for invoking a listener and also guards against a listener throwing exceptions on invocation.
     *
     * @param invocationAction The action to perform on all listeners.
     */
    public void invokeListeners(Action1<T> invocationAction) {
        ListenerInvocationException exception = null;
        for (final ListenerHolder<T> listener : listeners) {
            if (!listener.subscription.isUnsubscribed()) {
                try {
                    invocationAction.call(listener.delegate);
                } catch (Throwable e) {
                    exception = handleListenerError(exception, listener, e);
                }
            }
        }

        if (null != exception) {
            exception.finish();
            /*Do not bubble event notification errors to the caller, event notifications are best effort.*/
            logger.error("Error occured while invoking event listeners.", exception);
        }
    }

    /**
     * Invoke listeners with an action expressed by the passed {@code invocationAction}. This method does the necessary
     * validations required for invoking a listener and also guards against a listener throwing exceptions on invocation.
     *
     * @param invocationAction The action to perform on all listeners.
     * @param duration Duration.
     * @param timeUnit Time unit for the duration.
     */
    public void invokeListeners(Action3<T, Long, TimeUnit> invocationAction, long duration, TimeUnit timeUnit) {
        ListenerInvocationException exception = null;
        for (ListenerHolder<T> listener : listeners) {
            if (!listener.subscription.isUnsubscribed()) {
                try {
                    invocationAction.call(listener.delegate, duration, timeUnit);
                } catch (Throwable e) {
                    exception = handleListenerError(exception, listener, e);
                }
            }
        }

        if (null != exception) {
            exception.finish();
            /*Do not bubble event notification errors to the caller, event notifications are best effort.*/
            logger.error("Error occured while invoking event listeners.", exception);
        }
    }

    /**
     * Invoke listeners with an action expressed by the passed {@code invocationAction}. This method does the necessary
     * validations required for invoking a listener and also guards against a listener throwing exceptions on invocation.
     *
     * @param invocationAction The action to perform on all listeners.
     * @param duration Duration.
     * @param timeUnit Time unit for the duration.
     * @param throwable An error.
     */
    public void invokeListeners(Action4<T, Long, TimeUnit, Throwable> invocationAction, long duration,
                                TimeUnit timeUnit, Throwable throwable) {
        ListenerInvocationException exception = null;
        for (ListenerHolder<T> listener : listeners) {
            if (!listener.subscription.isUnsubscribed()) {
                try {
                    invocationAction.call(listener.delegate, duration, timeUnit, throwable);
                } catch (Throwable e) {
                    exception = handleListenerError(exception, listener, e);
                }
            }
        }

        if (null != exception) {
            exception.finish();
            /*Do not bubble event notification errors to the caller, event notifications are best effort.*/
            logger.error("Error occured while invoking event listeners.", exception);
        }
    }

    /**
     * Invoke listeners with an action expressed by the passed {@code invocationAction}. This method does the necessary
     * validations required for invoking a listener and also guards against a listener throwing exceptions on invocation.
     *
     * @param invocationAction The action to perform on all listeners.
     * @param duration Duration.
     * @param timeUnit Time unit for the duration.
     * @param arg Any arbitrary argument
     */
    public <A> void invokeListeners(Action4<T, Long, TimeUnit, A> invocationAction, long duration,
                                    TimeUnit timeUnit, A arg) {
        ListenerInvocationException exception = null;
        for (ListenerHolder<T> listener : listeners) {
            if (!listener.subscription.isUnsubscribed()) {
                try {
                    invocationAction.call(listener.delegate, duration, timeUnit, arg);
                } catch (Throwable e) {
                    exception = handleListenerError(exception, listener, e);
                }
            }
        }

        if (null != exception) {
            exception.finish();
            /*Do not bubble event notification errors to the caller, event notifications are best effort.*/
            logger.error("Error occured while invoking event listeners.", exception);
        }
    }

    /**
     * Invoke listeners with an action expressed by the passed {@code invocationAction}. This method does the necessary
     * validations required for invoking a listener and also guards against a listener throwing exceptions on invocation.
     *
     * @param invocationAction The action to perform on all listeners.
     * @param arg Any arbitrary argument
     */
    public <A> void invokeListeners(Action2<T, A> invocationAction, A arg) {
        ListenerInvocationException exception = null;
        for (ListenerHolder<T> listener : listeners) {
            if (!listener.subscription.isUnsubscribed()) {
                try {
                    invocationAction.call(listener.delegate, arg);
                } catch (Throwable e) {
                    exception = handleListenerError(exception, listener, e);
                }
            }
        }

        if (null != exception) {
            exception.finish();
            /*Do not bubble event notification errors to the caller, event notifications are best effort.*/
            logger.error("Error occured while invoking event listeners.", exception);
        }
    }

    private ListenerInvocationException handleListenerError(ListenerInvocationException exception,
                                                              ListenerHolder<T> listener, Throwable e) {
        Exceptions.throwIfFatal(e);
        if (null == exception) {
            exception = new ListenerInvocationException();
        }
        exception.addException(listener.delegate, e);
        return exception;
    }

    public ListenersHolder<T> copy() {
        return new ListenersHolder<>(this);
    }

    /*Visible for testing*/Collection<T> getAllListeners() {
        final Collection<T> toReturn = new ArrayList<>();
        for (ListenerHolder<T> listener : listeners) {
            toReturn.add(listener.delegate);
        }
        return toReturn;
    }

    /*Visible for testing*/CopyOnWriteArrayList<ListenerHolder<T>> getActualListenersList() {
        return listeners;
    }

    public void subscribeAllTo(EventSource<T> lazySource) {
        for (ListenerHolder<T> listener : listeners) {
            listener.subscription.add(lazySource.subscribe(listener.delegate));
        }
    }

    @Beta
    public void subscribeAllTo(ConnectionObservable<?, ?> connectionObservable) {
        for (ListenerHolder<T> listener : listeners) {
            listener.subscription
                    .add(connectionObservable.subscribeForEvents((TcpClientEventListener) listener.delegate));
        }
    }

    private static class ListenerHolder<T extends EventListener> implements EventListener {

        private static final CompositeSubscription EMPTY_SUB_FOR_REMOVAL = new CompositeSubscription();

        private final T delegate;
        private final CompositeSubscription subscription;

        public ListenerHolder(T delegate, CompositeSubscription subscription) {
            this.delegate = delegate;
            this.subscription = subscription;
        }

        @Override
        public void onCompleted() {
            if (!subscription.isUnsubscribed()) {
                try {
                    delegate.onCompleted();
                } finally {
                    subscription.unsubscribe();
                }
            }
        }

        public static <X extends EventListener> ListenerHolder<X> forRemoval(X listenerToRemove) {
            return new ListenerHolder<>(listenerToRemove, EMPTY_SUB_FOR_REMOVAL);
        }

        public static <X extends EventListener> void configureRemoval(CompositeSubscription cs,
                                                                      final X listenerToRemove,
                                                                      final CopyOnWriteArrayList<ListenerHolder<X>> removeFrom) {
            cs.add(Subscriptions.create(new Action0() {
                @Override
                public void call() {
                    /**
                     * Why do we add {@link ListenerHolder} but remove {@link X}?
                     * Since {@link ListenerHolder} requires the associated {@link Subscription}, and then
                     * {@link Subscription} will require the {@link ListenerHolder}, there will be a circular dependency.
                     *
                     * Instead, by having {@link ListenerHolder} implement equals/hashcode to only look for the
                     * enclosing {@link X} instance, it is possible to add {@link ListenerHolder} but remove {@link X}
                     */
                    removeFrom.remove(forRemoval(listenerToRemove));
                }
            }));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (!(o instanceof ListenerHolder)) {
                return false;
            }

            @SuppressWarnings("rawtypes")
            ListenerHolder that = (ListenerHolder) o;

            if (!delegate.equals(that.delegate)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }
    }
}
