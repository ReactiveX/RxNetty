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
import io.reactivex.netty.client.internal.SingleHostConnectionProvider;
import rx.Observable;
import rx.functions.Func1;

/**
 * A {@link ConnectionProviderFactory} that must only be used for a client that operates on a single host.
 */
public class SingleHostPoolingProviderFactory<W, R> implements ConnectionProviderFactory<W, R> {

    private final PoolConfig<W, R> config;

    private SingleHostPoolingProviderFactory(PoolConfig<W, R> config) {
        this.config = config;
    }

    @Override
    public ConnectionProvider<W, R> newProvider(Observable<HostConnector<W, R>> hosts) {
        return new SingleHostConnectionProvider<>(hosts.map(new Func1<HostConnector<W, R>, HostConnector<W, R>>() {
            @Override
            public HostConnector<W, R> call(HostConnector<W, R> hc) {
                return new HostConnector<>(hc, PooledConnectionProvider.create(config, hc));
            }
        }));
    }

    public static <W, R> SingleHostPoolingProviderFactory<W, R> createUnbounded() {
        return create(new PoolConfig<W, R>());
    }

    public static <W, R> SingleHostPoolingProviderFactory<W, R> createBounded(int maxConnections) {
        return create(new PoolConfig<W, R>().maxConnections(maxConnections));
    }

    public static <W, R> SingleHostPoolingProviderFactory<W, R> create(final PoolConfig<W, R> config) {
        return new SingleHostPoolingProviderFactory<>(config);
    }

}
