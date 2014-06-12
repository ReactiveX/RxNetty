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
import io.reactivex.netty.protocol.http.client.HttpClientMetricsEvent;
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
public class HttpClientListener extends TcpClientListener<HttpClientMetricsEvent<?>> {

    private final LongGauge requestBacklog;
    private final LongGauge inflightRequests;
    private final Counter processedRequests;
    private final Counter requestWriteFailed;
    private final Timer requestWriteTimes;
    private final Timer responseReadTimes;

    protected HttpClientListener(String monitorId) {
        super(monitorId);
        requestBacklog = newLongGauge("inflightRequests");
        inflightRequests = newLongGauge("inflightRequests");
        requestWriteTimes = newTimer("requestWriteTimes");
        responseReadTimes = newTimer("responseReadTimes");
        processedRequests = newCounter("processedRequests");
        requestWriteFailed = newCounter("requestWriteFailed");
    }

    @Override
    public void onEvent(HttpClientMetricsEvent<?> event, long duration, TimeUnit timeUnit, Throwable throwable,
                        Object value) {
        if (event.getType() instanceof ClientMetricsEvent.EventType) {
            super.onEvent(event, duration, timeUnit, throwable, value);
        } else {
            switch ((HttpClientMetricsEvent.EventType) event.getType()) {
                case RequestSubmitted:
                    incrementLongGauge(requestBacklog);
                    incrementLongGauge(inflightRequests);
                    break;
                case RequestHeadersWriteStart:
                    decrementLongGauge(requestBacklog);
                    break;
                case RequestHeadersWriteSuccess:
                    break;
                case RequestHeadersWriteFailed:
                    requestWriteFailed.increment();
                    break;
                case RequestContentWriteStart:
                    break;
                case RequestContentWriteSuccess:
                    break;
                case RequestContentWriteFailed:
                    requestWriteFailed.increment();
                    break;
                case RequestWriteComplete:
                    requestWriteTimes.record(duration, timeUnit);
                    break;
                case ResponseHeadersReceived:
                    break;
                case ResponseContentReceived:
                    break;
                case ResponseReceiveComplete:
                    decrementLongGauge(inflightRequests);
                    processedRequests.increment();
                    responseReadTimes.record(duration, timeUnit);
                    break;
            }
        }
    }

    public static HttpClientListener newHttpListener(String monitorId) {
        return new HttpClientListener(monitorId);
    }
}
