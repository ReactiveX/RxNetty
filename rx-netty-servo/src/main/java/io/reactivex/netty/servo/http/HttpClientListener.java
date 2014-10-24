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

package io.reactivex.netty.servo.http;

import com.netflix.servo.monitor.Counter;
import com.netflix.servo.monitor.LongGauge;
import com.netflix.servo.monitor.Timer;
import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.metrics.HttpClientMetricEventsListener;
import io.reactivex.netty.servo.tcp.TcpClientListener;

import java.util.concurrent.TimeUnit;

import static com.netflix.servo.monitor.Monitors.newCounter;
import static com.netflix.servo.monitor.Monitors.newTimer;
import static io.reactivex.netty.servo.ServoUtils.decrementLongGauge;
import static io.reactivex.netty.servo.ServoUtils.incrementLongGauge;
import static io.reactivex.netty.servo.ServoUtils.newLongGauge;

/**
 * @author Nitesh Kant
 */
public class HttpClientListener extends TcpClientListener<ClientMetricsEvent<?>> {

    private final LongGauge requestBacklog;
    private final LongGauge inflightRequests;
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
        requestBacklog = newLongGauge("requestBacklog");
        inflightRequests = newLongGauge("inflightRequests");
        requestWriteTimes = newTimer("requestWriteTimes");
        responseReadTimes = newTimer("responseReadTimes");
        processedRequests = newCounter("processedRequests");
        requestWriteFailed = newCounter("requestWriteFailed");
        failedResponses = newCounter("failedResponses");
        failedContentSource = newCounter("failedContentSource");
        requestProcessingTimes = newTimer("requestProcessingTimes");
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
        return requestBacklog.getNumber().get();
    }

    public long getInflightRequests() {
        return inflightRequests.getNumber().get();
    }

    public long getProcessedRequests() {
        return processedRequests.getValue().longValue();
    }

    public long getRequestWriteFailed() {
        return requestWriteFailed.getValue().longValue();
    }

    public long getFailedResponses() {
        return failedResponses.getValue().longValue();
    }

    public long getFailedContentSource() {
        return failedContentSource.getValue().longValue();
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
            decrementLongGauge(inflightRequests);
            processedRequests.increment();
            responseReadTimes.record(duration, timeUnit);
        }

        @Override
        protected void onResponseFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
            decrementLongGauge(inflightRequests);
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
            decrementLongGauge(inflightRequests);
            requestWriteFailed.increment();
        }

        @Override
        protected void onRequestHeadersWriteStart() {
            decrementLongGauge(requestBacklog);
        }

        @Override
        protected void onRequestSubmitted() {
            incrementLongGauge(requestBacklog);
            incrementLongGauge(inflightRequests);
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
