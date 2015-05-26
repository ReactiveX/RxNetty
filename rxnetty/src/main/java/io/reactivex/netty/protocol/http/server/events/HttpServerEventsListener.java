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
package io.reactivex.netty.protocol.http.server.events;

import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.tcp.server.events.TcpServerEventListener;

import java.util.concurrent.TimeUnit;

/**
 * A listener for all events published by {@link HttpServer}
 */
public abstract class HttpServerEventsListener extends TcpServerEventListener {

    /**
     * Event whenever a new request is received by the server.
     */
    public void onNewRequestReceived() {}

    /**
     * When request handling started.
     *
     * @param duration Time between the receiving request and start of processing.
     * @param timeUnit Time unit for the duration.
     */
    @SuppressWarnings("unused")
    public void onRequestHandlingStart(long duration, TimeUnit timeUnit) { }

    /**
     * When request handling completes successfully.
     *
     * @param duration Time between the request processing start and completion.
     * @param timeUnit Time unit for the duration.
     */
    @SuppressWarnings("unused")
    public void onRequestHandlingSuccess(long duration, TimeUnit timeUnit) {}

    /**
     * When request handling completes with an error.
     *
     * @param duration Time between the request processing start and failure.
     * @param timeUnit Time unit for the duration.
     * @param throwable Error that caused the failure.
     */
    @SuppressWarnings("unused")
    public void onRequestHandlingFailed(long duration, TimeUnit timeUnit, Throwable throwable) {}

    /**
     * Whenever request headers are received.
     */
    public void onRequestHeadersReceived() {}

    /**
     * Event whenever an HTTP request content is received (an HTTP request can have multiple content chunks, in which
     * case this event will be fired as many times for the same request).
     */
    public void onRequestContentReceived() {}

    /**
     * Event when the request receive is completed.
     *
     * @param duration Time taken between receiving the request headers and completion of request.
     * @param timeUnit Time unit for the duration.
     */
    @SuppressWarnings("unused")
    public void onRequestReceiveComplete(long duration, TimeUnit timeUnit) {}

    /**
     * Event when the response write starts.
     */
    @SuppressWarnings("unused")
    public void onResponseWriteStart() {}

    /**
     * Event when the response write is completed successfully.
     *
     * @param duration Time taken between write start and completion.
     * @param timeUnit Time unit for the duration.
     * @param responseCode HTTP response code for the response.
     */
    @SuppressWarnings("unused")
    public void onResponseWriteSuccess(long duration, TimeUnit timeUnit, int responseCode) {}

    /**
     * Event when the response write is completed with an error.
     *
     * @param duration Time taken between write start and completion.
     * @param timeUnit Time unit for the duration.
     * @param throwable Error that caused the failure.
     */
    @SuppressWarnings("unused")
    public void onResponseWriteFailed(long duration, TimeUnit timeUnit, Throwable throwable) {}
}
