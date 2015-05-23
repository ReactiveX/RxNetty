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
import io.reactivex.netty.protocol.tcp.client.events.TcpClientEventPublisher;
import rx.Subscription;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

import java.util.concurrent.TimeUnit;

public final class HttpClientEventPublisher extends HttpClientEventsListener
        implements EventSource<HttpClientEventsListener>, EventPublisher {

    private final ListenersHolder<HttpClientEventsListener> listeners;
    private final TcpClientEventPublisher tcpDelegate;

    public HttpClientEventPublisher(TcpClientEventPublisher tcpDelegate) {
        listeners = new ListenersHolder<>();
        this.tcpDelegate = tcpDelegate;
    }

    public HttpClientEventPublisher(ListenersHolder<HttpClientEventsListener> l, TcpClientEventPublisher tcpDelegate) {
        listeners = l;
        this.tcpDelegate = tcpDelegate;
    }

    @Override
    public void onRequestSubmitted() {
        listeners.invokeListeners(new Action1<HttpClientEventsListener>() {
            @Override
            public void call(HttpClientEventsListener listener) {
                listener.onRequestSubmitted();
            }
        });
    }

    @Override
    public void onRequestWriteStart() {
        listeners.invokeListeners(new Action1<HttpClientEventsListener>() {
            @Override
            public void call(HttpClientEventsListener listener) {
                listener.onRequestWriteStart();
            }
        });
    }

    @Override
    public void onRequestWriteComplete(final long duration, final TimeUnit timeUnit) {
        listeners.invokeListeners(new Action1<HttpClientEventsListener>() {
            @Override
            public void call(HttpClientEventsListener listener) {
                listener.onRequestWriteComplete(duration, timeUnit);
            }
        });
    }

    @Override
    public void onRequestWriteFailed(final long duration, final TimeUnit timeUnit, final Throwable throwable) {
        listeners.invokeListeners(new Action1<HttpClientEventsListener>() {
            @Override
            public void call(HttpClientEventsListener listener) {
                listener.onRequestWriteFailed(duration, timeUnit, throwable);
            }
        });
    }

    @Override
    public void onResponseHeadersReceived(final int responseCode) {
        listeners.invokeListeners(new Action1<HttpClientEventsListener>() {
            @Override
            public void call(HttpClientEventsListener listener) {
                listener.onResponseHeadersReceived(responseCode);
            }
        });
    }

    @Override
    public void onResponseContentReceived() {
        listeners.invokeListeners(new Action1<HttpClientEventsListener>() {
            @Override
            public void call(HttpClientEventsListener listener) {
                listener.onResponseContentReceived();
            }
        });
    }

    @Override
    public void onResponseReceiveComplete(final long duration, final TimeUnit timeUnit) {
        listeners.invokeListeners(new Action1<HttpClientEventsListener>() {
            @Override
            public void call(HttpClientEventsListener listener) {
                listener.onResponseReceiveComplete(duration, timeUnit);
            }
        });
    }

    @Override
    public void onResponseFailed(final Throwable throwable) {
        listeners.invokeListeners(new Action1<HttpClientEventsListener>() {
            @Override
            public void call(HttpClientEventsListener listener) {
                listener.onResponseFailed(throwable);
            }
        });
    }

    @Override
    public void onRequestProcessingComplete(final long duration, final TimeUnit timeUnit) {
        listeners.invokeListeners(new Action1<HttpClientEventsListener>() {
            @Override
            public void call(HttpClientEventsListener listener) {
                listener.onRequestProcessingComplete(duration, timeUnit);
            }
        });
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
    public boolean publishingEnabled() {
        return listeners.publishingEnabled();
    }

    @Override
    public Subscription subscribe(HttpClientEventsListener listener) {
        CompositeSubscription cs = new CompositeSubscription();
        cs.add(listeners.subscribe(listener));
        cs.add(tcpDelegate.subscribe(listener));
        return cs;
    }

    public HttpClientEventPublisher copy(TcpClientEventPublisher newTcpDelegate) {
        return new HttpClientEventPublisher(listeners.copy(), newTcpDelegate);
    }
}
