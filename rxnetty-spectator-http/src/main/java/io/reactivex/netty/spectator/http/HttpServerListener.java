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

import com.netflix.spectator.api.Counter;
import com.netflix.spectator.api.Timer;
import io.reactivex.netty.protocol.http.server.events.HttpServerEventsListener;
import io.reactivex.netty.spectator.tcp.TcpServerListener;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.reactivex.netty.spectator.SpectatorUtils.*;

/**
 * HttpServerListener.
 */
public class HttpServerListener extends HttpServerEventsListener {

    private final AtomicInteger requestBacklog;
    private final AtomicInteger inflightRequests;
    private final Counter processedRequests;
    private final Counter failedRequests;
    private final Counter responseWriteFailed;
    private final Timer responseWriteTimes;
    private final Timer requestReadTimes;

    private final TcpServerListener tcpDelegate;

    public HttpServerListener(String monitorId) {
        requestBacklog = newGauge("requestBacklog", monitorId, new AtomicInteger());
        inflightRequests = newGauge("inflightRequests", monitorId, new AtomicInteger());
        responseWriteTimes = newTimer("responseWriteTimes", monitorId);
        requestReadTimes = newTimer("requestReadTimes", monitorId);
        processedRequests = newCounter("processedRequests", monitorId);
        failedRequests = newCounter("failedRequests", monitorId);
        responseWriteFailed = newCounter("responseWriteFailed", monitorId);
        tcpDelegate = new TcpServerListener(monitorId);
    }

    public long getRequestBacklog() {
        return requestBacklog.get();
    }

    public long getInflightRequests() {
        return inflightRequests.get();
    }

    public long getProcessedRequests() {
        return processedRequests.count();
    }

    public long getFailedRequests() {
        return failedRequests.count();
    }

    public long getResponseWriteFailed() {
        return responseWriteFailed.count();
    }

    public Timer getResponseWriteTimes() {
        return responseWriteTimes;
    }

    public Timer getRequestReadTimes() {
        return requestReadTimes;
    }

    public static HttpServerListener newHttpListener(String monitorId) {
        return new HttpServerListener(monitorId);
    }
    
    @Override
    public void onRequestHandlingFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        processedRequests.increment();
        inflightRequests.decrementAndGet();
        failedRequests.increment();
    }

    @Override
    public void onRequestHandlingSuccess(long duration, TimeUnit timeUnit) {
        inflightRequests.decrementAndGet();
        processedRequests.increment();
    }

    @Override
    public void onResponseWriteSuccess(long duration, TimeUnit timeUnit, int responseCode) {
        responseWriteTimes.record(duration, timeUnit);
    }

    @Override
    public void onResponseWriteFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        responseWriteFailed.increment();
    }

    @Override
    public void onRequestReceiveComplete(long duration, TimeUnit timeUnit) {
        requestReadTimes.record(duration, timeUnit);
    }

    @Override
    public void onRequestHandlingStart(long duration, TimeUnit timeUnit) {
        requestBacklog.decrementAndGet();
    }

    @Override
    public void onNewRequestReceived() {
        requestBacklog.incrementAndGet();
        inflightRequests.incrementAndGet();
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

    public long getLiveConnections() {
        return tcpDelegate.getLiveConnections();
    }

    public long getInflightConnections() {
        return tcpDelegate.getInflightConnections();
    }

    public long getFailedConnections() {
        return tcpDelegate.getFailedConnections();
    }

    public Timer getConnectionProcessingTimes() {
        return tcpDelegate.getConnectionProcessingTimes();
    }

    public long getPendingWrites() {
        return tcpDelegate.getPendingWrites();
    }

    public long getPendingFlushes() {
        return tcpDelegate.getPendingFlushes();
    }

    public long getBytesWritten() {
        return tcpDelegate.getBytesWritten();
    }

    public Timer getWriteTimes() {
        return tcpDelegate.getWriteTimes();
    }

    public long getBytesRead() {
        return tcpDelegate.getBytesRead();
    }

    public long getFailedWrites() {
        return tcpDelegate.getFailedWrites();
    }

    public long getFailedFlushes() {
        return tcpDelegate.getFailedFlushes();
    }

    public Timer getFlushTimes() {
        return tcpDelegate.getFlushTimes();
    }
}
