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
import rx.functions.Action2;
import rx.functions.Action3;
import rx.functions.Action4;

import java.util.concurrent.TimeUnit;

/**
 * A publisher which is both {@link EventSource} and {@link EventListener} for connection events.
 *
 * @param <T> Type of listener to expect.
 */
public final class ConnectionEventPublisher<T extends ConnectionEventListener> extends ConnectionEventListener
        implements EventSource<T>, EventPublisher {

    private final Action2<T, Long> bytesReadAction = new Action2<T, Long>() {
        @Override
        public void call(T l, Long bytesRead) {
            l.onByteRead(bytesRead);
        }
    };

    private final Action1<T> flushStartAction = new Action1<T>() {
        @Override
        public void call(T l) {
            l.onFlushStart();
        }
    };

    private final Action3<T, Long, TimeUnit> flushSuccessAction = new Action3<T, Long, TimeUnit>() {
                @Override
                public void call(T l, Long duration, TimeUnit timeUnit) {
                    l.onFlushSuccess(duration, timeUnit);
                }
            };

    private final Action4<T, Long, TimeUnit, Throwable> flushFailedAction =
            new Action4<T, Long, TimeUnit, Throwable>() {
                @Override
                public void call(T l, Long duration, TimeUnit timeUnit, Throwable t) {
                    l.onFlushFailed(duration, timeUnit, t);
                }
            };

    private final Action1<T> writeStartAction = new Action1<T>() {
        @Override
        public void call(T l) {
            l.onWriteStart();
        }
    };

    private final Action4<T, Long, TimeUnit, Long> writeSuccessAction = new Action4<T, Long, TimeUnit, Long>() {
        @Override
        public void call(T l, Long duration, TimeUnit timeUnit, Long bytesWritten) {
            l.onWriteSuccess(duration, timeUnit, bytesWritten);
        }
    };

    private final Action4<T, Long, TimeUnit, Throwable> writeFailedAction =
            new Action4<T, Long, TimeUnit, Throwable>() {
                @Override
                public void call(T l, Long duration, TimeUnit timeUnit, Throwable t) {
                    l.onWriteFailed(duration, timeUnit, t);
                }
            };

    private final Action1<T> closeStartAction = new Action1<T>() {
        @Override
        public void call(T l) {
            l.onConnectionCloseStart();
        }
    };

    private final Action3<T, Long, TimeUnit> closeSuccessAction = new Action3<T, Long, TimeUnit>() {
        @Override
        public void call(T l, Long duration, TimeUnit timeUnit) {
            l.onConnectionCloseSuccess(duration, timeUnit);
        }
    };

    private final Action4<T, Long, TimeUnit, Throwable> closeFailedAction =
            new Action4<T, Long, TimeUnit, Throwable>() {
                @Override
                public void call(T l, Long duration, TimeUnit timeUnit, Throwable t) {
                    l.onConnectionCloseFailed(duration, timeUnit, t);
                }
            };

    private final ListenersHolder<T> listeners;

    public ConnectionEventPublisher() {
        listeners = new ListenersHolder<>();
    }

    public ConnectionEventPublisher(ConnectionEventPublisher<T> toCopy) {
        listeners = toCopy.listeners.copy();
    }

    @Override
    public void onByteRead(final long bytesRead) {
        listeners.invokeListeners(bytesReadAction, bytesRead);
    }

    @Override
    public void onFlushStart() {
        listeners.invokeListeners(flushStartAction);
    }

    @Override
    public void onFlushSuccess(final long duration, final TimeUnit timeUnit) {
        listeners.invokeListeners(flushSuccessAction, duration, timeUnit);
    }

    @Override
    public void onFlushFailed(final long duration, final TimeUnit timeUnit, final Throwable throwable) {
        listeners.invokeListeners(flushFailedAction, duration, timeUnit, throwable);
    }

    @Override
    public void onWriteStart() {
        listeners.invokeListeners(writeStartAction);
    }

    @Override
    public void onWriteSuccess(final long duration, final TimeUnit timeUnit, final long bytesWritten) {
        listeners.invokeListeners(writeSuccessAction, duration, timeUnit, bytesWritten);
    }

    @Override
    public void onWriteFailed(final long duration, final TimeUnit timeUnit, final Throwable throwable) {
        listeners.invokeListeners(writeFailedAction, duration, timeUnit, throwable);
    }

    @Override
    public void onConnectionCloseStart() {
        listeners.invokeListeners(closeStartAction);
    }

    @Override
    public void onConnectionCloseSuccess(final long duration, final TimeUnit timeUnit) {
        listeners.invokeListeners(closeSuccessAction, duration, timeUnit);
    }

    @Override
    public void onConnectionCloseFailed(final long duration, final TimeUnit timeUnit, final Throwable throwable) {
        listeners.invokeListeners(closeFailedAction, duration, timeUnit, throwable);
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

    /*Visible for testing*/ ListenersHolder<T> getListeners() {
        return listeners;
    }
}
