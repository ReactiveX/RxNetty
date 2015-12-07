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
package io.reactivex.netty.protocol.tcp.server.events;

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

public final class TcpServerEventPublisher extends TcpServerEventListener
        implements EventSource<TcpServerEventListener>, EventPublisher {

    private static final Action1<TcpServerEventListener> NEW_CLIENT_ACTION = new Action1<TcpServerEventListener>() {
        @Override
        public void call(TcpServerEventListener l) {
            l.onNewClientConnected();
        }
    };

    private static final Action3<TcpServerEventListener, Long, TimeUnit> HANDLE_START_ACTION =
            new Action3<TcpServerEventListener, Long, TimeUnit>() {
                @Override
                public void call(TcpServerEventListener l, Long duration, TimeUnit timeUnit) {
                    l.onConnectionHandlingStart(duration, timeUnit);
                }
            };

    private static final Action3<TcpServerEventListener, Long, TimeUnit> HANDLE_SUCCESS_ACTION =
            new Action3<TcpServerEventListener, Long, TimeUnit>() {
                @Override
                public void call(TcpServerEventListener l, Long duration, TimeUnit timeUnit) {
                    l.onConnectionHandlingSuccess(duration, timeUnit);
                }
            };

    private static final Action4<TcpServerEventListener, Long, TimeUnit, Throwable> HANDLE_FAILED_ACTION =
            new Action4<TcpServerEventListener, Long, TimeUnit, Throwable>() {
                @Override
                public void call(TcpServerEventListener l, Long duration, TimeUnit timeUnit, Throwable t) {
                    l.onConnectionHandlingFailed(duration, timeUnit, t);
                }
            };

    private final ListenersHolder<TcpServerEventListener> listeners;
    private final ConnectionEventPublisher<TcpServerEventListener> connDelegate;

    public TcpServerEventPublisher() {
        listeners = new ListenersHolder<>();
        connDelegate = new ConnectionEventPublisher<>();
    }

    private TcpServerEventPublisher(TcpServerEventPublisher toCopy) {
        listeners = toCopy.listeners.copy();
        connDelegate = toCopy.connDelegate.copy();
    }

    @Override
    public void onNewClientConnected() {
        listeners.invokeListeners(NEW_CLIENT_ACTION);
    }

    @Override
    public void onConnectionHandlingStart(final long duration, final TimeUnit timeUnit) {
        listeners.invokeListeners(HANDLE_START_ACTION, duration, timeUnit);
    }

    @Override
    public void onConnectionHandlingSuccess(final long duration, final TimeUnit timeUnit) {
        listeners.invokeListeners(HANDLE_SUCCESS_ACTION, duration, timeUnit);
    }

    @Override
    public void onConnectionHandlingFailed(final long duration, final TimeUnit timeUnit, final Throwable throwable) {
        listeners.invokeListeners(HANDLE_FAILED_ACTION, duration, timeUnit, throwable);
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
    public void onWriteFailed(long duration, TimeUnit timeUnit,
                              Throwable throwable) {
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
    public void onConnectionCloseFailed(long duration, TimeUnit timeUnit,
                                        Throwable throwable) {
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
    public boolean publishingEnabled() {
        return listeners.publishingEnabled();
    }

    @Override
    public Subscription subscribe(TcpServerEventListener listener) {
        if (!SafeEventListener.class.isAssignableFrom(listener.getClass())) {
            listener = new SafeTcpServerEventListener(listener);
        }

        CompositeSubscription cs = new CompositeSubscription();
        cs.add(listeners.subscribe(listener));
        cs.add(connDelegate.subscribe(listener));
        return cs;
    }

    public TcpServerEventPublisher copy() {
        return new TcpServerEventPublisher(this);
    }

    /*Visible for testing*/ListenersHolder<TcpServerEventListener> getListeners() {
        return listeners;
    }

    /*Visible for testing*/ConnectionEventPublisher<TcpServerEventListener> getConnDelegate() {
        return connDelegate;
    }
}
