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
package io.reactivex.netty.examples.tcp.loadbalancing;

import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.channel.pool.PooledConnectionProvider;
import io.reactivex.netty.protocol.tcp.client.ConnectionFactory;
import io.reactivex.netty.protocol.tcp.client.ConnectionObservable;
import io.reactivex.netty.protocol.tcp.client.ConnectionObservable.AbstractOnSubscribeFunc;
import io.reactivex.netty.protocol.tcp.client.ConnectionProvider;
import io.reactivex.netty.protocol.tcp.client.events.TcpClientEventListener;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Actions;
import rx.functions.Func1;
import rx.subjects.ReplaySubject;

import java.net.BindException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A simple round-robin load balancer that
 *
 * @param <W> Type of Objects written on the connections created by this load balancer.
 * @param <R> Type of Objects read from the connections created by this load balancer.
 */
public abstract class RoundRobinLoadBalancer<W, R> extends ConnectionProvider<W, R> {

    private final AtomicReference<List<HostHolder>> hostConnectionProviders;
    private final Func1<Action0, TcpClientEventListener> failureDetetctorFactory;
    private final AtomicInteger currentIndex;
    private final ReplaySubject<ConnectionProvider<W, R>> hostRemover;

    protected RoundRobinLoadBalancer(SocketAddress[] hosts, ConnectionFactory<W, R> cf,
                                     Func1<Action0, TcpClientEventListener> fdFactory) {
        super(cf);
        hostConnectionProviders = new AtomicReference<>();

        hostRemover = ReplaySubject.create();
        failureDetetctorFactory = fdFactory;
        List<HostHolder> _hostConnectionProviders = new ArrayList<>(hosts.length);
        for (SocketAddress host : hosts) {
            final PooledConnectionProvider<W, R> hostConnProvider = PooledConnectionProvider.createUnbounded(cf, host);
            TcpClientEventListener fd = registerNewHost(hostConnProvider);
            _hostConnectionProviders.add(new HostHolder(hostConnProvider, fd));
        }
        hostConnectionProviders.set(_hostConnectionProviders);

        hostRemover.serialize()
                   .doOnNext(hostToRemove -> {
                       final List<HostHolder> cps = hostConnectionProviders.get();
                       final List<HostHolder> copyCps = new ArrayList<>(cps.size() - 1);
                       cps.stream().forEach(toAdd -> {
                           if (toAdd.connectionProvider != hostToRemove) {
                               copyCps.add(toAdd);
                           }
                       });
                       hostConnectionProviders.set(copyCps);
                   })
                   .ignoreElements()
                   .subscribe(Actions.empty(), Throwable::printStackTrace);


        currentIndex = new AtomicInteger();
    }

    private TcpClientEventListener registerNewHost(ConnectionProvider<W, R> connectionProvider) {
        return failureDetetctorFactory.call(() -> hostRemover.onNext(connectionProvider));
    }

    @Override
    public ConnectionObservable<R, W> nextConnection() {
        return ConnectionObservable.createNew(new AbstractOnSubscribeFunc<R, W>() {
            @Override
            protected void doSubscribe(Subscriber<? super Connection<R, W>> sub,
                                       Action1<ConnectionObservable<R, W>> subscribeAllListenersAction) {
                _connect(subscribeAllListenersAction)
                        .retry((count, th) -> th instanceof BindException)
                        .unsafeSubscribe(sub);
            }
        });
    }

    private Observable<Connection<R, W>> _connect(Action1<ConnectionObservable<R, W>> subscribeAllListenersAction) {
        return Observable.create(subscriber -> {
            final List<HostHolder> hostHolders = hostConnectionProviders.get();

            if (hostHolders.isEmpty()) {
                subscriber.onError(new NoSuchElementException("No host available."));
            }

            final int factoriesSize = hostHolders.size();
            final int index = Math.abs(currentIndex.incrementAndGet() % factoriesSize);
            HostHolder holderToUse = hostHolders.get(index);
            ConnectionObservable<R, W> co = holderToUse.connectionProvider.nextConnection();
            co.subscribeForEvents(holderToUse.failureDetector);
            subscribeAllListenersAction.call(co);
            co.unsafeSubscribe(subscriber);
        });
    }

    private class HostHolder {

        private final PooledConnectionProvider<W, R> connectionProvider;
        private final TcpClientEventListener failureDetector;

        private HostHolder(PooledConnectionProvider<W, R> connectionProvider, TcpClientEventListener failureDetector) {
            this.connectionProvider = connectionProvider;
            this.failureDetector = failureDetector;
        }
    }
}
