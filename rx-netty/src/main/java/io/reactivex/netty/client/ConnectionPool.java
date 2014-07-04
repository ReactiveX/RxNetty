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

import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.metrics.MetricEventsPublisher;
import rx.Observable;

/**
 * A pool of {@link PooledConnection}
 *
 * @author Nitesh Kant
 */
public interface ConnectionPool<I, O> extends MetricEventsPublisher<ClientMetricsEvent<?>> {

    Observable<ObservableConnection<I, O>> acquire();

    Observable<Void> release(PooledConnection<I, O> connection);

    /**
     * Discards the passed connection from the pool. This is usually called due to an external event like closing of
     * a connection that the pool may not know. <br/>
     * <b> This operation is idempotent and hence can be called multiple times with no side effects</b>
     *
     * @param connection The connection to discard.
     *
     * @return Observable indicating the completion of the discard operation. Since, this operation is idempotent, the
     * returned observable does not indicate throw an error if the eviction of this connection was not due to that
     * particular invocation.
     */
    Observable<Void> discard(PooledConnection<I, O> connection);

    void shutdown();
}
