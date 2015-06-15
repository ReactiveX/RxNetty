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

import io.reactivex.netty.protocol.tcp.client.events.TcpClientEventListenerImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class HttpClientEventsListenerImpl extends HttpClientEventsListener {

    public enum HttpEvent {
        ReqSubmitted, ReqWriteStart, ReqWriteSuccess, ReqWriteFailed, ResHeadersReceived, ResContentReceived,
        ResReceiveComplete, RespFailed, ProcessingComplete
    }

    private final TcpClientEventListenerImpl tcpDelegate;

    private int responseCode;
    private long duration;
    private TimeUnit timeUnit;
    private Throwable recievedError;
    private final List<HttpEvent> methodsCalled = new ArrayList<>();

    public HttpClientEventsListenerImpl() {
        tcpDelegate = new TcpClientEventListenerImpl();
    }

    @Override
    public void onRequestSubmitted() {
        methodsCalled.add(HttpEvent.ReqSubmitted);
    }

    @Override
    public void onRequestWriteStart() {
        methodsCalled.add(HttpEvent.ReqWriteStart);
    }

    @Override
    public void onRequestWriteComplete(long duration, TimeUnit timeUnit) {
        this.duration = duration;
        this.timeUnit = timeUnit;
        methodsCalled.add(HttpEvent.ReqWriteSuccess);
    }

    @Override
    public void onRequestWriteFailed(long duration, TimeUnit timeUnit, Throwable recievedError) {
        this.duration = duration;
        this.timeUnit = timeUnit;
        this.recievedError = recievedError;
        methodsCalled.add(HttpEvent.ReqWriteFailed);
    }

    @Override
    public void onResponseHeadersReceived(int responseCode) {
        this.responseCode = responseCode;
        methodsCalled.add(HttpEvent.ResHeadersReceived);
    }

    @Override
    public void onResponseContentReceived() {
        methodsCalled.add(HttpEvent.ResContentReceived);
    }

    @Override
    public void onResponseReceiveComplete(long duration, TimeUnit timeUnit) {
        this.duration = duration;
        this.timeUnit = timeUnit;
        methodsCalled.add(HttpEvent.ResReceiveComplete);
    }

    @Override
    public void onResponseFailed(Throwable recievedError) {
        this.recievedError = recievedError;
        methodsCalled.add(HttpEvent.RespFailed);
    }

    @Override
    public void onRequestProcessingComplete(long duration, TimeUnit timeUnit) {
        this.duration = duration;
        this.timeUnit = timeUnit;
        methodsCalled.add(HttpEvent.ProcessingComplete);
    }

    public int getResponseCode() {
        return responseCode;
    }

    public Throwable getRecievedError() {
        return recievedError;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public long getDuration() {
        return duration;
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
    public void onConnectFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
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
    public void onConnectionCloseFailed(long duration, TimeUnit timeUnit,
                                        Throwable throwable) {
        tcpDelegate.onConnectionCloseFailed(duration, timeUnit, throwable);
    }

    @Override
    public void onCompleted() {
        tcpDelegate.onCompleted();
    }

    public void assertMethodCalled(HttpEvent... events) {
        assertThat("Unexpected methods called count.", methodsCalled, hasSize(events.length));
        assertThat("Unexpected methods called.", methodsCalled, contains(events));
    }

    public TcpClientEventListenerImpl getTcpDelegate() {
        return tcpDelegate;
    }
}
