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

package io.reactivex.netty.spectator.http;

import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.Spectator;
import io.reactivex.netty.protocol.http.server.events.HttpServerEventsListener;
import io.reactivex.netty.spectator.http.internal.ResponseCodesHolder;
import io.reactivex.netty.spectator.internal.EventMetric;
import io.reactivex.netty.spectator.tcp.TcpServerListener;

import java.util.concurrent.TimeUnit;

/**
 * HttpServerListener.
 */
public class HttpServerListener extends HttpServerEventsListener {

    private final EventMetric requestRead;
    private final EventMetric requestProcessing;
    private final EventMetric responseWrite;

    private final ResponseCodesHolder responseCodesHolder;
    private final TcpServerListener tcpDelegate;

    public HttpServerListener(String monitorId) {
        this(Spectator.globalRegistry(), monitorId);
    }

    public HttpServerListener(Registry registry, String monitorId) {
        requestRead = new EventMetric(registry, "request", monitorId, "action", "read");
        requestProcessing = new EventMetric(registry, "request", monitorId, "action", "processing");
        responseWrite = new EventMetric(registry, "response", monitorId, "action", "write");
        responseCodesHolder = new ResponseCodesHolder(registry, monitorId);
        tcpDelegate = new TcpServerListener(registry, monitorId);
    }

    public static HttpServerListener newHttpListener(String monitorId) {
        return new HttpServerListener(monitorId);
    }
    
    @Override
    public void onRequestHandlingFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        requestProcessing.failure(duration, timeUnit);
    }

    @Override
    public void onRequestHandlingSuccess(long duration, TimeUnit timeUnit) {
        requestProcessing.success(duration, timeUnit);
    }

    @Override
    public void onResponseWriteSuccess(long duration, TimeUnit timeUnit, int responseCode) {
        responseWrite.success(duration, timeUnit);
    }

    @Override
    public void onResponseWriteFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        responseWrite.failure(duration, timeUnit);
    }

    @Override
    public void onRequestReceiveComplete(long duration, TimeUnit timeUnit) {
        requestRead.success(duration, timeUnit);
    }

    @Override
    public void onRequestHandlingStart(long duration, TimeUnit timeUnit) {
        requestProcessing.start(duration, timeUnit);
    }

    @Override
    public void onRequestHeadersReceived() {
        requestRead.start();
    }

    @Override
    public void onResponseWriteStart() {
        responseWrite.start();
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
    public void onNewClientConnected() {
        tcpDelegate.onNewClientConnected();
    }

    @Override
    public void onByteRead(long bytesRead) {
        tcpDelegate.onByteRead(bytesRead);
    }

    @Override
    public void onByteWritten(long bytesWritten) {
        tcpDelegate.onByteWritten(bytesWritten);
    }

    @Override
    public void onFlushComplete(long duration, TimeUnit timeUnit) {
        tcpDelegate.onFlushComplete(duration, timeUnit);
    }

    @Override
    public void onFlushStart() {
        tcpDelegate.onFlushStart();
    }

    @Override
    public void onWriteFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        tcpDelegate.onWriteFailed(duration, timeUnit, throwable);
    }

    @Override
    public void onWriteSuccess(long duration, TimeUnit timeUnit) {
        tcpDelegate.onWriteSuccess(duration, timeUnit);
    }

    @Override
    public void onWriteStart() {
        tcpDelegate.onWriteStart();
    }
}
