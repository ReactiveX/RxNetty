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

/**
 * A contract for statistics maintained by the {@link ConnectionPool}
 *
 * @deprecated Use the {@link MetricEventsListener}s to get the metric.
 * @author Nitesh Kant
 */
@Deprecated
public interface PoolStats {

    long getInUseCount();

    long getIdleCount();

    long getTotalConnectionCount();

    long getPendingAcquireRequestCount();

    long getPendingReleaseRequestCount();
}
