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
package io.reactivex.netty.protocol.tcp.server.events;

import io.reactivex.netty.channel.events.ConnectionEventPublisherTest.ConnectionEventListenerImpl;
import io.reactivex.netty.channel.events.ConnectionEventPublisherTest.ConnectionEventListenerImpl.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class TcpServerEventListenerImpl extends TcpServerEventListener {

    public enum ServerEvent {
        NewClient, HandlingStart, HandlingSuccess, HandlingFailed
    }

    private final List<ServerEvent> methodsCalled = new ArrayList<>();
    private long duration;
    private TimeUnit timeUnit;
    private Throwable recievedError;

    private final ConnectionEventListenerImpl connDelegate;

    public TcpServerEventListenerImpl() {
        connDelegate = new ConnectionEventListenerImpl();
    }

    @Override
    public void onNewClientConnected() {
        methodsCalled.add(ServerEvent.NewClient);
    }

    @Override
    public void onConnectionHandlingStart(long duration, TimeUnit timeUnit) {
        this.duration = duration;
        this.timeUnit = timeUnit;
        methodsCalled.add(ServerEvent.HandlingStart);
    }

    @Override
    public void onConnectionHandlingSuccess(long duration, TimeUnit timeUnit) {
        this.duration = duration;
        this.timeUnit = timeUnit;
        methodsCalled.add(ServerEvent.HandlingSuccess);
    }

    @Override
    public void onConnectionHandlingFailed(long duration, TimeUnit timeUnit, Throwable recievedError) {
        this.duration = duration;
        this.timeUnit = timeUnit;
        this.recievedError = recievedError;
        methodsCalled.add(ServerEvent.HandlingFailed);
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

    @Override
    public void onSubscribe() {
        connDelegate.onSubscribe();
    }

    @Override
    public void onCompleted() {
        connDelegate.onCompleted();
    }

    @Override
    public void onConnectionCloseFailed(long duration, TimeUnit timeUnit,
                                        Throwable recievedError) {
        connDelegate.onConnectionCloseFailed(duration, timeUnit, recievedError);
    }

    @Override
    public void onConnectionCloseSuccess(long duration, TimeUnit timeUnit) {
        connDelegate.onConnectionCloseSuccess(duration, timeUnit);
    }

    @Override
    public void onConnectionCloseStart() {
        connDelegate.onConnectionCloseStart();
    }

    @Override
    public void onWriteFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        connDelegate.onWriteFailed(duration, timeUnit, throwable);
    }

    @Override
    public void onWriteSuccess(long duration, TimeUnit timeUnit, long bytesWritten) {
        connDelegate.onWriteSuccess(duration, timeUnit, bytesWritten);
    }

    @Override
    public void onWriteStart() {
        connDelegate.onWriteStart();
    }

    @Override
    public void onFlushFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        connDelegate.onFlushFailed(duration, timeUnit, throwable);
    }

    @Override
    public void onFlushSuccess(long duration, TimeUnit timeUnit) {
        connDelegate.onFlushSuccess(duration, timeUnit);
    }

    @Override
    public void onFlushStart() {
        connDelegate.onFlushStart();
    }

    @Override
    public void onByteRead(long bytesRead) {
        connDelegate.onByteRead(bytesRead);
    }

    public void assertMethodsCalled(ServerEvent... events) {
        connDelegate.assertMethodsCalled(Event.Subscribe);
        assertThat("Unexpected methods called count.", methodsCalled, hasSize(events.length));
        assertThat("Unexpected methods called.", methodsCalled, contains(events));
    }

    public ConnectionEventListenerImpl getConnDelegate() {
        return connDelegate;
    }
}
