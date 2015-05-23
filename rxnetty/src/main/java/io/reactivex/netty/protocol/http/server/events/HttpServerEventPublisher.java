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
package io.reactivex.netty.protocol.http.server.events;

import io.reactivex.netty.events.EventPublisher;
import io.reactivex.netty.events.EventSource;
import io.reactivex.netty.events.ListenersHolder;
import io.reactivex.netty.protocol.tcp.server.events.TcpServerEventPublisher;
import rx.Subscription;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

import java.util.concurrent.TimeUnit;

public final class HttpServerEventPublisher extends HttpServerEventsListener
        implements EventSource<HttpServerEventsListener>, EventPublisher {

    private final ListenersHolder<HttpServerEventsListener> listeners;
    private final TcpServerEventPublisher tcpDelegate;

    public HttpServerEventPublisher(TcpServerEventPublisher tcpDelegate) {
        listeners = new ListenersHolder<>();
        this.tcpDelegate = tcpDelegate;
    }

    public HttpServerEventPublisher(TcpServerEventPublisher tcpDelegate, ListenersHolder<HttpServerEventsListener> l) {
        this.tcpDelegate = tcpDelegate;
        listeners = l;
    }

    @Override
    public void onNewRequestReceived() {
        listeners.invokeListeners(new Action1<HttpServerEventsListener>() {
            @Override
            public void call(HttpServerEventsListener listener) {
                listener.onNewRequestReceived();
            }
        });
    }

    @Override
    public void onRequestHandlingStart(final long duration, final TimeUnit timeUnit) {
        listeners.invokeListeners(new Action1<HttpServerEventsListener>() {
            @Override
            public void call(HttpServerEventsListener listener) {
                listener.onRequestHandlingStart(duration, timeUnit);
            }
        });
    }

    @Override
    public void onRequestHandlingSuccess(final long duration, final TimeUnit timeUnit) {
        listeners.invokeListeners(new Action1<HttpServerEventsListener>() {
            @Override
            public void call(HttpServerEventsListener listener) {
                listener.onRequestHandlingSuccess(duration, timeUnit);
            }
        });
    }

    @Override
    public void onRequestHandlingFailed(final long duration, final TimeUnit timeUnit, final Throwable throwable) {
        listeners.invokeListeners(new Action1<HttpServerEventsListener>() {
            @Override
            public void call(HttpServerEventsListener listener) {
                listener.onRequestHandlingFailed(duration, timeUnit, throwable);
            }
        });
    }

    @Override
    public void onRequestHeadersReceived() {
        listeners.invokeListeners(new Action1<HttpServerEventsListener>() {
            @Override
            public void call(HttpServerEventsListener listener) {
                listener.onRequestHeadersReceived();
            }
        });
    }

    @Override
    public void onRequestContentReceived() {
        listeners.invokeListeners(new Action1<HttpServerEventsListener>() {
            @Override
            public void call(HttpServerEventsListener listener) {
                listener.onRequestContentReceived();
            }
        });
    }

    @Override
    public void onRequestReceiveComplete(final long duration, final TimeUnit timeUnit) {
        listeners.invokeListeners(new Action1<HttpServerEventsListener>() {
            @Override
            public void call(HttpServerEventsListener listener) {
                listener.onRequestReceiveComplete(duration, timeUnit);
            }
        });
    }

    @Override
    public void onResponseWriteStart() {
        listeners.invokeListeners(new Action1<HttpServerEventsListener>() {
            @Override
            public void call(HttpServerEventsListener listener) {
                listener.onResponseWriteStart();
            }
        });
    }

    @Override
    public void onResponseWriteSuccess(final long duration, final TimeUnit timeUnit, final int responseCode) {
        listeners.invokeListeners(new Action1<HttpServerEventsListener>() {
            @Override
            public void call(HttpServerEventsListener listener) {
                listener.onResponseWriteSuccess(duration, timeUnit, responseCode);
            }
        });
    }

    @Override
    public void onResponseWriteFailed(final long duration, final TimeUnit timeUnit, final Throwable throwable) {
        listeners.invokeListeners(new Action1<HttpServerEventsListener>() {
            @Override
            public void call(HttpServerEventsListener listener) {
                listener.onResponseWriteFailed(duration, timeUnit, throwable);
            }
        });
    }

    @Override
    public void onConnectionCloseFailed(long duration, TimeUnit timeUnit,
                                        Throwable throwable) {
        tcpDelegate.onConnectionCloseFailed(duration, timeUnit, throwable);
    }

    @Override
    public void onConnectionCloseSuccess(long duration, TimeUnit timeUnit) {
        tcpDelegate.onConnectionCloseSuccess(duration, timeUnit);
    }

    @Override
    public void onConnectionCloseStart() {
        tcpDelegate.onConnectionCloseStart();
    }

    @Override
    public void onWriteFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        tcpDelegate.onWriteFailed(duration, timeUnit, throwable);
    }

    @Override
    public void onWriteSuccess(long duration, TimeUnit timeUnit, long bytesWritten) {
        tcpDelegate.onWriteSuccess(duration, timeUnit, bytesWritten);
    }

    @Override
    public void onWriteStart() {
        tcpDelegate.onWriteStart();
    }

    @Override
    public void onFlushFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        tcpDelegate.onFlushFailed(duration, timeUnit, throwable);
    }

    @Override
    public void onFlushSuccess(long duration, TimeUnit timeUnit) {
        tcpDelegate.onFlushSuccess(duration, timeUnit);
    }

    @Override
    public void onFlushStart() {
        tcpDelegate.onFlushStart();
    }

    @Override
    public void onByteRead(long bytesRead) {
        tcpDelegate.onByteRead(bytesRead);
    }

    @Override
    public void onConnectionHandlingFailed(long duration, TimeUnit timeUnit,
                                           Throwable throwable) {
        tcpDelegate.onConnectionHandlingFailed(duration, timeUnit, throwable);
    }

    @Override
    public void onConnectionHandlingSuccess(long duration, TimeUnit timeUnit) {
        tcpDelegate.onConnectionHandlingSuccess(duration, timeUnit);
    }

    @Override
    public void onConnectionHandlingStart(long duration, TimeUnit timeUnit) {
        tcpDelegate.onConnectionHandlingStart(duration, timeUnit);
    }

    @Override
    public void onNewClientConnected() {
        tcpDelegate.onNewClientConnected();
    }

    @Override
    public boolean publishingEnabled() {
        return listeners.publishingEnabled();
    }

    @Override
    public Subscription subscribe(HttpServerEventsListener listener) {
        CompositeSubscription cs = new CompositeSubscription();
        cs.add(listeners.subscribe(listener));
        cs.add(tcpDelegate.subscribe(listener));
        return cs;
    }

    public HttpServerEventPublisher copy(TcpServerEventPublisher newTcpDelegate) {
        return new HttpServerEventPublisher(newTcpDelegate, listeners.copy());
    }
}
