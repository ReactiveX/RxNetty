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
package io.reactivex.netty.protocol.tcp.client.events;

import io.reactivex.netty.channel.events.ConnectionEventPublisher;
import io.reactivex.netty.events.EventPublisher;
import io.reactivex.netty.events.EventSource;
import io.reactivex.netty.events.ListenersHolder;
import rx.Subscription;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

import java.util.concurrent.TimeUnit;

public final class TcpClientEventPublisher extends TcpClientEventListener
        implements EventSource<TcpClientEventListener>, EventPublisher {

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
        listeners.invokeListeners(new Action1<TcpClientEventListener>() {
            @Override
            public void call(TcpClientEventListener l) {
                l.onConnectStart();
            }
        });
    }

    @Override
    public void onConnectSuccess(final long duration, final TimeUnit timeUnit) {
        listeners.invokeListeners(new Action1<TcpClientEventListener>() {
            @Override
            public void call(TcpClientEventListener l) {
                l.onConnectSuccess(duration, timeUnit);
            }
        });    }

    @Override
    public void onConnectFailed(final long duration, final TimeUnit timeUnit, final Throwable throwable) {
        listeners.invokeListeners(new Action1<TcpClientEventListener>() {
            @Override
            public void call(TcpClientEventListener l) {
                l.onConnectFailed(duration, timeUnit, throwable);
            }
        });
    }

    @Override
    public void onPoolReleaseStart() {
        listeners.invokeListeners(new Action1<TcpClientEventListener>() {
            @Override
            public void call(TcpClientEventListener l) {
                l.onPoolReleaseStart();
            }
        });
    }

    @Override
    public void onPoolReleaseSuccess(final long duration, final TimeUnit timeUnit) {
        listeners.invokeListeners(new Action1<TcpClientEventListener>() {
            @Override
            public void call(TcpClientEventListener eventListener) {
                eventListener.onPoolReleaseSuccess(duration, timeUnit);

            }
        });
    }

    @Override
    public void onPoolReleaseFailed(final long duration, final TimeUnit timeUnit, final Throwable throwable) {
        listeners.invokeListeners(new Action1<TcpClientEventListener>() {
            @Override
            public void call(TcpClientEventListener eventListener) {
                eventListener.onPoolReleaseFailed(duration, timeUnit, throwable);

            }
        });
    }

    @Override
    public void onPooledConnectionEviction() {
        listeners.invokeListeners(new Action1<TcpClientEventListener>() {
            @Override
            public void call(TcpClientEventListener eventListener) {
                eventListener.onPooledConnectionEviction();

            }
        });
    }

    @Override
    public void onPooledConnectionReuse() {
        listeners.invokeListeners(new Action1<TcpClientEventListener>() {
            @Override
            public void call(TcpClientEventListener eventListener) {
                eventListener.onPooledConnectionReuse();

            }
        });
    }

    @Override
    public void onPoolAcquireStart() {
        listeners.invokeListeners(new Action1<TcpClientEventListener>() {
            @Override
            public void call(TcpClientEventListener eventListener) {
                eventListener.onPoolAcquireStart();

            }
        });
    }

    @Override
    public void onPoolAcquireSuccess(final long duration, final TimeUnit timeUnit) {
        listeners.invokeListeners(new Action1<TcpClientEventListener>() {
            @Override
            public void call(TcpClientEventListener eventListener) {
                eventListener.onPoolAcquireSuccess(duration, timeUnit);

            }
        });
    }

    @Override
    public void onPoolAcquireFailed(final long duration, final TimeUnit timeUnit, final Throwable throwable) {
        listeners.invokeListeners(new Action1<TcpClientEventListener>() {
            @Override
            public void call(TcpClientEventListener eventListener) {
                eventListener.onPoolAcquireFailed(duration, timeUnit, throwable);

            }
        });
    }

    @Override
    public void onByteRead(long bytesRead) {
        connDelegate.onByteRead(bytesRead);
    }

    @Override
    public void onFlushStart() {
        connDelegate.onFlushStart();
    }

    @Override
    public void onFlushSuccess(long duration, TimeUnit timeUnit) {
        connDelegate.onFlushSuccess(duration, timeUnit);
    }

    @Override
    public void onFlushFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        connDelegate.onFlushFailed(duration, timeUnit, throwable);
    }

    @Override
    public void onWriteStart() {
        connDelegate.onWriteStart();
    }

    @Override
    public void onWriteSuccess(long duration, TimeUnit timeUnit, long bytesWritten) {
        connDelegate.onWriteSuccess(duration, timeUnit, bytesWritten);
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
    public Subscription subscribe(TcpClientEventListener listener) {
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
}
