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
import io.reactivex.netty.protocol.tcp.client.events.TcpClientEventListener;
import io.reactivex.netty.spectator.internal.EventMetric;

import java.util.concurrent.TimeUnit;

import static io.reactivex.netty.spectator.internal.SpectatorUtils.*;

/**
 * TcpClientListener
 */
public class TcpClientListener extends TcpClientEventListener {

    private final EventMetric connection;
    private final EventMetric connectionClose;
    private final EventMetric poolAcquire;
    private final EventMetric poolRelease;
    private final Counter poolEvictions;
    private final Counter poolReuse;

    private final EventMetric write;
    private final EventMetric flush;

    private final Counter bytesRead;
    private final Counter bytesWritten;

    public TcpClientListener(Registry registry, String monitorId) {

        connection = new EventMetric(registry, "connection", monitorId, "action", "connect");
        connectionClose = new EventMetric(registry, "connection", monitorId, "action", "handle");
        poolAcquire = new EventMetric(registry, "connection.pool", monitorId, "action", "acquire");
        poolRelease = new EventMetric(registry, "connection.pool", monitorId, "action", "release");
        poolEvictions = newCounter(registry, "connection.pool", monitorId, "action", "evict");
        poolReuse = newCounter(registry, "connection.pool", monitorId, "action", "reuse");

        write = new EventMetric(registry, "writes", monitorId, "action", "write");
        flush = new EventMetric(registry, "writes", monitorId, "action", "flush");

        bytesWritten = newCounter(registry, "bytes", monitorId, "rtype", "count", "action", "write");
        bytesRead = newCounter(registry, "bytes", monitorId, "rtype", "count", "action", "read");
    }

    public TcpClientListener(String monitorId) {
        this(Spectator.globalRegistry(), monitorId);
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

    @Override
    public void onPoolReleaseFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        poolRelease.failure(duration, timeUnit);
    }

    @Override
    public void onPoolReleaseSuccess(long duration, TimeUnit timeUnit) {
        poolRelease.success(duration, timeUnit);
    }

    @Override
    public void onPoolReleaseStart() {
        poolRelease.start();
    }

    @Override
    public void onPooledConnectionEviction() {
        poolEvictions.increment();
    }

    @Override
    public void onPooledConnectionReuse() {
        poolReuse.increment();
    }

    @Override
    public void onPoolAcquireFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        poolAcquire.failure(duration, timeUnit);
    }

    @Override
    public void onPoolAcquireSuccess(long duration, TimeUnit timeUnit) {
        poolAcquire.success(duration, timeUnit);
    }

    @Override
    public void onPoolAcquireStart() {
        poolAcquire.start();
    }

    @Override
    public void onConnectionCloseFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        connectionClose.failure(duration, timeUnit);
    }

    @Override
    public void onConnectionCloseSuccess(long duration, TimeUnit timeUnit) {
        connectionClose.success(duration, timeUnit);
    }

    @Override
    public void onConnectionCloseStart() {
        connectionClose.start();
    }

    @Override
    public void onConnectFailed(long duration, TimeUnit timeUnit, Throwable throwable) {
        connection.failure(duration, timeUnit);
    }

    @Override
    public void onConnectSuccess(long duration, TimeUnit timeUnit) {
        connection.success(duration, timeUnit);
    }

    @Override
    public void onConnectStart() {
        connection.start();
    }
}
