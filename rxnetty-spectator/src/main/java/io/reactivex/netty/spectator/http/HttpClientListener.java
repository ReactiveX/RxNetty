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

package io.reactivex.netty.spectator.http;

import com.netflix.spectator.api.Counter;
import com.netflix.spectator.api.Timer;
import io.reactivex.netty.protocol.http.client.events.HttpClientEventsListener;
import io.reactivex.netty.spectator.tcp.TcpClientListener;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.reactivex.netty.spectator.SpectatorUtils.*;
/**
 * HttpClientListener.
 */
public class HttpClientListener extends HttpClientEventsListener {

    private final AtomicInteger requestBacklog;
    private final AtomicInteger inflightRequests;
    private final Counter processedRequests;
    private final Counter requestWriteFailed;
    private final Counter failedResponses;
    private final Timer requestWriteTimes;
    private final Timer responseReadTimes;
    private final Timer requestProcessingTimes;
    private final TcpClientListener tcpDelegate;

    public HttpClientListener(String monitorId) {
        requestBacklog = newGauge("requestBacklog", monitorId, new AtomicInteger());
        inflightRequests = newGauge("inflightRequests", monitorId, new AtomicInteger());
        requestWriteTimes = newTimer("requestWriteTimes", monitorId);
        responseReadTimes = newTimer("responseReadTimes", monitorId);
        processedRequests = newCounter("processedRequests", monitorId);
        requestWriteFailed = newCounter("requestWriteFailed", monitorId);
        failedResponses = newCounter("failedResponses", monitorId);
        requestProcessingTimes = newTimer("requestProcessingTimes", monitorId);
        tcpDelegate = new TcpClientListener(monitorId);
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

    public long getRequestWriteFailed() {
        return requestWriteFailed.count();
    }

    public long getFailedResponses() {
        return failedResponses.count();
    }

    public Timer getRequestWriteTimes() {
        return requestWriteTimes;
    }

    public Timer getResponseReadTimes() {
        return responseReadTimes;
    }

    @Override
    public void onRequestProcessingComplete(long duration, TimeUnit timeUnit) {
        requestProcessingTimes.record(duration, timeUnit);
    }

    @Override
    public void onResponseReceiveComplete(long duration, TimeUnit timeUnit) {
        inflightRequests.decrementAndGet();
        processedRequests.increment();
        responseReadTimes.record(duration, timeUnit);
    }

    @Override
    public void onRequestWriteStart() {
        requestBacklog.decrementAndGet();
    }

    @Override
    public void onResponseFailed(Throwable throwable) {
        inflightRequests.decrementAndGet();
        processedRequests.increment();
        failedResponses.increment();
    }

    @Override
    public void onRequestWriteComplete(long duration, TimeUnit timeUnit) {
        requestWriteTimes.record(duration, timeUnit);
    }

    @Override
    public void onRequestWriteFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        inflightRequests.decrementAndGet();
        requestWriteFailed.increment();
    }

    @Override
    public void onRequestSubmitted() {
        requestBacklog.incrementAndGet();
        inflightRequests.incrementAndGet();
    }

    @Override
    public void onByteRead(long bytesRead) {
        tcpDelegate.onByteRead(bytesRead);
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

    public long getLiveConnections() {
        return tcpDelegate.getLiveConnections();
    }

    public long getConnectionCount() {
        return tcpDelegate.getConnectionCount();
    }

    public long getPendingConnects() {
        return tcpDelegate.getPendingConnects();
    }

    public long getFailedConnects() {
        return tcpDelegate.getFailedConnects();
    }

    public Timer getConnectionTimes() {
        return tcpDelegate.getConnectionTimes();
    }

    public long getPendingConnectionClose() {
        return tcpDelegate.getPendingConnectionClose();
    }

    public long getFailedConnectionClose() {
        return tcpDelegate.getFailedConnectionClose();
    }

    public long getPendingPoolAcquires() {
        return tcpDelegate.getPendingPoolAcquires();
    }

    public long getFailedPoolAcquires() {
        return tcpDelegate.getFailedPoolAcquires();
    }

    public Timer getPoolAcquireTimes() {
        return tcpDelegate.getPoolAcquireTimes();
    }

    public long getPendingPoolReleases() {
        return tcpDelegate.getPendingPoolReleases();
    }

    public long getFailedPoolReleases() {
        return tcpDelegate.getFailedPoolReleases();
    }

    public Timer getPoolReleaseTimes() {
        return tcpDelegate.getPoolReleaseTimes();
    }

    public long getPoolEvictions() {
        return tcpDelegate.getPoolEvictions();
    }

    public long getPoolReuse() {
        return tcpDelegate.getPoolReuse();
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

    public long getPoolAcquires() {
        return tcpDelegate.getPoolAcquires();
    }

    public long getPoolReleases() {
        return tcpDelegate.getPoolReleases();
    }
}
