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

import io.reactivex.netty.protocol.tcp.server.events.TcpServerEventListenerImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class HttpServerEventsListenerImpl extends HttpServerEventsListener {

    public enum HttpEvent {
        ReqRecv, HandlingStart, HandlingSuccess, HandlingFailed, ReqHdrsReceived, ReqContentReceived,
        ReqReceiveComplete, RespWriteStart, RespWriteSuccess, RespWriteFailed
    }

    private final TcpServerEventListenerImpl tcpDelegate;

    private int responseCode;
    private long duration;
    private TimeUnit timeUnit;
    private Throwable recievedError;
    private final List<HttpEvent> methodsCalled = new ArrayList<>();

    public HttpServerEventsListenerImpl() {
        tcpDelegate = new TcpServerEventListenerImpl();
    }

    @Override
    public void onNewRequestReceived() {
        methodsCalled.add(HttpEvent.ReqRecv);
    }

    @Override
    public void onRequestHandlingStart(long duration, TimeUnit timeUnit) {
        this.duration = duration;
        this.timeUnit = timeUnit;
        methodsCalled.add(HttpEvent.HandlingStart);
    }

    @Override
    public void onRequestHandlingSuccess(long duration, TimeUnit timeUnit) {
        this.duration = duration;
        this.timeUnit = timeUnit;
        methodsCalled.add(HttpEvent.HandlingSuccess);
    }

    @Override
    public void onRequestHandlingFailed(long duration, TimeUnit timeUnit, Throwable recievedError) {
        this.duration = duration;
        this.timeUnit = timeUnit;
        this.recievedError = recievedError;
        methodsCalled.add(HttpEvent.HandlingFailed);
    }

    @Override
    public void onRequestHeadersReceived() {
        methodsCalled.add(HttpEvent.ReqHdrsReceived);
    }

    @Override
    public void onRequestContentReceived() {
        methodsCalled.add(HttpEvent.ReqContentReceived);
    }

    @Override
    public void onRequestReceiveComplete(long duration, TimeUnit timeUnit) {
        this.duration = duration;
        this.timeUnit = timeUnit;
        methodsCalled.add(HttpEvent.ReqReceiveComplete);
    }

    @Override
    public void onResponseWriteStart() {
        methodsCalled.add(HttpEvent.RespWriteStart);
    }

    @Override
    public void onResponseWriteSuccess(long duration, TimeUnit timeUnit, int responseCode) {
        this.duration = duration;
        this.timeUnit = timeUnit;
        this.responseCode = responseCode;
        methodsCalled.add(HttpEvent.RespWriteSuccess);
    }

    @Override
    public void onResponseWriteFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        this.duration = duration;
        this.timeUnit = timeUnit;
        recievedError = throwable;
        methodsCalled.add(HttpEvent.RespWriteFailed);
    }

    @Override
    public void onByteRead(long bytesRead) {
        tcpDelegate.onByteRead(bytesRead);
    }

    @Override
    public void onNewClientConnected() {
        tcpDelegate.onNewClientConnected();
    }

    @Override
    public void onConnectionHandlingStart(long duration, TimeUnit timeUnit) {
        tcpDelegate.onConnectionHandlingStart(duration, timeUnit);
    }

    @Override
    public void onConnectionHandlingSuccess(long duration, TimeUnit timeUnit) {
        tcpDelegate.onConnectionHandlingSuccess(duration, timeUnit);
    }

    @Override
    public void onConnectionHandlingFailed(long duration, TimeUnit timeUnit,
                                           Throwable recievedError) {
        tcpDelegate.onConnectionHandlingFailed(duration, timeUnit, recievedError);
    }

    public void assertMethodsCalled(HttpEvent... events) {
        assertThat("Unexpected methods called count.", methodsCalled, hasSize(events.length));
        assertThat("Unexpected methods called.", methodsCalled, contains(events));
    }

    public TcpServerEventListenerImpl getTcpDelegate() {
        return tcpDelegate;
    }

    public int getResponseCode() {
        return responseCode;
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
    public void onCompleted() {
        tcpDelegate.onCompleted();
    }

    @Override
    public void onConnectionCloseFailed(long duration, TimeUnit timeUnit,
                                        Throwable recievedError) {
        tcpDelegate.onConnectionCloseFailed(duration, timeUnit, recievedError);
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
}
