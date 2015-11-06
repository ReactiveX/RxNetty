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
package io.reactivex.netty.test.util;

import io.reactivex.netty.client.events.ClientEventListener;
import io.reactivex.netty.test.util.MockConnectionEventListener.Event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MockClientEventListener extends ClientEventListener {

    public enum ClientEvent {
        ConnectStart, ConnectSuccess, ConnectFailed, ReleaseStart, ReleaseSuccess, ReleaseFailed, Eviction, Reuse,
        AcquireStart, AcquireSuccess, AcquireFailed
    }

    private final List<ClientEvent> methodsCalled = new ArrayList<>();
    private long duration;
    private TimeUnit timeUnit;
    private Throwable recievedError;

    private final MockConnectionEventListener delegate = new MockConnectionEventListener();

    @Override
    public void onConnectStart() {
        methodsCalled.add(ClientEvent.ConnectStart);
    }

    @Override
    public void onConnectSuccess(long duration, TimeUnit timeUnit) {
        this.duration = duration;
        this.timeUnit = timeUnit;
        methodsCalled.add(ClientEvent.ConnectSuccess);
    }

    @Override
    public void onConnectFailed(long duration, TimeUnit timeUnit, Throwable recievedError) {
        methodsCalled.add(ClientEvent.ConnectFailed);
        this.duration = duration;
        this.timeUnit = timeUnit;
        this.recievedError = recievedError;
    }

    @Override
    public void onPoolReleaseStart() {
        methodsCalled.add(ClientEvent.ReleaseStart);
    }

    @Override
    public void onPoolReleaseSuccess(long duration, TimeUnit timeUnit) {
        methodsCalled.add(ClientEvent.ReleaseSuccess);
        this.duration = duration;
        this.timeUnit = timeUnit;
    }

    @Override
    public void onPoolReleaseFailed(long duration, TimeUnit timeUnit, Throwable recievedError) {
        methodsCalled.add(ClientEvent.ReleaseFailed);
        this.duration = duration;
        this.timeUnit = timeUnit;
        this.recievedError = recievedError;
    }

    @Override
    public void onPooledConnectionEviction() {
        methodsCalled.add(ClientEvent.Eviction);
    }

    @Override
    public void onPooledConnectionReuse() {
        methodsCalled.add(ClientEvent.Reuse);
    }

    @Override
    public void onPoolAcquireStart() {
        methodsCalled.add(ClientEvent.AcquireStart);
    }

    @Override
    public void onPoolAcquireSuccess(long duration, TimeUnit timeUnit) {
        methodsCalled.add(ClientEvent.AcquireSuccess);
        this.duration = duration;
        this.timeUnit = timeUnit;
    }

    @Override
    public void onPoolAcquireFailed(long duration, TimeUnit timeUnit, Throwable recievedError) {
        this.duration = duration;
        this.timeUnit = timeUnit;
        this.recievedError = recievedError;
        methodsCalled.add(ClientEvent.AcquireFailed);
    }

    @Override
    public void onConnectionCloseFailed(long duration, TimeUnit timeUnit,
                                        Throwable recievedError) {
        delegate.onConnectionCloseFailed(duration, timeUnit, recievedError);
    }

    @Override
    public void onConnectionCloseSuccess(long duration, TimeUnit timeUnit) {
        delegate.onConnectionCloseSuccess(duration, timeUnit);
    }

    @Override
    public void onConnectionCloseStart() {
        delegate.onConnectionCloseStart();
    }

    @Override
    public void onWriteFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        delegate.onWriteFailed(duration, timeUnit, throwable);
    }

    @Override
    public void onWriteSuccess(long duration, TimeUnit timeUnit) {
        delegate.onWriteSuccess(duration, timeUnit);
    }

    @Override
    public void onWriteStart() {
        delegate.onWriteStart();
    }

    @Override
    public void onFlushComplete(long duration, TimeUnit timeUnit) {
        delegate.onFlushComplete(duration, timeUnit);
    }

    @Override
    public void onFlushStart() {
        delegate.onFlushStart();
    }

    @Override
    public void onByteRead(long bytesRead) {
        delegate.onByteRead(bytesRead);
    }

    @Override
    public void onByteWritten(long bytesWritten) {
        delegate.onByteWritten(bytesWritten);
    }

    @Override
    public void onCustomEvent(Object event) {
        delegate.onCustomEvent(event);
    }

    @Override
    public void onCustomEvent(Object event, long duration, TimeUnit timeUnit) {
        delegate.onCustomEvent(event, duration, timeUnit);
    }

    @Override
    public void onCustomEvent(Object event, long duration, TimeUnit timeUnit, Throwable throwable) {
        delegate.onCustomEvent(event, duration, timeUnit, throwable);
    }

    @Override
    public void onCustomEvent(Object event, Throwable throwable) {
        delegate.onCustomEvent(event, throwable);
    }

    @Override
    public void onCompleted() {
        delegate.onCompleted();
    }

    public void assertMethodsCalled(Event... events) {
        delegate.assertMethodsCalled(events);
    }

    public void assertMethodsCalled(ClientEvent... events) {
        if (methodsCalled.size() != events.length) {
            throw new AssertionError("Unexpected methods called count. Methods called: " + methodsCalled.size()
                                     + ". Expected: " + events.length);
        }

        if (methodsCalled.containsAll(Arrays.asList(events))) {
            throw new AssertionError("Unexpected methods called count. Methods called: " + methodsCalled
                                     + ". Expected: " + Arrays.toString(events));
        }
    }

    public long getDuration() {
        return duration;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public Throwable getRecievedError() {
        return recievedError;
    }
}
