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
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.Spectator;
import io.reactivex.netty.protocol.http.client.events.HttpClientEventsListener;
import io.reactivex.netty.spectator.http.internal.ResponseCodesHolder;
import io.reactivex.netty.spectator.internal.LatencyMetrics;
import io.reactivex.netty.spectator.tcp.TcpClientListener;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.reactivex.netty.spectator.internal.SpectatorUtils.*;

/**
 * HttpClientListener.
 */
public class HttpClientListener extends HttpClientEventsListener {

    private final AtomicInteger requestBacklog;
    private final AtomicInteger inflightRequests;
    private final Counter processedRequests;
    private final Counter requestWriteFailed;
    private final Counter failedResponses;
    private final ResponseCodesHolder responseCodesHolder;
    private final LatencyMetrics requestWriteTimes;
    private final LatencyMetrics responseReadTimes;
    private final LatencyMetrics requestProcessingTimes;
    private final TcpClientListener tcpDelegate;

    public HttpClientListener(Registry registry, String monitorId) {
        requestBacklog = newGauge(registry, "requestBacklog", monitorId, new AtomicInteger());
        inflightRequests = newGauge(registry, "inflightRequests", monitorId, new AtomicInteger());
        requestWriteTimes = new LatencyMetrics("requestWriteTimes", monitorId, registry);
        responseReadTimes = new LatencyMetrics("responseReadTimes", monitorId, registry);
        processedRequests = newCounter(registry, "processedRequests", monitorId);
        requestWriteFailed = newCounter(registry, "requestWriteFailed", monitorId);
        failedResponses = newCounter(registry, "failedResponses", monitorId);
        requestProcessingTimes = new LatencyMetrics("requestProcessingTimes", monitorId, registry);
        responseCodesHolder = new ResponseCodesHolder(registry, monitorId);
        tcpDelegate = new TcpClientListener(registry, monitorId);
    }

    public HttpClientListener(String monitorId) {
        this(Spectator.globalRegistry(), monitorId);
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

    public long getResponse1xx() {
        return responseCodesHolder.getResponse1xx();
    }

    public long getResponse2xx() {
        return responseCodesHolder.getResponse2xx();
    }

    public long getResponse3xx() {
        return responseCodesHolder.getResponse3xx();
    }

    public long getResponse4xx() {
        return responseCodesHolder.getResponse4xx();
    }

    public long getResponse5xx() {
        return responseCodesHolder.getResponse5xx();
    }

    @Override
    public void onRequestProcessingComplete(long duration, TimeUnit timeUnit) {
        requestProcessingTimes.record(duration, timeUnit);
    }

    @Override
    public void onResponseHeadersReceived(int responseCode, long duration, TimeUnit timeUnit) {
        responseCodesHolder.update(responseCode);
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

    public long getPendingPoolReleases() {
        return tcpDelegate.getPendingPoolReleases();
    }

    public long getFailedPoolReleases() {
        return tcpDelegate.getFailedPoolReleases();
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

    public long getBytesRead() {
        return tcpDelegate.getBytesRead();
    }

    public long getFailedWrites() {
        return tcpDelegate.getFailedWrites();
    }

    public long getFailedFlushes() {
        return tcpDelegate.getFailedFlushes();
    }

    public long getPoolAcquires() {
        return tcpDelegate.getPoolAcquires();
    }

    public long getPoolReleases() {
        return tcpDelegate.getPoolReleases();
    }
}
