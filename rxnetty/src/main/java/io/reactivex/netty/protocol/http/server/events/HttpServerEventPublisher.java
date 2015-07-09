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
import io.reactivex.netty.events.internal.SafeEventListener;
import io.reactivex.netty.protocol.tcp.server.events.TcpServerEventPublisher;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Action3;
import rx.functions.Action4;
import rx.subscriptions.CompositeSubscription;

import java.util.concurrent.TimeUnit;

public final class HttpServerEventPublisher extends HttpServerEventsListener
        implements EventSource<HttpServerEventsListener>, EventPublisher {

    private static final Action1<HttpServerEventsListener> NEW_REQUEST_ACTION = new Action1<HttpServerEventsListener>() {
        @Override
        public void call(HttpServerEventsListener l) {
            l.onNewRequestReceived();
        }
    };

    private static final Action3<HttpServerEventsListener, Long, TimeUnit> HANDLE_START_ACTION =
            new Action3<HttpServerEventsListener, Long, TimeUnit>() {
                @Override
                public void call(HttpServerEventsListener l, Long duration, TimeUnit timeUnit) {
                    l.onRequestHandlingStart(duration, timeUnit);
                }
            };

    private static final Action3<HttpServerEventsListener, Long, TimeUnit> HANDLE_SUCCESS_ACTION =
            new Action3<HttpServerEventsListener, Long, TimeUnit>() {
                @Override
                public void call(HttpServerEventsListener l, Long duration, TimeUnit timeUnit) {
                    l.onRequestHandlingSuccess(duration, timeUnit);
                }
            };

    private static final Action4<HttpServerEventsListener, Long, TimeUnit, Throwable> HANDLE_FAILED_ACTION =
            new Action4<HttpServerEventsListener, Long, TimeUnit, Throwable>() {
                @Override
                public void call(HttpServerEventsListener l, Long duration, TimeUnit timeUnit, Throwable t) {
                    l.onRequestHandlingFailed(duration, timeUnit, t);
                }
            };

    private static final Action1<HttpServerEventsListener> HEADER_RECIEVED_ACTION = new Action1<HttpServerEventsListener>() {
        @Override
        public void call(HttpServerEventsListener l) {
            l.onRequestHeadersReceived();
        }
    };

    private static final Action1<HttpServerEventsListener> CONTENT_RECIEVED_ACTION = new Action1<HttpServerEventsListener>() {
        @Override
        public void call(HttpServerEventsListener l) {
            l.onRequestContentReceived();
        }
    };

    private static final Action3<HttpServerEventsListener, Long, TimeUnit> REQ_RECV_COMPLETE_ACTION =
            new Action3<HttpServerEventsListener, Long, TimeUnit>() {
                @Override
                public void call(HttpServerEventsListener l, Long duration, TimeUnit timeUnit) {
                    l.onRequestReceiveComplete(duration, timeUnit);
                }
            };

    private static final Action1<HttpServerEventsListener> RESP_WRITE_START_ACTION =
            new Action1<HttpServerEventsListener>() {
                @Override
                public void call(HttpServerEventsListener l) {
                    l.onResponseWriteStart();
                }
            };

    private static final Action4<HttpServerEventsListener, Long, TimeUnit, Integer> RESP_WRITE_SUCCESS_ACTION =
            new Action4<HttpServerEventsListener, Long, TimeUnit, Integer>() {
                @Override
                public void call(HttpServerEventsListener l, Long duration, TimeUnit timeUnit, Integer respCode) {
                    l.onResponseWriteSuccess(duration, timeUnit, respCode);
                }
            };

    private static final Action4<HttpServerEventsListener, Long, TimeUnit, Throwable> RESP_WRITE_FAILED_ACTION =
            new Action4<HttpServerEventsListener, Long, TimeUnit, Throwable>() {
                @Override
                public void call(HttpServerEventsListener l, Long duration, TimeUnit timeUnit, Throwable t) {
                    l.onResponseWriteFailed(duration, timeUnit, t);
                }
            };

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
        listeners.invokeListeners(NEW_REQUEST_ACTION);
    }

    @Override
    public void onRequestHandlingStart(final long duration, final TimeUnit timeUnit) {
        listeners.invokeListeners(HANDLE_START_ACTION, duration, timeUnit);
    }

    @Override
    public void onRequestHandlingSuccess(final long duration, final TimeUnit timeUnit) {
        listeners.invokeListeners(HANDLE_SUCCESS_ACTION, duration, timeUnit);
    }

    @Override
    public void onRequestHandlingFailed(final long duration, final TimeUnit timeUnit, final Throwable throwable) {
        listeners.invokeListeners(HANDLE_FAILED_ACTION, duration, timeUnit, throwable);
    }

    @Override
    public void onRequestHeadersReceived() {
        listeners.invokeListeners(HEADER_RECIEVED_ACTION);
    }

    @Override
    public void onRequestContentReceived() {
        listeners.invokeListeners(CONTENT_RECIEVED_ACTION);
    }

    @Override
    public void onRequestReceiveComplete(final long duration, final TimeUnit timeUnit) {
        listeners.invokeListeners(REQ_RECV_COMPLETE_ACTION, duration, timeUnit);
    }

    @Override
    public void onResponseWriteStart() {
        listeners.invokeListeners(RESP_WRITE_START_ACTION);
    }

    @Override
    public void onResponseWriteSuccess(final long duration, final TimeUnit timeUnit, final int responseCode) {
        listeners.invokeListeners(RESP_WRITE_SUCCESS_ACTION, duration, timeUnit, responseCode);
    }

    @Override
    public void onResponseWriteFailed(final long duration, final TimeUnit timeUnit, final Throwable throwable) {
        listeners.invokeListeners(RESP_WRITE_FAILED_ACTION, duration, timeUnit, throwable);
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
    public void onCustomEvent(Object event) {
        tcpDelegate.onCustomEvent(event);
    }

    @Override
    public void onCustomEvent(Object event, long duration, TimeUnit timeUnit) {
        tcpDelegate.onCustomEvent(event, duration, timeUnit);
    }

    @Override
    public void onCustomEvent(Object event, long duration, TimeUnit timeUnit, Throwable throwable) {
        tcpDelegate.onCustomEvent(event, duration, timeUnit, throwable);
    }

    @Override
    public void onCustomEvent(Object event, Throwable throwable) {
        tcpDelegate.onCustomEvent(event, throwable);
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
        if (!SafeEventListener.class.isAssignableFrom(listener.getClass())) {
            listener = new SafeHttpServerEventsListener(listener);
        }

        CompositeSubscription cs = new CompositeSubscription();
        cs.add(listeners.subscribe(listener));
        cs.add(tcpDelegate.subscribe(listener));
        return cs;
    }

    public HttpServerEventPublisher copy(TcpServerEventPublisher newTcpDelegate) {
        return new HttpServerEventPublisher(newTcpDelegate, listeners.copy());
    }

    /*Visible for testing*/ListenersHolder<HttpServerEventsListener> getListeners() {
        return listeners;
    }

    /*Visible for testing*/TcpServerEventPublisher getTcpDelegate() {
        return tcpDelegate;
    }
}
