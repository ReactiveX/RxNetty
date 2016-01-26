/*
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.netty.client.loadbalancer;

import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.client.ConnectionProvider;
import io.reactivex.netty.client.ConnectionProviderFactory;
import io.reactivex.netty.client.HostConnector;
import io.reactivex.netty.client.loadbalancer.HostCollector.HostUpdate;
import io.reactivex.netty.client.loadbalancer.HostCollector.HostUpdate.Action;
import io.reactivex.netty.internal.VoidToAnythingCast;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Single;
import rx.functions.Action1;
import rx.functions.Func1;

import java.util.List;

public class LoadBalancerFactory<W, R> implements ConnectionProviderFactory<W, R> {

    private static final Logger logger = LoggerFactory.getLogger(LoadBalancerFactory.class);

    private final LoadBalancingStrategy<W, R> strategy;
    private final HostCollector collector;

    private LoadBalancerFactory(LoadBalancingStrategy<W, R> strategy, HostCollector collector) {
        this.strategy = strategy;
        this.collector = collector;
    }

    @Override
    public ConnectionProvider<W, R> newProvider(Observable<HostConnector<W, R>> hosts) {

        return new ConnectionProviderImpl(hosts.map(new Func1<HostConnector<W, R>, HostHolder<W, R>>() {
            @Override
            public HostHolder<W, R> call(HostConnector<W, R> connector) {
                HostHolder<W, R> newHolder = strategy.toHolder(connector);
                connector.subscribe(newHolder.getEventListener());
                return newHolder;
            }
        }).flatMap(new Func1<HostHolder<W, R>, Observable<HostUpdate<W, R>>>() {
            @Override
            public Observable<HostUpdate<W, R>> call(HostHolder<W, R> holder) {
                return holder.getConnector()
                             .getHost()
                             .getCloseNotifier()
                             .map(new VoidToAnythingCast<HostUpdate<W, R>>())
                             .ignoreElements()
                             .onErrorResumeNext(Observable.<HostUpdate<W, R>>empty())
                             .concatWith(Observable.just(new HostUpdate<>(Action.Remove, holder)))
                             .mergeWith(Observable.just(new HostUpdate<>(Action.Add, holder)));
            }
        }).flatMap(newCollector(collector.<W, R>newCollector()), 1).distinctUntilChanged());
    }

    public static <WW, RR> LoadBalancerFactory<WW, RR> create(LoadBalancingStrategy<WW, RR> strategy) {
        return create(strategy, new NoBufferHostCollector());
    }

    public static <WW, RR> LoadBalancerFactory<WW, RR> create(LoadBalancingStrategy<WW, RR> strategy,
                                                              HostCollector collector) {
        return new LoadBalancerFactory<>(strategy, collector);
    }

    private class ConnectionProviderImpl implements ConnectionProvider<W, R> {

        private volatile ConnectionProvider<W, R> currentProvider = new ConnectionProvider<W, R>() {
            @Override
            public Observable<Connection<R, W>> newConnectionRequest() {
                return Observable.error(NoHostsAvailableException.EMPTY_INSTANCE);
            }
        };

        public ConnectionProviderImpl(Observable<List<HostHolder<W, R>>> hosts) {
            hosts.subscribe(new Action1<List<HostHolder<W, R>>>() {
                @Override
                public void call(List<HostHolder<W, R>> hostHolders) {
                    currentProvider = strategy.newStrategy(hostHolders);
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    logger.error("Error while listening on the host stream. Hosts will not be refreshed.", throwable);
                }
            });
        }

        @Override
        public Observable<Connection<R, W>> newConnectionRequest() {
            return currentProvider.newConnectionRequest();
        }
    }

    private Func1<? super HostUpdate<W, R>, ? extends Observable<List<HostHolder<W, R>>>>
    newCollector(final Func1<HostUpdate<W, R>, Single<List<HostHolder<W, R>>>> f) {
        return new Func1<HostUpdate<W, R>, Observable<List<HostHolder<W, R>>>>() {
            @Override
            public Observable<List<HostHolder<W, R>>> call(HostUpdate<W, R> holder) {
                return f.call(holder).toObservable();
            }
        };
    }
}
