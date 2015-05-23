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
package io.reactivex.netty.protocol.tcp.server.events;

import io.reactivex.netty.channel.events.ConnectionEventListener;
import io.reactivex.netty.protocol.tcp.server.TcpServer;

import java.util.concurrent.TimeUnit;

/**
 * A listener for all events published by {@link TcpServer}
 */
public abstract class TcpServerEventListener extends ConnectionEventListener {

    /**
     * Event whenever a new client connection is accepted.
     */
    protected void onNewClientConnected() { }

    /**
     * Event when any connection handling starts.
     *
     * @param duration Time between a client connection is accepted to when it is handled by the connection handler.
     * @param timeUnit Time unit for the duration.
     */
    @SuppressWarnings("unused")
    public void onConnectionHandlingStart(long duration, TimeUnit timeUnit) { }

    /**
     * Event when any connection handling is successfully completed.
     *
     * @param duration Time taken for connection handling.
     * @param timeUnit Time unit for the duration.
     */
    @SuppressWarnings("unused")
    public void onConnectionHandlingSuccess(long duration, TimeUnit timeUnit) {}

    /**
     * Event when any connection handling completes with an error.
     *
     * @param duration Time taken for connection handling.
     * @param timeUnit Time unit for the duration.
     */
    @SuppressWarnings("unused")
    public void onConnectionHandlingFailed(long duration, TimeUnit timeUnit, Throwable throwable) { }
}
