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
package io.reactivex.netty.client;

import rx.Observable;

/**
 * An interface providing insights into the connection pool. This essentially separates read from write operations of a
 * {@link ConnectionPool}
 *
 * @deprecated Use {@link io.reactivex.netty.metrics.MetricEventsPublisher} instead.
 *
 * @author Nitesh Kant
 */
@Deprecated
public interface PoolInsightProvider {

    /**
     * Returns the {@link Observable} that emits any changes to the state of the pool as
     * {@link PoolInsightProvider.PoolStateChangeEvent}
     *
     * @return An {@link Observable} emitting all state change events to the pool.
     */
    Observable<PoolStateChangeEvent> poolStateChangeObservable();

    PoolStats getStats();

    /**
     * @deprecated Use {@link io.reactivex.netty.metrics.MetricsEvent} instead.
     */
    @Deprecated
    enum PoolStateChangeEvent {
        NewConnectionCreated,
        ConnectFailed,
        OnConnectionReuse,
        OnConnectionEviction,
        onAcquireAttempted,
        onAcquireSucceeded,
        onAcquireFailed,
        onReleaseAttempted,
        onReleaseSucceeded,
        onReleaseFailed
    }
}
