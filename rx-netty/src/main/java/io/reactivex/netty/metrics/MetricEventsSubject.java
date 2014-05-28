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

import rx.exceptions.Exceptions;

import java.util.concurrent.CopyOnWriteArrayList;

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
public class MetricEventsSubject<E extends MetricsEvent> implements MetricEventsPublisher<E>, MetricEventsListener<E> {

    private final CopyOnWriteArrayList<MetricEventsListener<? extends E>> listeners;

    public MetricEventsSubject() {
        listeners = new CopyOnWriteArrayList<MetricEventsListener<? extends E>>();
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onEvent(E event) {
        ListenerInvocationException exception = null;
        for (MetricEventsListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (Throwable e) {
                exception = handleListenerError(exception, listener, e);
            }
        }
        throwIfErrorOccured(exception);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onEvent(E event, long duration) {
        ListenerInvocationException exception = null;
        for (MetricEventsListener listener : listeners) {
            try {
                listener.onEvent(event, duration);
            } catch (Throwable e) {
                exception = handleListenerError(exception, listener, e);
            }
        }
        throwIfErrorOccured(exception);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onEvent(E event, Throwable throwable) {
        ListenerInvocationException exception = null;
        for (MetricEventsListener listener : listeners) {
            try {
                listener.onEvent(event, throwable);
            } catch (Throwable e) {
                exception = handleListenerError(exception, listener, e);
            }
        }
        throwIfErrorOccured(exception);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onEvent(E event, Throwable throwable, long duration) {
        ListenerInvocationException exception = null;
        for (MetricEventsListener listener : listeners) {
            try {
                listener.onEvent(event, throwable, duration);
            } catch (Throwable e) {
                exception = handleListenerError(exception, listener, e);
            }
        }
        throwIfErrorOccured(exception);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onEvent(E event, Object value) {
        ListenerInvocationException exception = null;
        for (MetricEventsListener listener : listeners) {
            try {
                listener.onEvent(event, value);
            } catch (Throwable e) {
                exception = handleListenerError(exception, listener, e);
            }
        }
        throwIfErrorOccured(exception);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onEvent(E event, Object value, long duration) {
        ListenerInvocationException exception = null;
        for (MetricEventsListener listener : listeners) {
            try {
                listener.onEvent(event, value, duration);
            } catch (Throwable e) {
                exception = handleListenerError(exception, listener, e);
            }
        }
        throwIfErrorOccured(exception);
    }

    @Override
    public void addListener(MetricEventsListener<? extends E> listener) {
        listeners.add(listener);
    }

    @Override
    public boolean removeListener(MetricEventsListener<? extends E> listener) {
        return listeners.remove(listener);
    }

    protected ListenerInvocationException handleListenerError(ListenerInvocationException exception,
                                                              MetricEventsListener<? extends E> listener, Throwable e) {
        Exceptions.throwIfFatal(e);
        if (null == exception) {
            exception = new ListenerInvocationException();
        }
        exception.addException(listener, e);
        return exception;
    }

    protected void throwIfErrorOccured(ListenerInvocationException exception) {
        if (null != exception) {
            throw exception;
        }
    }
}
