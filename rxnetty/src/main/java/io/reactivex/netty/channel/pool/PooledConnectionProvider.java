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
package io.reactivex.netty.channel.pool;

import io.reactivex.netty.channel.client.PoolLimitDeterminationStrategy;
import io.reactivex.netty.channel.pool.PooledConnection.Owner;
import io.reactivex.netty.protocol.tcp.client.ClientState;
import io.reactivex.netty.protocol.tcp.client.ConnectionFactory;
import io.reactivex.netty.protocol.tcp.client.ConnectionObservable;
import io.reactivex.netty.protocol.tcp.client.ConnectionProvider;
import rx.functions.Func1;

import java.net.SocketAddress;

/**
 * An implementation of {@link PooledConnectionProvider} that pools connections. Configuration of the pool is as defined
 * by {@link PoolConfig} passed in with the {@link ClientState}.
 *
 * Following are the key parameters:
 *
 * <ul>
 <li>{@link PoolLimitDeterminationStrategy}: A stratgey to determine whether a new physical connection should be
 created as part of the user request.</li>
 <li>{@link PoolConfig#getIdleConnectionsCleanupTimer()}: The schedule for cleaning up idle connections in the pool.</li>
 <li>{@link PoolConfig#getMaxIdleTimeMillis()}: Maximum time a connection can be idle in this pool.</li>
 </ul>
 */
public abstract class PooledConnectionProvider<W, R> extends ConnectionProvider<W, R> implements Owner<R, W> {

    protected PooledConnectionProvider(ConnectionFactory<W, R> connectionFactory) {
        super(connectionFactory);
    }

    @Override
    public abstract ConnectionObservable<R, W> nextConnection();

    public static <W, R> PooledConnectionProvider<W, R> createUnbounded(ConnectionFactory<W, R> bootstrap,
                                                                        SocketAddress remoteAddress) {
        return new PooledConnectionProviderImpl<>(bootstrap, new PoolConfig<W, R>(), remoteAddress);
    }

    public static <W, R> PooledConnectionProvider<W, R> createBounded(int maxConnections,
                                                                      ConnectionFactory<W, R> bootstrap,
                                                                      SocketAddress remoteAddress) {
        return new PooledConnectionProviderImpl<>(bootstrap, new PoolConfig<W, R>().maxConnections(maxConnections),
                                                  remoteAddress);
    }

    public static <W, R> ConnectionProvider<W, R> createUnbounded(final SocketAddress remoteAddress) {
        return create(new PoolConfig<W, R>(), remoteAddress);
    }

    public static <W, R> ConnectionProvider<W, R> createBounded(int maxConnections, SocketAddress remoteAddress) {
        return create(new PoolConfig<W, R>().maxConnections(maxConnections), remoteAddress);
    }

    public static <W, R> PooledConnectionProvider<W, R> create(PoolConfig<W, R> config,
                                                               ConnectionFactory<W, R> bootstrap,
                                                               SocketAddress remoteAddress) {
        return new PooledConnectionProviderImpl<>(bootstrap, config, remoteAddress);
    }

    public static <W, R> ConnectionProvider<W, R> create(final PoolConfig<W, R> config,
                                                         final SocketAddress remoteAddress) {
        return ConnectionProvider.create(new Func1<ConnectionFactory<W, R>, ConnectionProvider<W, R>>() {
            @Override
            public ConnectionProvider<W, R> call(ConnectionFactory<W, R> cf) {
                return new PooledConnectionProviderImpl<>(cf, config, remoteAddress);
            }
        });
    }
}
