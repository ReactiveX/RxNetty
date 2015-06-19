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
import rx.functions.Func1;
import rx.functions.Func2;
import rx.subjects.PublishSubject;

import java.net.BindException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A simple round-robin load balancer that round-robins between the available host and uses a failure detector to remove
 * hosts from the active host list. A failure detector is implemented using a {@link TcpClientEventListener}
 * implementation to choose various ways in which unhealthy hosts can be identified.
 *
 * @param <W> Type of Objects written on the connections created by this load balancer.
 * @param <R> Type of Objects read from the connections created by this load balancer.
 */
public abstract class RoundRobinLoadBalancer<W, R> extends ConnectionProvider<W, R> {

    /*A list of active hosts, which is updated when the failure detects that a host is unhealthy*/
    private final AtomicReference<List<HostHolder>> activeHosts;

    /*A Failure detector factory that returns an event listener to be subscribed to the events pertaining to a host*/
    private final Func1<Action0, TcpClientEventListener> failureDetetctorFactory;

    /*An index to the active host list that selects the next host to be used*/
    private final AtomicInteger currentHostIndex;

    /*A subject that publishes a ConnectionProvider for a specific host to be removed.*/
    private final PublishSubject<ConnectionProvider<W, R>> hostRemover;

    protected RoundRobinLoadBalancer(SocketAddress[] hosts, ConnectionFactory<W, R> cf,
                                     Func1<Action0, TcpClientEventListener> fdFactory) {
        super(cf);
        activeHosts = new AtomicReference<>();

        hostRemover = PublishSubject.create();
        failureDetetctorFactory = fdFactory;

        List<HostHolder> _hostConnectionProviders = new ArrayList<>(hosts.length);

        /*Iterate over all the provided hosts and create a pooled connection provider for each host. Also, create the
        * failure detector for that host*/
        for (SocketAddress host : hosts) {
            /*Creates an unbounded pool as the bounds should be applied based on the desired concurrency level of the
            client using it. One may choose to have a relatively higher bound in cases, where the usage is not trusted
            and can lead to large amount of connections.*/
            final PooledConnectionProvider<W, R> hostConnProvider = PooledConnectionProvider.createUnbounded(cf, host);

            /*Create the listener that acts as the failure detector for this host.*/
            TcpClientEventListener fd = failureDetetctorFactory.call(() -> hostRemover.onNext(hostConnProvider));

            /*Add the holder to the list of active hosts.*/
            _hostConnectionProviders.add(new HostHolder(hostConnProvider, fd));
        }

        /*For every modification, the list of active hosts is copied to remove contention in the request path, thus this
        * list is unmodifiable.*/
        activeHosts.set(Collections.unmodifiableList(_hostConnectionProviders));

        /*Serialize the subject so that even though there are concurrent removals from various failure detectors, the
        * active host list is not modified concurrently. This makes the implementation simpler*/
        hostRemover.serialize()
                   .subscribe(hostToRemove -> {
                       final List<HostHolder> cps = activeHosts.get();
                       /*Create a new active hosts list with one lesser slot*/
                       final List<HostHolder> copyCps = new ArrayList<>(cps.size() - 1);
                       /*Add all hosts, but the one to be removed*/
                       cps.stream().forEach(toAdd -> {
                           if (toAdd.connectionProvider != hostToRemove) {
                               copyCps.add(toAdd);
                           }
                       });
                       /*Set this new list as the active hosts*/
                       activeHosts.set(copyCps);
                   }, Throwable::printStackTrace);


        currentHostIndex = new AtomicInteger();
    }

    @Override
    public ConnectionObservable<R, W> nextConnection() {
        return ConnectionObservable.createNew(new AbstractOnSubscribeFunc<R, W>() {
            @Override
            protected void doSubscribe(Subscriber<? super Connection<R, W>> sub,
                                       Action1<ConnectionObservable<R, W>> subscribeAllListenersAction) {
                /*For every subscription, connect to the best host.*/
                _connect(subscribeAllListenersAction)
                        /*If there is a connect failure, retry at max two times.*/
                        .retry((count, th) -> count < 3 && th instanceof SocketException)
                        .unsafeSubscribe(sub);
            }
        });
    }

    private Observable<Connection<R, W>> _connect(Action1<ConnectionObservable<R, W>> subscribeAllListenersAction) {
        return Observable.create(subscriber -> {

            /*Hold a reference to the current list of active hosts, so that any change to the list does not happen
            during this processing*/
            final List<HostHolder> hostHolders = activeHosts.get();

            /*If no hosts are available, bail out*/
            if (hostHolders.isEmpty()) {
                subscriber.onError(new NoSuchElementException("No host available."));
            }

            final int factoriesSize = hostHolders.size();
            /*Get the next host than what was used last.*/
            final int index = Math.abs(currentHostIndex.incrementAndGet() % factoriesSize);
            HostHolder holderToUse = hostHolders.get(index);
            ConnectionObservable<R, W> co = holderToUse.connectionProvider.nextConnection();
            /*Subscribe the failure detector to the next connection events*/
            co.subscribeForEvents(holderToUse.failureDetector);
            /*Subscribe all the listeners subscribed to this ConnectionObservable to the newly created connection Obsv.*/
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
