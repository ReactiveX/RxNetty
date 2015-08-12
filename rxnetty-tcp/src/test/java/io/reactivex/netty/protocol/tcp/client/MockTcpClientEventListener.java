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

package io.reactivex.netty.protocol.tcp.client;

import io.reactivex.netty.protocol.tcp.client.events.TcpClientEventListener;
import io.reactivex.netty.test.util.MockClientEventListener;
import io.reactivex.netty.test.util.MockClientEventListener.ClientEvent;
import io.reactivex.netty.test.util.MockConnectionEventListener.Event;

import java.util.concurrent.TimeUnit;

public class MockTcpClientEventListener extends TcpClientEventListener {

    private final MockClientEventListener mockDelegate = new MockClientEventListener();

    public void assertMethodsCalled(ClientEvent... events) {
        mockDelegate.assertMethodsCalled(events);
    }

    public void assertMethodsCalled(Event... events) {
        mockDelegate.assertMethodsCalled(events);
    }

    public long getDuration() {
        return mockDelegate.getDuration();
    }

    public Throwable getRecievedError() {
        return mockDelegate.getRecievedError();
    }

    public TimeUnit getTimeUnit() {
        return mockDelegate.getTimeUnit();
    }

    @Override
    public void onByteRead(long bytesRead) {
        mockDelegate.onByteRead(bytesRead);
    }

    @Override
    public void onCompleted() {
        mockDelegate.onCompleted();
    }

    @Override
    public void onConnectFailed(long duration, TimeUnit timeUnit, Throwable recievedError) {
        mockDelegate.onConnectFailed(duration, timeUnit, recievedError);
    }

    @Override
    public void onConnectionCloseFailed(long duration, TimeUnit timeUnit,
                                        Throwable recievedError) {
        mockDelegate.onConnectionCloseFailed(duration, timeUnit, recievedError);
    }

    @Override
    public void onConnectionCloseStart() {
        mockDelegate.onConnectionCloseStart();
    }

    @Override
    public void onConnectionCloseSuccess(long duration, TimeUnit timeUnit) {
        mockDelegate.onConnectionCloseSuccess(duration, timeUnit);
    }

    @Override
    public void onConnectStart() {
        mockDelegate.onConnectStart();
    }

    @Override
    public void onConnectSuccess(long duration, TimeUnit timeUnit) {
        mockDelegate.onConnectSuccess(duration, timeUnit);
    }

    @Override
    public void onCustomEvent(Object event) {
        mockDelegate.onCustomEvent(event);
    }

    @Override
    public void onCustomEvent(Object event, long duration, TimeUnit timeUnit) {
        mockDelegate.onCustomEvent(event, duration, timeUnit);
    }

    @Override
    public void onCustomEvent(Object event, long duration, TimeUnit timeUnit,
                              Throwable throwable) {
        mockDelegate.onCustomEvent(event, duration, timeUnit, throwable);
    }

    @Override
    public void onCustomEvent(Object event, Throwable throwable) {
        mockDelegate.onCustomEvent(event, throwable);
    }

    @Override
    public void onFlushFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        mockDelegate.onFlushFailed(duration, timeUnit, throwable);
    }

    @Override
    public void onFlushStart() {
        mockDelegate.onFlushStart();
    }

    @Override
    public void onFlushSuccess(long duration, TimeUnit timeUnit) {
        mockDelegate.onFlushSuccess(duration, timeUnit);
    }

    @Override
    public void onPoolAcquireFailed(long duration, TimeUnit timeUnit,
                                    Throwable recievedError) {
        mockDelegate.onPoolAcquireFailed(duration, timeUnit, recievedError);
    }

    @Override
    public void onPoolAcquireStart() {
        mockDelegate.onPoolAcquireStart();
    }

    @Override
    public void onPoolAcquireSuccess(long duration, TimeUnit timeUnit) {
        mockDelegate.onPoolAcquireSuccess(duration, timeUnit);
    }

    @Override
    public void onPooledConnectionEviction() {
        mockDelegate.onPooledConnectionEviction();
    }

    @Override
    public void onPooledConnectionReuse() {
        mockDelegate.onPooledConnectionReuse();
    }

    @Override
    public void onPoolReleaseFailed(long duration, TimeUnit timeUnit,
                                    Throwable recievedError) {
        mockDelegate.onPoolReleaseFailed(duration, timeUnit, recievedError);
    }

    @Override
    public void onPoolReleaseStart() {
        mockDelegate.onPoolReleaseStart();
    }

    @Override
    public void onPoolReleaseSuccess(long duration, TimeUnit timeUnit) {
        mockDelegate.onPoolReleaseSuccess(duration, timeUnit);
    }

    @Override
    public void onWriteFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        mockDelegate.onWriteFailed(duration, timeUnit, throwable);
    }

    @Override
    public void onWriteStart() {
        mockDelegate.onWriteStart();
    }

    @Override
    public void onWriteSuccess(long duration, TimeUnit timeUnit, long bytesWritten) {
        mockDelegate.onWriteSuccess(duration, timeUnit, bytesWritten);
    }
}
