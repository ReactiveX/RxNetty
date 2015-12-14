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

package io.reactivex.netty.examples.tcp.loadbalancing;

import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.client.ConnectionProvider;
import io.reactivex.netty.client.HostConnector;
import io.reactivex.netty.client.events.ClientEventListener;
import io.reactivex.netty.client.loadbalancer.HostHolder;
import io.reactivex.netty.client.loadbalancer.LoadBalancingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Observable.OnSubscribe;

import java.net.SocketException;
import java.util.List;
import java.util.Random;

public abstract class AbstractLoadBalancer<W, R> implements LoadBalancingStrategy<W, R> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractLoadBalancer.class);

    @Override
    public ConnectionProvider<W, R> newStrategy(final List<HostHolder<W, R>> hosts) {

        final int size = hosts.size();
        final Random r1 = new Random();
        final Random r2 = new Random();

        return () -> Observable.create((OnSubscribe<Connection<R, W>>) subscriber -> {

            ConnectionProvider<W, R> hostToUse;

            HostHolder<W, R> host1 = hosts.get(r1.nextInt(size));
            HostHolder<W, R> host2 = hosts.get(r2.nextInt(size));

            long weight1 = getWeight(host1.getEventListener());
            long weight2 = getWeight(host2.getEventListener());

            //logger.error("Weight 1 => " + weight1 + ", weight 2 => " + weight2);

            if (weight1 >= weight2) {
                hostToUse = host1.getConnector().getConnectionProvider();
            } else {
                hostToUse = host2.getConnector().getConnectionProvider();
            }

            hostToUse.newConnectionRequest().unsafeSubscribe(subscriber);

        }).retry((count, th) -> count < 3 && th instanceof SocketException);
    }

    @Override
    public final HostHolder<W, R> toHolder(HostConnector<W, R> connector) {
        return new HostHolder<>(connector, newListener());
    }

    protected abstract ClientEventListener newListener();

    protected abstract long getWeight(ClientEventListener eventListener);
}
