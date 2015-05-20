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

import io.reactivex.netty.channel.pool.PooledConnection.Owner;
import io.reactivex.netty.client.PoolLimitDeterminationStrategy;
import io.reactivex.netty.protocol.tcp.client.ClientConnectionFactory;
import io.reactivex.netty.protocol.tcp.client.ClientState;

/**
 * An implementation of {@link io.reactivex.netty.protocol.tcp.client.ClientConnectionFactory} that pools connections. Configuration of the pool is as defined
 * by {@link PoolConfig} passed in with the {@link io.reactivex.netty.protocol.tcp.client.ClientState}.
 *
 * Following are the key parameters:
 *
 * <ul>
 <li>{@link PoolLimitDeterminationStrategy}: A stratgey to determine whether a new physical connection should be
 created as part of the user request.</li>
 <li>{@link PoolConfig#getIdleConnectionsCleanupTimer()}: The schedule for cleaning up idle connections in the pool.</li>
 <li>{@link PoolConfig#getMaxIdleTimeMillis()}: Maximum time a connection can be idle in this pool.</li>
 </ul>
 *
 * @author Nitesh Kant
 */
public abstract class PooledClientConnectionFactory<W, R> extends ClientConnectionFactory<W, R>
        implements Owner<R, W> {

    protected final PoolConfig<W, R> poolConfig;
    protected final ClientConnectionFactory<W, R> connectDelegate;

    protected PooledClientConnectionFactory(PoolConfig<W, R> poolConfig, ClientState<W, R> clientState,
                                            ClientConnectionFactory<W, R> delegate) {
        super(clientState);
        this.poolConfig = poolConfig;
        connectDelegate = delegate;
    }

    public ClientConnectionFactory<W, R> getConnectDelegate() {
        return connectDelegate;
    }
}
