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

import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.tcp.client.events.TcpClientEventListener;

import java.util.concurrent.TimeUnit;

/**
 * A listener for all events published by {@link HttpClient}
 */
public abstract class HttpClientEventsListener extends TcpClientEventListener {

    /**
     * Event when a new request is submitted for the client.
     */
    public void onRequestSubmitted() {}

    /**
     * Event when the write of request started.
     */
    public void onRequestWriteStart() {}

    /**
     * Event when a request write is completed.
     *
     * @param duration Time taken from the start of write to completion.
     * @param timeUnit Time unit for the duration.
     */
    @SuppressWarnings("unused")
    public void onRequestWriteComplete(long duration, TimeUnit timeUnit) {}

    /**
     * Event when a request write failed.
     *
     * @param duration Time taken from the start of write to failure.
     * @param timeUnit Time unit for the duration.
     * @param throwable Error that caused the failure.
     */
    @SuppressWarnings("unused")
    public void onRequestWriteFailed(long duration, TimeUnit timeUnit, Throwable throwable) {}

    /**
     * Event when the response headers are received.
     *
     * @param responseCode The HTTP response code.
     */
    @SuppressWarnings("unused")
    public void onResponseHeadersReceived(int responseCode) {}

    /**
     * Event whenever an HTTP response content is received (an HTTP response can have multiple content chunks, in which
     * case this event will be fired as many times for the same response).
     */
    public void onResponseContentReceived() {}

    /**
     * Event when the response receive is completed.
     *
     * @param duration Time taken between receiving the response headers and completion of response.
     * @param timeUnit Time unit for the duration.
     */
    @SuppressWarnings("unused")
    public void onResponseReceiveComplete(long duration, TimeUnit timeUnit) {}

    /**
     * Event when the response failed (either it did not arrive or not arrived completely)
     *
     * @param throwable Error that caused the failure.
     */
    @SuppressWarnings("unused")
    public void onResponseFailed(Throwable throwable) {}

    /**
     * Event when the entire request processing (request submitted to response failed/complete) is completed.
     *
     * @param duration Time taken from submission of request to completion.
     * @param timeUnit Time unit for the duration.
     */
    @SuppressWarnings("unused")
    public void onRequestProcessingComplete(long duration, TimeUnit timeUnit) {}
}
