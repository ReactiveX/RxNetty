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
package io.reactivex.netty.channel.events;

import io.reactivex.netty.events.EventListener;
import io.reactivex.netty.events.EventPublisher;
import io.reactivex.netty.events.EventSource;
import io.reactivex.netty.events.ListenersHolder;
import rx.Subscription;
import rx.functions.Action1;

import java.util.concurrent.TimeUnit;

/**
 * A publisher which is both {@link EventSource} and {@link EventListener} for connection events.
 *
 * @param <T> Type of listener to expect.
 */
public final class ConnectionEventPublisher<T extends ConnectionEventListener> extends ConnectionEventListener
        implements EventSource<T>, EventPublisher {

    private final ListenersHolder<T> listeners;

    public ConnectionEventPublisher() {
        listeners = new ListenersHolder<>();
    }

    public ConnectionEventPublisher(ConnectionEventPublisher<T> toCopy) {
        listeners = toCopy.listeners.copy();
    }

    @Override
    public void onByteRead(final long bytesRead) {
        listeners.invokeListeners(new Action1<T>() {
            @Override
            public void call(T t) {
                t.onByteRead(bytesRead);
            }
        });
    }

    @Override
    public void onFlushStart() {
        listeners.invokeListeners(new Action1<T>() {
            @Override
            public void call(T t) {
                t.onFlushStart();
            }
        });
    }

    @Override
    public void onFlushSuccess(final long duration, final TimeUnit timeUnit) {
        listeners.invokeListeners(new Action1<T>() {
            @Override
            public void call(T t) {
                t.onFlushSuccess(duration, timeUnit);
            }
        });
    }

    @Override
    public void onFlushFailed(final long duration, final TimeUnit timeUnit, final Throwable throwable) {
        listeners.invokeListeners(new Action1<T>() {
            @Override
            public void call(T t) {
                t.onFlushFailed(duration, timeUnit, throwable);
            }
        });
    }

    @Override
    public void onWriteStart() {
        listeners.invokeListeners(new Action1<T>() {
            @Override
            public void call(T t) {
                t.onWriteStart();
            }
        });
    }

    @Override
    public void onWriteSuccess(final long duration, final TimeUnit timeUnit, final long bytesWritten) {
        listeners.invokeListeners(new Action1<T>() {
            @Override
            public void call(T t) {
                t.onWriteSuccess(duration, timeUnit, bytesWritten);
            }
        });
    }

    @Override
    public void onWriteFailed(final long duration, final TimeUnit timeUnit, final Throwable throwable) {
        listeners.invokeListeners(new Action1<T>() {
            @Override
            public void call(T t) {
                t.onWriteFailed(duration, timeUnit, throwable);
            }
        });
    }

    @Override
    public void onConnectionCloseStart() {
        listeners.invokeListeners(new Action1<T>() {
            @Override
            public void call(T t) {
                t.onConnectionCloseStart();
            }
        });
    }

    @Override
    public void onConnectionCloseSuccess(final long duration, final TimeUnit timeUnit) {
        listeners.invokeListeners(new Action1<T>() {
            @Override
            public void call(T t) {
                t.onConnectionCloseSuccess(duration, timeUnit);
            }
        });
    }

    @Override
    public void onConnectionCloseFailed(final long duration, final TimeUnit timeUnit, final Throwable throwable) {
        listeners.invokeListeners(new Action1<T>() {
            @Override
            public void call(T t) {
                t.onConnectionCloseFailed(duration, timeUnit, throwable);
            }
        });
    }

    @Override
    public Subscription subscribe(T listener) {
        return listeners.subscribe(listener);
    }

    @Override
    public boolean publishingEnabled() {
        return listeners.publishingEnabled();
    }

    public ConnectionEventPublisher<T> copy() {
        return new ConnectionEventPublisher<>(this);
    }
}
