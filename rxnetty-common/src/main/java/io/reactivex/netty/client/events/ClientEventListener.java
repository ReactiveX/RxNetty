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

package io.reactivex.netty.client.events;

import io.reactivex.netty.channel.events.ConnectionEventListener;

import java.util.concurrent.TimeUnit;

public class ClientEventListener extends ConnectionEventListener {
    /**
     * Event whenever a new connection attempt is made.
     */
    @SuppressWarnings("unused")
    public void onConnectStart() {}

    /**
     * Event whenever a new connection is successfully established.
     *
     * @param duration Duration between connect start and completion.
     * @param timeUnit Timeunit for the duration.
     */
    @SuppressWarnings("unused")
    public void onConnectSuccess(long duration, TimeUnit timeUnit) {}

    /**
     * Event whenever a connect attempt failed.
     *
     * @param duration Duration between connect start and failure.
     * @param timeUnit Timeunit for the duration.
     * @param throwable Error that caused the failure.
     */
    @SuppressWarnings("unused")
    public void onConnectFailed(long duration, TimeUnit timeUnit, Throwable throwable) {}

    /**
     * Event whenever a connection release to the pool is initiated (by closing the connection)
     */
    @SuppressWarnings("unused")
    public void onPoolReleaseStart() {}

    /**
     * Event whenever a connection is successfully released to the pool.
     *
     * @param duration Duration between release start and completion.
     * @param  timeUnit Timeunit for the duration.
     */
    @SuppressWarnings("unused")
    public void onPoolReleaseSuccess(long duration, TimeUnit timeUnit) {}

    /**
     * Event whenever a connection release to pool fails.
     *
     * @param duration Duration between release start and failure.
     * @param timeUnit Timeunit for the duration.
     * @param throwable Error that caused the failure.
     */
    @SuppressWarnings("unused")
    public void onPoolReleaseFailed(long duration, TimeUnit timeUnit, Throwable throwable) {}

    /**
     * Event whenever an idle connection is removed/evicted from the pool.
     */
    @SuppressWarnings("unused")
    public void onPooledConnectionEviction() {}

    /**
     * Event whenever a connection is reused from the pool.
     */
    @SuppressWarnings("unused")
    public void onPooledConnectionReuse() {}

    /**
     * Event whenever an acquire from the pool is initiated.
     */
    @SuppressWarnings("unused")
    public void onPoolAcquireStart() {}

    /**
     * Event whenever an acquire from the pool is successful.
     *
     * @param duration Duration between acquire start and completion.
     * @param timeUnit Timeunit for the duration.
     */
    @SuppressWarnings("unused")
    public void onPoolAcquireSuccess(long duration, TimeUnit timeUnit) {}

    /**
     * Event whenever an acquire from the pool failed.
     *
     * @param duration Duration between acquire start and failure.
     * @param timeUnit Timeunit for the duration.
     * @param throwable Error that caused the failure.
     */
    @SuppressWarnings("unused")
    public void onPoolAcquireFailed(long duration, TimeUnit timeUnit, Throwable throwable) {}
}
