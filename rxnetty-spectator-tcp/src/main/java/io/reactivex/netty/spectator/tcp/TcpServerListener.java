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

package io.reactivex.netty.spectator.tcp;

import com.netflix.spectator.api.Counter;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.Spectator;
import io.reactivex.netty.protocol.tcp.server.events.TcpServerEventListener;
import io.reactivex.netty.spectator.internal.EventMetric;

import java.util.concurrent.TimeUnit;

import static io.reactivex.netty.spectator.internal.SpectatorUtils.*;

/**
 * TcpServerListener.
 */
public class TcpServerListener extends TcpServerEventListener {

    private final Counter connectionAccept;

    private final EventMetric connectionHandling;
    private final EventMetric connectionClose;
    private final EventMetric write;
    private final EventMetric flush;

    private final Counter bytesRead;
    private final Counter bytesWritten;

    public TcpServerListener(Registry registry, String monitorId) {
        connectionAccept = newCounter(registry, "connection", monitorId, "rtype", "count",
                                      "action", "accept");
        connectionHandling = new EventMetric(registry, "connection", monitorId, "action", "handle");
        connectionClose = new EventMetric(registry, "connection", monitorId, "action", "close");

        write = new EventMetric(registry, "writes", monitorId, "action", "write");
        flush = new EventMetric(registry, "writes", monitorId, "action", "flush");

        bytesWritten = newCounter(registry, "bytes", monitorId, "rtype", "count", "action", "write");
        bytesRead = newCounter(registry, "bytes", monitorId, "rtype", "count", "action", "read");
    }

    public TcpServerListener(String monitorId) {
        this(Spectator.globalRegistry(), monitorId);
    }

    @Override
    public void onConnectionHandlingFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        connectionHandling.failure(duration, timeUnit);
    }

    @Override
    public void onConnectionHandlingSuccess(long duration, TimeUnit timeUnit) {
        connectionHandling.success(duration, timeUnit);
    }

    @Override
    public void onConnectionHandlingStart(long duration, TimeUnit timeUnit) {
        connectionHandling.start(duration, timeUnit);
    }

    @Override
    public void onConnectionCloseStart() {
        connectionClose.start();
    }

    @Override
    public void onConnectionCloseSuccess(long duration, TimeUnit timeUnit) {
        connectionClose.success(duration, timeUnit);
    }

    @Override
    public void onConnectionCloseFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        connectionClose.failure(duration, timeUnit);
    }

    @Override
    public void onNewClientConnected() {
        connectionAccept.increment();
    }

    @Override
    public void onByteRead(long bytesRead) {
        this.bytesRead.increment(bytesRead);
    }

    @Override
    public void onByteWritten(long bytesWritten) {
        this.bytesWritten.increment(bytesWritten);
    }

    @Override
    public void onFlushComplete(long duration, TimeUnit timeUnit) {
        flush.success(duration, timeUnit);
    }

    @Override
    public void onFlushStart() {
        flush.start();
    }

    @Override
    public void onWriteFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        write.failure(duration, timeUnit);
    }

    @Override
    public void onWriteSuccess(long duration, TimeUnit timeUnit) {
        write.success(duration, timeUnit);
    }

    @Override
    public void onWriteStart() {
        write.start();
    }
}
