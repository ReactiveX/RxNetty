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

import io.reactivex.netty.events.internal.SafeEventListener;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

final class SafeHttpClientEventsListener extends HttpClientEventsListener implements SafeEventListener {

    private final HttpClientEventsListener delegate;

    private final AtomicBoolean completed = new AtomicBoolean();

    public SafeHttpClientEventsListener(HttpClientEventsListener delegate) {
        this.delegate = delegate;
    }

    @Override
    public void onCompleted() {
        if (completed.compareAndSet(false, true)) {
            delegate.onCompleted();
        }
    }

    @Override
    public void onRequestSubmitted() {
        delegate.onRequestSubmitted();
    }

    @Override
    public void onRequestWriteStart() {
        delegate.onRequestWriteStart();
    }

    @Override
    public void onRequestWriteComplete(long duration, TimeUnit timeUnit) {
        delegate.onRequestWriteComplete(duration, timeUnit);
    }

    @Override
    public void onRequestWriteFailed(long duration, TimeUnit timeUnit,
                                     Throwable throwable) {
        delegate.onRequestWriteFailed(duration, timeUnit, throwable);
    }

    @Override
    public void onResponseHeadersReceived(int responseCode) {
        delegate.onResponseHeadersReceived(responseCode);
    }

    @Override
    public void onResponseContentReceived() {
        delegate.onResponseContentReceived();
    }

    @Override
    public void onResponseReceiveComplete(long duration, TimeUnit timeUnit) {
        delegate.onResponseReceiveComplete(duration, timeUnit);
    }

    @Override
    public void onResponseFailed(Throwable throwable) {
        delegate.onResponseFailed(throwable);
    }

    @Override
    public void onRequestProcessingComplete(long duration, TimeUnit timeUnit) {
        delegate.onRequestProcessingComplete(duration, timeUnit);
    }

    @Override
    public void onConnectStart() {
        delegate.onConnectStart();
    }

    @Override
    public void onConnectSuccess(long duration, TimeUnit timeUnit) {
        delegate.onConnectSuccess(duration, timeUnit);
    }

    @Override
    public void onConnectFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        delegate.onConnectFailed(duration, timeUnit, throwable);
    }

    @Override
    public void onPoolReleaseStart() {
        delegate.onPoolReleaseStart();
    }

    @Override
    public void onPoolReleaseSuccess(long duration, TimeUnit timeUnit) {
        delegate.onPoolReleaseSuccess(duration, timeUnit);
    }

    @Override
    public void onPoolReleaseFailed(long duration, TimeUnit timeUnit,
                                    Throwable throwable) {
        delegate.onPoolReleaseFailed(duration, timeUnit, throwable);
    }

    @Override
    public void onPooledConnectionEviction() {
        delegate.onPooledConnectionEviction();
    }

    @Override
    public void onPooledConnectionReuse() {
        delegate.onPooledConnectionReuse();
    }

    @Override
    public void onPoolAcquireStart() {
        delegate.onPoolAcquireStart();
    }

    @Override
    public void onPoolAcquireSuccess(long duration, TimeUnit timeUnit) {
        delegate.onPoolAcquireSuccess(duration, timeUnit);
    }

    @Override
    public void onPoolAcquireFailed(long duration, TimeUnit timeUnit,
                                    Throwable throwable) {
        delegate.onPoolAcquireFailed(duration, timeUnit, throwable);
    }

    @Override
    public void onByteRead(long bytesRead) {
        delegate.onByteRead(bytesRead);
    }

    @Override
    public void onFlushStart() {
        delegate.onFlushStart();
    }

    @Override
    public void onFlushSuccess(long duration, TimeUnit timeUnit) {
        delegate.onFlushSuccess(duration, timeUnit);
    }

    @Override
    public void onFlushFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        delegate.onFlushFailed(duration, timeUnit, throwable);
    }

    @Override
    public void onWriteStart() {
        delegate.onWriteStart();
    }

    @Override
    public void onWriteSuccess(long duration, TimeUnit timeUnit, long bytesWritten) {
        delegate.onWriteSuccess(duration, timeUnit, bytesWritten);
    }

    @Override
    public void onWriteFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        delegate.onWriteFailed(duration, timeUnit, throwable);
    }

    @Override
    public void onConnectionCloseStart() {
        delegate.onConnectionCloseStart();
    }

    @Override
    public void onConnectionCloseSuccess(long duration, TimeUnit timeUnit) {
        delegate.onConnectionCloseSuccess(duration, timeUnit);
    }

    @Override
    public void onConnectionCloseFailed(long duration, TimeUnit timeUnit,
                                        Throwable throwable) {
        delegate.onConnectionCloseFailed(duration, timeUnit, throwable);
    }
}
