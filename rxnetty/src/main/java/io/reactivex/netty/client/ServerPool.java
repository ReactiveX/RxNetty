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
package io.reactivex.netty.client;

import io.reactivex.netty.metrics.MetricEventsListener;
import io.reactivex.netty.metrics.MetricsEvent;
import rx.Observable;
import rx.annotations.Experimental;

import java.net.SocketAddress;
import java.util.NoSuchElementException;

/**
 * A pool of servers that returns a "best-suited" {@link Server} every time {@link #next()} is invoked.
 *
 * Typical use of this pool is to implement a load-balancer for distributing load over a pool of target servers.
 */
@Experimental
public interface ServerPool<M extends MetricsEvent<?>> {

    /**
     * Returns a {@link Server} instance upon each invocation.
     *
     * @return A server.
     *
     * @throws NoSuchElementException If there is no server available in the pool.
     */
    Server<M> next();

    /**
     * A contract for a server returned by {@link ServerPool}.
     *
     * @param <M>
     */
    interface Server<M extends MetricsEvent<?>> extends MetricEventsListener<M> {

        /**
         * The address for this server.
         *
         * @return The socket address for this server.
         */
        SocketAddress getAddress();

        /**
         * An {@link Observable} representing the lifecycle of this server. A terminal event on this {@link Observable}
         * would indicate removal of the server from the pool. This would be an opportunity for the client using this
         * {@link ServerPool} to cleanup resources associated to this server.
         *
         * @return An {@link Observable} representing the lifecycle of this server.
         */
        Observable<Void> getLifecycle();
    }
}
