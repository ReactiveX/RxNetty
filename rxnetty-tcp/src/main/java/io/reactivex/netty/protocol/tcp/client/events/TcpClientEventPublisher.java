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
package io.reactivex.netty.protocol.tcp.client.events;

import io.reactivex.netty.channel.events.ConnectionEventPublisher;
import io.reactivex.netty.events.EventPublisher;
import io.reactivex.netty.events.EventSource;
import io.reactivex.netty.events.ListenersHolder;
import io.reactivex.netty.events.internal.SafeEventListener;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Action3;
import rx.functions.Action4;
import rx.subscriptions.CompositeSubscription;

import java.util.concurrent.TimeUnit;

public final class TcpClientEventPublisher extends TcpClientEventListener
        implements EventSource<TcpClientEventListener>, EventPublisher {

    public static final Action1<TcpClientEventListener> CONN_START_ACTION = new Action1<TcpClientEventListener>() {
        @Override
        public void call(TcpClientEventListener l) {
            l.onConnectStart();
        }
    };

    public static final Action3<TcpClientEventListener, Long, TimeUnit> CONN_SUCCESS_ACTION =
            new Action3<TcpClientEventListener, Long, TimeUnit>() {
                @Override
                public void call(TcpClientEventListener l, Long duration, TimeUnit timeUnit) {
                    l.onConnectSuccess(duration, timeUnit);
                }
            };

    public static final Action4<TcpClientEventListener, Long, TimeUnit, Throwable> CONN_FAILED_ACTION =
            new Action4<TcpClientEventListener, Long, TimeUnit, Throwable>() {
                @Override
                public void call(TcpClientEventListener l, Long duration, TimeUnit timeUnit, Throwable t) {
                    l.onConnectFailed(duration, timeUnit, t);
                }
            };

    public static final Action1<TcpClientEventListener> EVICTION_ACTION = new Action1<TcpClientEventListener>() {
        @Override
        public void call(TcpClientEventListener l) {
            l.onPooledConnectionEviction();
        }
    };

    public static final Action1<TcpClientEventListener> REUSE_ACTION = new Action1<TcpClientEventListener>() {
        @Override
        public void call(TcpClientEventListener l) {
            l.onPooledConnectionReuse();
        }
    };

    public static final Action1<TcpClientEventListener> ACQUIRE_START_ACTION = new Action1<TcpClientEventListener>() {
        @Override
        public void call(TcpClientEventListener l) {
            l.onPoolAcquireStart();
        }
    };

    public static final Action3<TcpClientEventListener, Long, TimeUnit> ACQUIRE_SUCCESS_ACTION =
            new Action3<TcpClientEventListener, Long, TimeUnit>() {
                @Override
                public void call(TcpClientEventListener l, Long duration, TimeUnit timeUnit) {
                    l.onPoolAcquireSuccess(duration, timeUnit);
                }
            };

    public static final Action4<TcpClientEventListener, Long, TimeUnit, Throwable> ACQUIRE_FAILED_ACTION =
            new Action4<TcpClientEventListener, Long, TimeUnit, Throwable>() {
                @Override
                public void call(TcpClientEventListener l, Long duration, TimeUnit timeUnit, Throwable t) {
                    l.onPoolAcquireFailed(duration, timeUnit, t);
                }
            };

    public static final Action1<TcpClientEventListener> RELEASE_START_ACTION = new Action1<TcpClientEventListener>() {
        @Override
        public void call(TcpClientEventListener l) {
            l.onPoolReleaseStart();
        }
    };

    public static final Action3<TcpClientEventListener, Long, TimeUnit> RELEASE_SUCCESS_ACTION =
            new Action3<TcpClientEventListener, Long, TimeUnit>() {
                @Override
                public void call(TcpClientEventListener l, Long duration, TimeUnit timeUnit) {
                    l.onPoolReleaseSuccess(duration, timeUnit);
                }
            };

    public static final Action4<TcpClientEventListener, Long, TimeUnit, Throwable> RELEASE_FAILED_ACTION =
            new Action4<TcpClientEventListener, Long, TimeUnit, Throwable>() {
                @Override
                public void call(TcpClientEventListener l, Long duration, TimeUnit timeUnit, Throwable t) {
                    l.onPoolReleaseFailed(duration, timeUnit, t);
                }
            };

    private final ListenersHolder<TcpClientEventListener> listeners;
    private final ConnectionEventPublisher<TcpClientEventListener> connDelegate;

    public TcpClientEventPublisher() {
        listeners = new ListenersHolder<>();
        connDelegate = new ConnectionEventPublisher<>();
    }

    public TcpClientEventPublisher(TcpClientEventPublisher toCopy) {
        listeners = toCopy.listeners.copy();
        connDelegate = toCopy.connDelegate.copy();
    }

    @Override
    public void onConnectStart() {
        listeners.invokeListeners(CONN_START_ACTION);
    }

    @Override
    public void onConnectSuccess(final long duration, final TimeUnit timeUnit) {
        listeners.invokeListeners(CONN_SUCCESS_ACTION, duration, timeUnit);
    }

    @Override
    public void onConnectFailed(final long duration, final TimeUnit timeUnit, final Throwable throwable) {
        listeners.invokeListeners(CONN_FAILED_ACTION, duration, timeUnit, throwable);
    }

    @Override
    public void onPoolReleaseStart() {
        listeners.invokeListeners(RELEASE_START_ACTION);
    }

    @Override
    public void onPoolReleaseSuccess(final long duration, final TimeUnit timeUnit) {
        listeners.invokeListeners(RELEASE_SUCCESS_ACTION, duration, timeUnit);
    }

    @Override
    public void onPoolReleaseFailed(final long duration, final TimeUnit timeUnit, final Throwable throwable) {
        listeners.invokeListeners(RELEASE_FAILED_ACTION, duration, timeUnit, throwable);
    }

    @Override
    public void onPooledConnectionEviction() {
        listeners.invokeListeners(EVICTION_ACTION);
    }

    @Override
    public void onPooledConnectionReuse() {
        listeners.invokeListeners(REUSE_ACTION);
    }

    @Override
    public void onPoolAcquireStart() {
        listeners.invokeListeners(ACQUIRE_START_ACTION);
    }

    @Override
    public void onPoolAcquireSuccess(final long duration, final TimeUnit timeUnit) {
        listeners.invokeListeners(ACQUIRE_SUCCESS_ACTION, duration, timeUnit);
    }

    @Override
    public void onPoolAcquireFailed(final long duration, final TimeUnit timeUnit, final Throwable throwable) {
        listeners.invokeListeners(ACQUIRE_FAILED_ACTION, duration, timeUnit, throwable);
    }

    @Override
    public void onByteRead(long bytesRead) {
        connDelegate.onByteRead(bytesRead);
    }

    @Override
    public void onByteWritten(long bytesWritten) {
        connDelegate.onByteWritten(bytesWritten);
    }

    @Override
    public void onFlushStart() {
        connDelegate.onFlushStart();
    }

    @Override
    public void onFlushComplete(long duration, TimeUnit timeUnit) {
        connDelegate.onFlushComplete(duration, timeUnit);
    }

    @Override
    public void onWriteStart() {
        connDelegate.onWriteStart();
    }

    @Override
    public void onWriteSuccess(long duration, TimeUnit timeUnit) {
        connDelegate.onWriteSuccess(duration, timeUnit);
    }

    @Override
    public void onWriteFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        connDelegate.onWriteFailed(duration, timeUnit, throwable);
    }

    @Override
    public void onConnectionCloseStart() {
        connDelegate.onConnectionCloseStart();
    }

    @Override
    public void onConnectionCloseSuccess(long duration, TimeUnit timeUnit) {
        connDelegate.onConnectionCloseSuccess(duration, timeUnit);
    }

    @Override
    public void onConnectionCloseFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        connDelegate.onConnectionCloseFailed(duration, timeUnit, throwable);
    }

    @Override
    public void onCustomEvent(Object event) {
        connDelegate.onCustomEvent(event);
    }

    @Override
    public void onCustomEvent(Object event, long duration, TimeUnit timeUnit) {
        connDelegate.onCustomEvent(event, duration, timeUnit);
    }

    @Override
    public void onCustomEvent(Object event, long duration, TimeUnit timeUnit, Throwable throwable) {
        connDelegate.onCustomEvent(event, duration, timeUnit, throwable);
    }

    @Override
    public void onCustomEvent(Object event, Throwable throwable) {
        connDelegate.onCustomEvent(event, throwable);
    }

    @Override
    public Subscription subscribe(TcpClientEventListener listener) {
        if (!SafeEventListener.class.isAssignableFrom(listener.getClass())) {
            listener = new SafeTcpClientEventListener(listener);
        }

        CompositeSubscription cs = new CompositeSubscription();
        cs.add(listeners.subscribe(listener));
        cs.add(connDelegate.subscribe(listener));
        return cs;
    }

    @Override
    public boolean publishingEnabled() {
        return listeners.publishingEnabled();
    }

    public TcpClientEventPublisher copy() {
        return new TcpClientEventPublisher(this);
    }

    /*Visible for testing*/ ListenersHolder<TcpClientEventListener> getListeners() {
        return listeners;
    }
}
