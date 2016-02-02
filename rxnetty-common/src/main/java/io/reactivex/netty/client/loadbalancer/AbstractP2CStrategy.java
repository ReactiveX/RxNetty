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
import io.reactivex.netty.client.Host;
import io.reactivex.netty.client.HostConnector;
import io.reactivex.netty.client.events.ClientEventListener;
import rx.Observable;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public abstract class AbstractP2CStrategy<W, R, L extends ClientEventListener> implements LoadBalancingStrategy<W, R>  {

    @Override
    public ConnectionProvider<W, R> newStrategy(final List<HostHolder<W, R>> hosts) {
        newHostsList(hosts.size());
        return new ConnectionProvider<W, R>() {

            @Override
            public Observable<Connection<R, W>> newConnectionRequest() {
                HostHolder<W, R> selected = null;
                if (hosts.isEmpty()) {
                    noUsableHostsFound();
                    return Observable.error(NoHostsAvailableException.EMPTY_INSTANCE);
                } else if (hosts.size() == 1) {
                    HostHolder<W, R> holder = hosts.get(0);
                    @SuppressWarnings("unchecked")
                    L eventListener = (L) holder.getEventListener();
                    double weight = getWeight(eventListener);
                    if (isUnusable(weight)) {
                        noUsableHostsFound();
                        return Observable.error(new NoHostsAvailableException("No usable hosts found."));
                    }
                    selected = holder;
                } else {
                    ThreadLocalRandom rand = ThreadLocalRandom.current();
                    for (int i = 0; i < 5; i++) {
                        int pos  = rand.nextInt(hosts.size());
                        HostHolder<W, R> first  = hosts.get(pos);
                        int pos2 = (rand.nextInt(hosts.size() - 1) + pos + 1) % hosts.size();
                        HostHolder<W, R> second = hosts.get(pos2);

                        @SuppressWarnings("unchecked")
                        double w1 = getWeight((L) first.getEventListener());
                        @SuppressWarnings("unchecked")
                        double w2 = getWeight((L) second.getEventListener());

                        if (w1 > w2) {
                            selected = first;
                            break;
                        } else if (w1 < w2) {
                            selected = second;
                            break;
                        } else if (!isUnusable(w1)) {
                            selected = first;
                            break;
                        }
                        foundTwoUnusableHosts();
                    }
                    if (null == selected) {
                        noUsableHostsFound();
                        return Observable.error(new NoHostsAvailableException("No usable hosts found after 5 tries."));
                    }
                }

                return selected.getConnector().getConnectionProvider().newConnectionRequest();
            }
        };
    }

    protected boolean isUnusable(double weight) {
        return weight < 0.0;
    }

    @Override
    public HostHolder<W, R> toHolder(HostConnector<W, R> connector) {
        return new HostHolder<>(connector, newListener(connector.getHost()));
    }

    protected abstract L newListener(Host host);

    protected abstract double getWeight(L listener);

    protected void noUsableHostsFound() {
        // No Op by default
    }

    protected void foundTwoUnusableHosts() {
        // No Op by default
    }

    protected void newHostsList(int size) {
        // No Op by default
    }
}
