/*
 * Copyright 2014 Netflix, Inc.
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
import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.metrics.HttpClientMetricEventsListener;
import io.reactivex.netty.spectator.tcp.TcpClientListener;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.reactivex.netty.spectator.SpectatorUtils.newCounter;
import static io.reactivex.netty.spectator.SpectatorUtils.newGauge;
import static io.reactivex.netty.spectator.SpectatorUtils.newTimer;
/**
 * HttpClientListener.
 *  */
public class HttpClientListener extends TcpClientListener<ClientMetricsEvent<?>> {

    private final AtomicInteger requestBacklog;
    private final AtomicInteger inflightRequests;
    private final Counter processedRequests;
    private final Counter requestWriteFailed;
    private final Counter failedResponses;
    private final Counter failedContentSource;
    private final Timer requestWriteTimes;
    private final Timer responseReadTimes;
    private final Timer requestProcessingTimes;

    private final HttpClientMetricEventsListenerImpl delegate = new HttpClientMetricEventsListenerImpl();

    protected HttpClientListener(String monitorId) {
        super(monitorId);
        requestBacklog = newGauge("requestBacklog", monitorId, new AtomicInteger());
        inflightRequests = newGauge("inflightRequests", monitorId, new AtomicInteger());
        requestWriteTimes = newTimer("requestWriteTimes", monitorId);
        responseReadTimes = newTimer("responseReadTimes", monitorId);
        processedRequests = newCounter("processedRequests", monitorId);
        requestWriteFailed = newCounter("requestWriteFailed", monitorId);
        failedResponses = newCounter("failedResponses", monitorId);
        failedContentSource = newCounter("failedContentSource", monitorId);
        requestProcessingTimes = newTimer("requestProcessingTimes", monitorId);
    }

    @Override
    public void onEvent(ClientMetricsEvent<?> event, long duration, TimeUnit timeUnit, Throwable throwable,
                        Object value) {
        delegate.onEvent(event, duration, timeUnit, throwable, value);
    }

    public static HttpClientListener newHttpListener(String monitorId) {
        return new HttpClientListener(monitorId);
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

    public long getFailedContentSource() {
        return failedContentSource.count();
    }

    public Timer getRequestWriteTimes() {
        return requestWriteTimes;
    }

    public Timer getResponseReadTimes() {
        return responseReadTimes;
    }

    private class HttpClientMetricEventsListenerImpl extends HttpClientMetricEventsListener {

        @Override
        protected void onRequestProcessingComplete(long duration, TimeUnit timeUnit) {
            requestProcessingTimes.record(duration, timeUnit);
        }

        @Override
        protected void onResponseReceiveComplete(long duration, TimeUnit timeUnit) {
            inflightRequests.decrementAndGet();
            processedRequests.increment();
            responseReadTimes.record(duration, timeUnit);
        }

        @Override
        protected void onResponseFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
            inflightRequests.decrementAndGet();
            processedRequests.increment();
            failedResponses.increment();
        }

        @Override
        protected void onRequestContentSourceError(Throwable throwable) {
            failedContentSource.increment();
        }

        @Override
        protected void onRequestWriteComplete(long duration, TimeUnit timeUnit) {
            requestWriteTimes.record(duration, timeUnit);
        }

        @Override
        protected void onRequestWriteFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
            inflightRequests.decrementAndGet();
            requestWriteFailed.increment();
        }

        @Override
        protected void onRequestHeadersWriteStart() {
            requestBacklog.decrementAndGet();
        }

        @Override
        protected void onRequestSubmitted() {
            requestBacklog.incrementAndGet();
            inflightRequests.incrementAndGet();
        }

        @Override
        protected void onByteRead(long bytesRead) {
            HttpClientListener.this.onByteRead(bytesRead);
        }

        @Override
        protected void onFlushFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
            HttpClientListener.this.onFlushFailed(duration, timeUnit, throwable);
        }

        @Override
        protected void onFlushSuccess(long duration, TimeUnit timeUnit) {
            HttpClientListener.this.onFlushSuccess(duration, timeUnit);
        }

        @Override
        protected void onFlushStart() {
            HttpClientListener.this.onFlushStart();
        }

        @Override
        protected void onWriteFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
            HttpClientListener.this.onWriteFailed(duration, timeUnit, throwable);
        }

        @Override
        protected void onWriteSuccess(long duration, TimeUnit timeUnit, long bytesWritten) {
            HttpClientListener.this.onWriteSuccess(duration, timeUnit, bytesWritten);
        }

        @Override
        protected void onWriteStart() {
            HttpClientListener.this.onWriteStart();
        }

        @Override
        protected void onPoolReleaseFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
            HttpClientListener.this.onPoolReleaseFailed(duration, timeUnit, throwable);
        }

        @Override
        protected void onPoolReleaseSuccess(long duration, TimeUnit timeUnit) {
            HttpClientListener.this.onPoolReleaseSuccess(duration, timeUnit);
        }

        @Override
        protected void onPoolReleaseStart() {
            HttpClientListener.this.onPoolReleaseStart();
        }

        @Override
        protected void onPooledConnectionEviction() {
            HttpClientListener.this.onPooledConnectionEviction();
        }

        @Override
        protected void onPooledConnectionReuse(long duration, TimeUnit timeUnit) {
            HttpClientListener.this.onPooledConnectionReuse(duration, timeUnit);
        }

        @Override
        protected void onPoolAcquireFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
            HttpClientListener.this.onPoolAcquireFailed(duration, timeUnit, throwable);
        }

        @Override
        protected void onPoolAcquireSuccess(long duration, TimeUnit timeUnit) {
            HttpClientListener.this.onPoolAcquireSuccess(duration, timeUnit);
        }

        @Override
        protected void onPoolAcquireStart() {
            HttpClientListener.this.onPoolAcquireStart();
        }

        @Override
        protected void onConnectionCloseFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
            HttpClientListener.this.onConnectionCloseFailed(duration, timeUnit, throwable);
        }

        @Override
        protected void onConnectionCloseSuccess(long duration, TimeUnit timeUnit) {
            HttpClientListener.this.onConnectionCloseSuccess(duration, timeUnit);
        }

        @Override
        protected void onConnectionCloseStart() {
            HttpClientListener.this.onConnectionCloseStart();
        }

        @Override
        protected void onConnectFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
            HttpClientListener.this.onConnectFailed(duration, timeUnit, throwable);
        }

        @Override
        protected void onConnectSuccess(long duration, TimeUnit timeUnit) {
            HttpClientListener.this.onConnectSuccess(duration, timeUnit);
        }

        @Override
        protected void onConnectStart() {
            HttpClientListener.this.onConnectStart();
        }
    }
}
