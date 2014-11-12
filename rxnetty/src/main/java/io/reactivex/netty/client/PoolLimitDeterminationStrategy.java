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

import io.reactivex.netty.metrics.MetricEventsListener;

import java.util.concurrent.TimeUnit;

/**
 * A strategy to delegate the decision pertaining to {@link ConnectionPool} size limits.
 *
 * @author Nitesh Kant
 */
public interface PoolLimitDeterminationStrategy extends MetricEventsListener<ClientMetricsEvent<?>> {

    /**
     * Attempts to acquire a creation permit.
     *
     * @param acquireStartTime The start time for the acquire process in milliseconds since epoch.
     * @param timeUnit The timeunit for the acquire start time.
     *
     * @return {@code true} if the permit was acquired, {@code false} otherwise.
     */
    boolean acquireCreationPermit(long acquireStartTime, TimeUnit timeUnit);

    int getAvailablePermits();
}
