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
import io.reactivex.netty.protocol.http.client.events.HttpClientEventsListener;
import io.reactivex.netty.spectator.http.internal.ResponseCodesHolder;
import io.reactivex.netty.spectator.internal.EventMetric;
import io.reactivex.netty.spectator.tcp.TcpClientListener;

import java.util.concurrent.TimeUnit;

/**
 * HttpClientListener.
 */
public class HttpClientListener extends HttpClientEventsListener {

    private final EventMetric requestWrite;
    private final EventMetric requestProcessing;
    private final EventMetric response;

    private final ResponseCodesHolder responseCodesHolder;
    private final TcpClientListener tcpDelegate;

    public HttpClientListener(Registry registry, String monitorId) {
        requestWrite = new EventMetric(registry, "request", monitorId, "action", "write");
        requestProcessing = new EventMetric(registry, "request", monitorId, "action", "processing");
        response = new EventMetric(registry, "response", monitorId, "action", "read");

        responseCodesHolder = new ResponseCodesHolder(registry, monitorId);
        tcpDelegate = new TcpClientListener(registry, monitorId);
    }

    public HttpClientListener(String monitorId) {
        this(Spectator.globalRegistry(), monitorId);
    }

    @Override
    public void onRequestProcessingComplete(long duration, TimeUnit timeUnit) {
        requestProcessing.success(duration, timeUnit);
    }

    @Override
    public void onResponseHeadersReceived(int responseCode, long duration, TimeUnit timeUnit) {
        responseCodesHolder.update(responseCode);
    }

    @Override
    public void onResponseReceiveComplete(long duration, TimeUnit timeUnit) {
        response.success(duration, timeUnit);
    }

    @Override
    public void onRequestWriteStart() {
        requestWrite.start();
    }

    @Override
    public void onResponseFailed(Throwable throwable) {
        response.failure();
    }

    @Override
    public void onRequestWriteComplete(long duration, TimeUnit timeUnit) {
        requestWrite.success(duration, timeUnit);
    }

    @Override
    public void onRequestWriteFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        requestWrite.failure(duration, timeUnit);
    }

    @Override
    public void onRequestSubmitted() {
        requestProcessing.start();
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

    @Override
    public void onPoolReleaseFailed(long duration, TimeUnit timeUnit,
                                    Throwable throwable) {
        tcpDelegate.onPoolReleaseFailed(duration, timeUnit, throwable);
    }

    @Override
    public void onPoolReleaseSuccess(long duration, TimeUnit timeUnit) {
        tcpDelegate.onPoolReleaseSuccess(duration, timeUnit);
    }

    @Override
    public void onPoolReleaseStart() {
        tcpDelegate.onPoolReleaseStart();
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
    public void onPoolAcquireFailed(long duration, TimeUnit timeUnit,
                                    Throwable throwable) {
        tcpDelegate.onPoolAcquireFailed(duration, timeUnit, throwable);
    }

    @Override
    public void onPoolAcquireSuccess(long duration, TimeUnit timeUnit) {
        tcpDelegate.onPoolAcquireSuccess(duration, timeUnit);
    }

    @Override
    public void onPoolAcquireStart() {
        tcpDelegate.onPoolAcquireStart();
    }

    @Override
    public void onConnectionCloseFailed(long duration, TimeUnit timeUnit,
                                        Throwable throwable) {
        tcpDelegate.onConnectionCloseFailed(duration, timeUnit, throwable);
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
    public void onConnectFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        tcpDelegate.onConnectFailed(duration, timeUnit, throwable);
    }

    @Override
    public void onConnectSuccess(long duration, TimeUnit timeUnit) {
        tcpDelegate.onConnectSuccess(duration, timeUnit);
    }

    @Override
    public void onConnectStart() {
        tcpDelegate.onConnectStart();
    }
}
