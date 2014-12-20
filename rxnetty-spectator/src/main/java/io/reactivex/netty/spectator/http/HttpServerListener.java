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

import io.reactivex.netty.metrics.HttpServerMetricEventsListener;
import io.reactivex.netty.server.ServerMetricsEvent;
import io.reactivex.netty.spectator.tcp.TcpServerListener;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.reactivex.netty.spectator.SpectatorUtils.newCounter;
import static io.reactivex.netty.spectator.SpectatorUtils.newGauge;
import static io.reactivex.netty.spectator.SpectatorUtils.newTimer;

/**
 * HttpServerListener.
 */
public class HttpServerListener extends TcpServerListener<ServerMetricsEvent<?>> {

    private final AtomicInteger requestBacklog;
    private final AtomicInteger inflightRequests;
    private final Counter processedRequests;
    private final Counter failedRequests;
    private final Counter responseWriteFailed;
    private final Timer responseWriteTimes;
    private final Timer requestReadTimes;

    private final HttpServerMetricEventsListenerImpl delegate;

    protected HttpServerListener(String monitorId) {
        super(monitorId);
        requestBacklog = newGauge("requestBacklog", monitorId, new AtomicInteger());
        inflightRequests = newGauge("inflightRequests", monitorId, new AtomicInteger());
        responseWriteTimes = newTimer("responseWriteTimes", monitorId);
        requestReadTimes = newTimer("requestReadTimes", monitorId);
        processedRequests = newCounter("processedRequests", monitorId);
        failedRequests = newCounter("failedRequests", monitorId);
        responseWriteFailed = newCounter("responseWriteFailed", monitorId);
        delegate = new HttpServerMetricEventsListenerImpl();
    }

    @Override
    public void onEvent(ServerMetricsEvent<?> event, long duration, TimeUnit timeUnit, Throwable throwable,
                        Object value) {
        delegate.onEvent(event, duration, timeUnit, throwable, value);
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

    private class HttpServerMetricEventsListenerImpl extends HttpServerMetricEventsListener {

        @Override
        protected void onRequestHandlingFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
            processedRequests.increment();
            inflightRequests.decrementAndGet();
            failedRequests.increment();
        }

        @Override
        protected void onRequestHandlingSuccess(long duration, TimeUnit timeUnit) {
            inflightRequests.decrementAndGet();
            processedRequests.increment();
        }

        @Override
        protected void onResponseContentWriteSuccess(long duration, TimeUnit timeUnit) {
            responseWriteTimes.record(duration, timeUnit);
        }

        @Override
        protected void onResponseHeadersWriteSuccess(long duration, TimeUnit timeUnit) {
            responseWriteTimes.record(duration, timeUnit);
        }

        @Override
        protected void onResponseContentWriteFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
            responseWriteFailed.increment();
        }

        @Override
        protected void onResponseHeadersWriteFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
            responseWriteFailed.increment();
        }

        @Override
        protected void onRequestReceiveComplete(long duration, TimeUnit timeUnit) {
            requestReadTimes.record(duration, timeUnit);
        }

        @Override
        protected void onRequestHandlingStart(long duration, TimeUnit timeUnit) {
            requestBacklog.decrementAndGet();
        }

        @Override
        protected void onNewRequestReceived() {
            requestBacklog.incrementAndGet();
            inflightRequests.incrementAndGet();
        }

        @Override
        protected void onConnectionHandlingFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
            HttpServerListener.this.onConnectionHandlingFailed(duration, timeUnit, throwable);
        }

        @Override
        protected void onConnectionHandlingSuccess(long duration, TimeUnit timeUnit) {
            HttpServerListener.this.onConnectionHandlingSuccess(duration, timeUnit);
        }

        @Override
        protected void onConnectionHandlingStart(long duration, TimeUnit timeUnit) {
            HttpServerListener.this.onConnectionHandlingStart(duration, timeUnit);
        }

        @Override
        protected void onNewClientConnected() {
            HttpServerListener.this.onNewClientConnected();
        }

        @Override
        protected void onByteRead(long bytesRead) {
            HttpServerListener.this.onByteRead(bytesRead);
        }

        @Override
        protected void onFlushFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
            HttpServerListener.this.onFlushFailed(duration, timeUnit, throwable);
        }

        @Override
        protected void onFlushSuccess(long duration, TimeUnit timeUnit) {
            HttpServerListener.this.onFlushSuccess(duration, timeUnit);
        }

        @Override
        protected void onFlushStart() {
            HttpServerListener.this.onFlushStart();
        }

        @Override
        protected void onWriteFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
            HttpServerListener.this.onWriteFailed(duration, timeUnit, throwable);
        }

        @Override
        protected void onWriteSuccess(long duration, TimeUnit timeUnit, long bytesWritten) {
            HttpServerListener.this.onWriteSuccess(duration, timeUnit, bytesWritten);
        }

        @Override
        protected void onWriteStart() {
            HttpServerListener.this.onWriteStart();
        }
    }
}
