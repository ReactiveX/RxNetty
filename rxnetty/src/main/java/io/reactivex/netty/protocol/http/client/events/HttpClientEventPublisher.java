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
package io.reactivex.netty.protocol.http.client.events;

import io.reactivex.netty.events.EventPublisher;
import io.reactivex.netty.events.EventSource;
import io.reactivex.netty.events.ListenersHolder;
import io.reactivex.netty.events.internal.SafeEventListener;
import io.reactivex.netty.protocol.tcp.client.events.TcpClientEventListener;
import io.reactivex.netty.protocol.tcp.client.events.TcpClientEventPublisher;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Action2;
import rx.functions.Action3;
import rx.functions.Action4;
import rx.subscriptions.CompositeSubscription;

import java.util.concurrent.TimeUnit;

public final class HttpClientEventPublisher extends HttpClientEventsListener
        implements EventSource<HttpClientEventsListener>, EventPublisher {

    private static final Action1<HttpClientEventsListener> REQUEST_SUBMIT_ACTION =
            new Action1<HttpClientEventsListener>() {
                @Override
                public void call(HttpClientEventsListener listener) {
                    listener.onRequestSubmitted();
                }
            };

    private static final Action1<HttpClientEventsListener> REQUEST_WRITE_START_ACTION =
            new Action1<HttpClientEventsListener>() {
                @Override
                public void call(HttpClientEventsListener listener) {
                    listener.onRequestWriteStart();
                }
            };

    private static final Action3<HttpClientEventsListener, Long, TimeUnit> REQUEST_WRITE_COMPLETE_ACTION =
            new Action3<HttpClientEventsListener, Long, TimeUnit>() {
                @Override
                public void call(HttpClientEventsListener listener, Long duration, TimeUnit timeUnit) {
                    listener.onRequestWriteComplete(duration, timeUnit);
                }
            };

    private static final Action4<HttpClientEventsListener, Long, TimeUnit, Throwable> REQUEST_WRITE_FAILED_ACTION =
            new Action4<HttpClientEventsListener, Long, TimeUnit, Throwable>() {
                @Override
                public void call(HttpClientEventsListener listener, Long duration, TimeUnit timeUnit, Throwable t) {
                    listener.onRequestWriteFailed(duration, timeUnit, t);
                }
            };

    private static final Action4<HttpClientEventsListener, Long, TimeUnit, Integer> RESP_HEADER_RECIEVED_ACTION =
            new Action4<HttpClientEventsListener, Long, TimeUnit, Integer>() {
                @Override
                public void call(HttpClientEventsListener listener, Long duration, TimeUnit timeUnit,
                                 Integer responseCode) {
                    listener.onResponseHeadersReceived(responseCode, duration, timeUnit);
                }
            };

    private static final Action1<HttpClientEventsListener> RESP_CONTENT_RECIEVED_ACTION =
            new Action1<HttpClientEventsListener>() {
                @Override
                public void call(HttpClientEventsListener listener) {
                    listener.onResponseContentReceived();
                }
            };

    private static final Action3<HttpClientEventsListener, Long, TimeUnit> RESP_RECIEVE_COMPLETE_ACTION =
            new Action3<HttpClientEventsListener, Long, TimeUnit>() {
                @Override
                public void call(HttpClientEventsListener listener, Long duration, TimeUnit timeUnit) {
                    listener.onResponseReceiveComplete(duration, timeUnit);
                }
            };

    private static final Action2<HttpClientEventsListener, Throwable> RESP_FAILED_ACTION =
            new Action2<HttpClientEventsListener, Throwable>() {
                @Override
                public void call(HttpClientEventsListener listener, Throwable t) {
                    listener.onResponseFailed(t);
                }
            };

    private static final Action3<HttpClientEventsListener, Long, TimeUnit> PROCESSING_COMPLETE_ACTION =
            new Action3<HttpClientEventsListener, Long, TimeUnit>() {
                @Override
                public void call(HttpClientEventsListener listener, Long duration, TimeUnit timeUnit) {
                    listener.onRequestProcessingComplete(duration, timeUnit);
                }
            };

    private final ListenersHolder<HttpClientEventsListener> listeners;
    private final TcpClientEventPublisher tcpDelegate;

    public HttpClientEventPublisher() {
        listeners = new ListenersHolder<>();
        tcpDelegate = new TcpClientEventPublisher();
    }

    private HttpClientEventPublisher(ListenersHolder<HttpClientEventsListener> l, TcpClientEventPublisher tcpDelegate) {
        listeners = new ListenersHolder<>(l);
        this.tcpDelegate = tcpDelegate;
    }

    @Override
    public void onRequestSubmitted() {
        listeners.invokeListeners(REQUEST_SUBMIT_ACTION);
    }

    @Override
    public void onRequestWriteStart() {
        listeners.invokeListeners(REQUEST_WRITE_START_ACTION);
    }

    @Override
    public void onRequestWriteComplete(final long duration, final TimeUnit timeUnit) {
        listeners.invokeListeners(REQUEST_WRITE_COMPLETE_ACTION, duration, timeUnit);
    }

    @Override
    public void onRequestWriteFailed(final long duration, final TimeUnit timeUnit, final Throwable throwable) {
        listeners.invokeListeners(REQUEST_WRITE_FAILED_ACTION, duration, timeUnit, throwable);
    }

    @Override
    public void onResponseHeadersReceived(final int responseCode, long duration, TimeUnit timeUnit) {
        listeners.invokeListeners(RESP_HEADER_RECIEVED_ACTION, duration, timeUnit, responseCode);
    }

    @Override
    public void onResponseContentReceived() {
        listeners.invokeListeners(RESP_CONTENT_RECIEVED_ACTION);
    }

    @Override
    public void onResponseReceiveComplete(final long duration, final TimeUnit timeUnit) {
        listeners.invokeListeners(RESP_RECIEVE_COMPLETE_ACTION, duration, timeUnit);
    }

    @Override
    public void onResponseFailed(final Throwable throwable) {
        listeners.invokeListeners(RESP_FAILED_ACTION, throwable);
    }

    @Override
    public void onRequestProcessingComplete(final long duration, final TimeUnit timeUnit) {
        listeners.invokeListeners(PROCESSING_COMPLETE_ACTION, duration, timeUnit);
    }

    @Override
    public void onConnectionCloseFailed(long duration, TimeUnit timeUnit,
                                        Throwable throwable) {
        tcpDelegate.onConnectionCloseFailed(duration, timeUnit, throwable);
    }

    @Override
    public void onConnectStart() {
        tcpDelegate.onConnectStart();
    }

    @Override
    public void onConnectSuccess(long duration, TimeUnit timeUnit) {
        tcpDelegate.onConnectSuccess(duration, timeUnit);
    }

    @Override
    public void onConnectFailed(long duration, TimeUnit timeUnit,
                                Throwable throwable) {
        tcpDelegate.onConnectFailed(duration, timeUnit, throwable);
    }

    @Override
    public void onPoolReleaseStart() {
        tcpDelegate.onPoolReleaseStart();
    }

    @Override
    public void onPoolReleaseSuccess(long duration, TimeUnit timeUnit) {
        tcpDelegate.onPoolReleaseSuccess(duration, timeUnit);
    }

    @Override
    public void onPoolReleaseFailed(long duration, TimeUnit timeUnit,
                                    Throwable throwable) {
        tcpDelegate.onPoolReleaseFailed(duration, timeUnit, throwable);
    }

    @Override
    public void onPooledConnectionEviction() {
        tcpDelegate.onPooledConnectionEviction();
    }

    @Override
    public void onPooledConnectionReuse() {
        tcpDelegate.onPooledConnectionReuse();
    }

    @Override
    public void onPoolAcquireStart() {
        tcpDelegate.onPoolAcquireStart();
    }

    @Override
    public void onPoolAcquireSuccess(long duration, TimeUnit timeUnit) {
        tcpDelegate.onPoolAcquireSuccess(duration, timeUnit);
    }

    @Override
    public void onPoolAcquireFailed(long duration, TimeUnit timeUnit,
                                    Throwable throwable) {
        tcpDelegate.onPoolAcquireFailed(duration, timeUnit, throwable);
    }

    @Override
    public void onByteRead(long bytesRead) {
        tcpDelegate.onByteRead(bytesRead);
    }

    @Override
    public void onFlushStart() {
        tcpDelegate.onFlushStart();
    }

    @Override
    public void onFlushSuccess(long duration, TimeUnit timeUnit) {
        tcpDelegate.onFlushSuccess(duration, timeUnit);
    }

    @Override
    public void onFlushFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        tcpDelegate.onFlushFailed(duration, timeUnit, throwable);
    }

    @Override
    public void onWriteStart() {
        tcpDelegate.onWriteStart();
    }

    @Override
    public void onWriteSuccess(long duration, TimeUnit timeUnit, long bytesWritten) {
        tcpDelegate.onWriteSuccess(duration, timeUnit, bytesWritten);
    }

    @Override
    public void onWriteFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        tcpDelegate.onWriteFailed(duration, timeUnit, throwable);
    }

    @Override
    public void onConnectionCloseStart() {
        tcpDelegate.onConnectionCloseStart();
    }

    @Override
    public void onConnectionCloseSuccess(long duration, TimeUnit timeUnit) {
        tcpDelegate.onConnectionCloseSuccess(duration, timeUnit);
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
    public boolean publishingEnabled() {
        return listeners.publishingEnabled();
    }

    @Override
    public Subscription subscribe(HttpClientEventsListener listener) {
        if (!SafeEventListener.class.isAssignableFrom(listener.getClass())) {
            listener = new SafeHttpClientEventsListener(listener);
        }

        CompositeSubscription cs = new CompositeSubscription();
        cs.add(listeners.subscribe(listener));
        cs.add(tcpDelegate.subscribe(listener));
        return cs;
    }

    public EventSource<TcpClientEventListener> asTcpEventSource() {
        return new EventSource<TcpClientEventListener>() {
            @Override
            public Subscription subscribe(TcpClientEventListener listener) {
                if (listener instanceof HttpClientEventsListener) {
                    return HttpClientEventPublisher.this.subscribe((HttpClientEventsListener) listener);
                }
                return tcpDelegate.subscribe(listener);
            }
        };
    }

    public HttpClientEventPublisher copy() {
        return new HttpClientEventPublisher(listeners.copy(), tcpDelegate.copy());
    }

    /*Visible for testing*/ListenersHolder<HttpClientEventsListener> getListeners() {
        return listeners;
    }

    /*Visible for testing*/TcpClientEventListener getTcpDelegate() {
        return tcpDelegate;
    }
}
