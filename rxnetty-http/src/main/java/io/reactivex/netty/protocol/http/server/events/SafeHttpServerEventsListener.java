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
package io.reactivex.netty.protocol.http.server.events;

import io.reactivex.netty.events.internal.SafeEventListener;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

final class SafeHttpServerEventsListener extends HttpServerEventsListener implements SafeEventListener {

    private final AtomicBoolean completed = new AtomicBoolean();
    private final HttpServerEventsListener delegate;

    public SafeHttpServerEventsListener(HttpServerEventsListener delegate) {
        this.delegate = delegate;
    }

    @Override
    public void onCompleted() {
        if (completed.compareAndSet(false, true)) {
            delegate.onCompleted();
        }
    }

    @Override
    public void onNewRequestReceived() {
        if (!completed.get()) {
            delegate.onNewRequestReceived();
        }
    }

    @Override
    public void onRequestHandlingStart(long duration, TimeUnit timeUnit) {
        if (!completed.get()) {
            delegate.onRequestHandlingStart(duration, timeUnit);
        }
    }

    @Override
    public void onRequestHandlingSuccess(long duration, TimeUnit timeUnit) {
        if (!completed.get()) {
            delegate.onRequestHandlingSuccess(duration, timeUnit);
        }
    }

    @Override
    public void onRequestHandlingFailed(long duration, TimeUnit timeUnit,
                                        Throwable throwable) {
        if (!completed.get()) {
            delegate.onRequestHandlingFailed(duration, timeUnit, throwable);
        }
    }

    @Override
    public void onRequestHeadersReceived() {
        if (!completed.get()) {
            delegate.onRequestHeadersReceived();
        }
    }

    @Override
    public void onRequestContentReceived() {
        if (!completed.get()) {
            delegate.onRequestContentReceived();
        }
    }

    @Override
    public void onRequestReceiveComplete(long duration, TimeUnit timeUnit) {
        if (!completed.get()) {
            delegate.onRequestReceiveComplete(duration, timeUnit);
        }
    }

    @Override
    public void onResponseWriteStart() {
        if (!completed.get()) {
            delegate.onResponseWriteStart();
        }
    }

    @Override
    public void onResponseWriteSuccess(long duration, TimeUnit timeUnit, int responseCode) {
        if (!completed.get()) {
            delegate.onResponseWriteSuccess(duration, timeUnit, responseCode);
        }
    }

    @Override
    public void onResponseWriteFailed(long duration, TimeUnit timeUnit,
                                      Throwable throwable) {
        if (!completed.get()) {
            delegate.onResponseWriteFailed(duration, timeUnit, throwable);
        }
    }

    @Override
    public void onNewClientConnected() {
        if (!completed.get()) {
            delegate.onNewClientConnected();
        }
    }

    @Override
    public void onConnectionHandlingStart(long duration, TimeUnit timeUnit) {
        if (!completed.get()) {
            delegate.onConnectionHandlingStart(duration, timeUnit);
        }
    }

    @Override
    public void onConnectionHandlingSuccess(long duration, TimeUnit timeUnit) {
        if (!completed.get()) {
            delegate.onConnectionHandlingSuccess(duration, timeUnit);
        }
    }

    @Override
    public void onConnectionHandlingFailed(long duration, TimeUnit timeUnit,
                                           Throwable throwable) {
        if (!completed.get()) {
            delegate.onConnectionHandlingFailed(duration, timeUnit, throwable);
        }
    }

    @Override
    public void onByteRead(long bytesRead) {
        if (!completed.get()) {
            delegate.onByteRead(bytesRead);
        }
    }

    @Override
    public void onByteWritten(long bytesWritten) {
        if (!completed.get()) {
            delegate.onByteWritten(bytesWritten);
        }
    }

    @Override
    public void onFlushStart() {
        if (!completed.get()) {
            delegate.onFlushStart();
        }
    }

    @Override
    public void onFlushComplete(long duration, TimeUnit timeUnit) {
        if (!completed.get()) {
            delegate.onFlushComplete(duration, timeUnit);
        }
    }

    @Override
    public void onWriteStart() {
        if (!completed.get()) {
            delegate.onWriteStart();
        }
    }

    @Override
    public void onWriteSuccess(long duration, TimeUnit timeUnit) {
        if (!completed.get()) {
            delegate.onWriteSuccess(duration, timeUnit);
        }
    }

    @Override
    public void onWriteFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        if (!completed.get()) {
            delegate.onWriteFailed(duration, timeUnit, throwable);
        }
    }

    @Override
    public void onConnectionCloseStart() {
        if (!completed.get()) {
            delegate.onConnectionCloseStart();
        }
    }

    @Override
    public void onConnectionCloseSuccess(long duration, TimeUnit timeUnit) {
        if (!completed.get()) {
            delegate.onConnectionCloseSuccess(duration, timeUnit);
        }
    }

    @Override
    public void onConnectionCloseFailed(long duration, TimeUnit timeUnit,
                                        Throwable throwable) {
        if (!completed.get()) {
            delegate.onConnectionCloseFailed(duration, timeUnit, throwable);
        }
    }

    @Override
    public void onCustomEvent(Object event) {
        if (!completed.get()) {
            delegate.onCustomEvent(event);
        }
    }

    @Override
    public void onCustomEvent(Object event, long duration, TimeUnit timeUnit) {
        if (!completed.get()) {
            delegate.onCustomEvent(event, duration, timeUnit);
        }
    }

    @Override
    public void onCustomEvent(Object event, long duration, TimeUnit timeUnit, Throwable throwable) {
        if (!completed.get()) {
            delegate.onCustomEvent(event, duration, timeUnit, throwable);
        }
    }

    @Override
    public void onCustomEvent(Object event, Throwable throwable) {
        if (!completed.get()) {
            delegate.onCustomEvent(event, throwable);
        }
    }

    public HttpServerEventsListener unwrap() {
        return delegate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SafeHttpServerEventsListener)) {
            return false;
        }

        SafeHttpServerEventsListener that = (SafeHttpServerEventsListener) o;

        return !(delegate != null? !delegate.equals(that.delegate) : that.delegate != null);

    }

    @Override
    public int hashCode() {
        return delegate != null? delegate.hashCode() : 0;
    }
}
