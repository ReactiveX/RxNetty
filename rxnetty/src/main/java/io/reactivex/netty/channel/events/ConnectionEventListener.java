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
package io.reactivex.netty.channel.events;

import io.reactivex.netty.events.EventListener;

import java.util.concurrent.TimeUnit;

/**
 * An event listener for all events releated to a {@link io.reactivex.netty.channel.Connection}
 */
public abstract class ConnectionEventListener implements EventListener {

    /**
     * Event whenever any bytes are read on any open connection.
     *
     * @param bytesRead Number of bytes read.
     */
    @SuppressWarnings("unused")
    public void onByteRead(long bytesRead) { }

    /**
     * Event whenever a flush is issued on a connection.
     */
    public void onFlushStart() {}

    /**
     * Event whenever flush succeeds.
     *
     * @param duration Duration between flush start and completion.
     * @param timeUnit Timeunit for the duration.
     */
    @SuppressWarnings("unused")
    public void onFlushSuccess(long duration, TimeUnit timeUnit) {}

    /**
     * Event whenever flush fails.
     *
     * @param duration Duration between flush start and failure.
     * @param timeUnit Timeunit for the duration.
     * @param throwable Error that caused the failure.
     */
    @SuppressWarnings("unused")
    public void onFlushFailed(long duration, TimeUnit timeUnit, Throwable throwable) {}

    /**
     * Event whenever a write is issued on a connection.
     */
    public void onWriteStart() {}

    /**
     * Event whenever data is written successfully on a connection.
     *
     * @param duration Duration between write start and completion.
     * @param timeUnit Timeunit for the duration.
     * @param bytesWritten Number of bytes written.
     */
    @SuppressWarnings("unused")
    public void onWriteSuccess(long duration, TimeUnit timeUnit, long bytesWritten) {}

    /**
     * Event whenever a write failed on a connection.
     *
     * @param duration Duration between write start and failure.
     * @param timeUnit Timeunit for the duration.
     * @param throwable Error that caused the failure..
     */
    @SuppressWarnings("unused")
    public void onWriteFailed(long duration, TimeUnit timeUnit, Throwable throwable) {}

    /**
     * Event whenever a close of any connection is issued. This event will only be fired when the physical connection
     * is closed and not when a pooled connection is closed and put back in the pool.
     */
    @SuppressWarnings("unused")
    public void onConnectionCloseStart() {}

    /**
     * Event whenever a close of any connection is successful.
     *
     * @param duration Duration between close start and completion.
     * @param timeUnit Timeunit for the duration.
     */
    @SuppressWarnings("unused")
    public void onConnectionCloseSuccess(long duration, TimeUnit timeUnit) {}

    /**
     * Event whenever a connection close failed.
     *
     * @param duration Duration between close start and failure.
     * @param timeUnit Timeunit for the duration.
     * @param throwable Error that caused the failure.
     */
    @SuppressWarnings("unused")
    public void onConnectionCloseFailed(long duration, TimeUnit timeUnit, Throwable throwable) {}

    @Override
    public void onCompleted() { }

    @Override
    public void onSubscribe() { }
}
