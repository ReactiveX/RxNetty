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
package io.reactivex.netty.client.pool;

import io.reactivex.netty.client.ConnectionProvider;
import io.reactivex.netty.client.ConnectionProviderFactory;
import io.reactivex.netty.client.HostConnector;
import io.reactivex.netty.client.pool.PooledConnection.Owner;

/**
 * An implementation of {@link PooledConnectionProvider} that pools connections.
 *
 * Following are the key parameters:
 *
 * <ul>
 <li>{@link PoolLimitDeterminationStrategy}: A strategy to determine whether a new physical connection should be
 created as part of the user request.</li>
 <li>{@link PoolConfig#getIdleConnectionsCleanupTimer()}: The schedule for cleaning up idle connections in the pool.</li>
 <li>{@link PoolConfig#getMaxIdleTimeMillis()}: Maximum time a connection can be idle in this pool.</li>
 </ul>
 *
 * <h2>Usage</h2>
 *
 * <h4>Complementing a {@link ConnectionProviderFactory}</h4>
 *
 * For employing better host selection strategies, this provider can be used to complement the default
 * {@link ConnectionProvider} provided by a {@link HostConnector}.
 *
 * <h4>Standalone</h4>
 *
 * For clients that do not use a pool of hosts can use {@link SingleHostPoolingProviderFactory} that will only ever pick
 * a single host but will pool connections.
 */
public abstract class PooledConnectionProvider<W, R> implements ConnectionProvider<W, R> , Owner<R, W> {

    public static <W, R> PooledConnectionProvider<W, R> createUnbounded(final HostConnector<W, R> delegate) {
        return create(new PoolConfig<W, R>(), delegate);
    }

    public static <W, R> PooledConnectionProvider<W, R> createBounded(int maxConnections,
                                                                      final HostConnector<W, R> delegate) {
        return create(new PoolConfig<W, R>().maxConnections(maxConnections), delegate);
    }

    public static <W, R> PooledConnectionProvider<W, R> create(final PoolConfig<W, R> config,
                                                               final HostConnector<W, R> delegate) {
        return new PooledConnectionProviderImpl<>(config, delegate);
    }
}
