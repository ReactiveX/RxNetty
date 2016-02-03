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

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.client.ConnectionProvider;
import io.reactivex.netty.client.Host;
import io.reactivex.netty.client.HostConnector;
import io.reactivex.netty.client.events.ClientEventListener;
import io.reactivex.netty.events.EventPublisher;
import io.reactivex.netty.test.util.DisabledEventPublisher;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import rx.Observable;
import rx.observers.TestSubscriber;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AbstractP2CStrategyTest {

    @Rule
    public final StrategyRule rule = new StrategyRule();

    @Test
    public void testNoHosts() {
        ConnectionProvider<ByteBuf, ByteBuf> cp = rule.strategy.newStrategy(
                Collections.<HostHolder<ByteBuf, ByteBuf>>emptyList());
        TestSubscriber<Connection<ByteBuf, ByteBuf>> sub = new TestSubscriber<>();
        cp.newConnectionRequest().subscribe(sub);

        sub.awaitTerminalEvent();
        sub.assertError(NoHostsAvailableException.class);
        Assert.assertEquals("Unexpected number of hosts in the pool.", 0, rule.strategy.hostsInPool);
        Assert.assertEquals("Unexpected number of no usable hosts count.", 1, rule.strategy.allUnusable);
    }

    @Test
    public void testSingleUnusableHost() {
        ConnectionProvider<ByteBuf, ByteBuf> cp = rule.strategy.newStrategy(rule.newHostStream(0));
        TestSubscriber<Connection<ByteBuf, ByteBuf>> sub = new TestSubscriber<>();
        cp.newConnectionRequest().subscribe(sub);

        sub.awaitTerminalEvent();
        sub.assertError(NoHostsAvailableException.class);

        Assert.assertEquals("Unexpected number of hosts in the pool.", 1, rule.strategy.hostsInPool);
        Assert.assertEquals("Unexpected number of Unusable hosts found count.", 0, rule.strategy.twoUnusableHosts);
        Assert.assertEquals("Unexpected number of no usable hosts count.", 1, rule.strategy.allUnusable);
    }

    @Test
    public void testMultipleUnusableHost() {
        ConnectionProvider<ByteBuf, ByteBuf> cp = rule.strategy.newStrategy(rule.newHostStream(0, 0));
        TestSubscriber<Connection<ByteBuf, ByteBuf>> sub = new TestSubscriber<>();
        cp.newConnectionRequest().subscribe(sub);

        sub.awaitTerminalEvent();
        sub.assertError(NoHostsAvailableException.class);

        Assert.assertEquals("Unexpected number of hosts in the pool.", 2, rule.strategy.hostsInPool);
        Assert.assertEquals("Unexpected number of Unusable hosts found count.", 5, rule.strategy.twoUnusableHosts);
        Assert.assertEquals("Unexpected number of no usable hosts count.", 1, rule.strategy.allUnusable);
    }

    @Test
    public void testUsableAndUnusable() {
        ConnectionProvider<ByteBuf, ByteBuf> cp = rule.strategy.newStrategy(rule.newHostStream(10, 0));
        TestSubscriber<Connection<ByteBuf, ByteBuf>> sub = new TestSubscriber<>();
        cp.newConnectionRequest().subscribe(sub);

        sub.awaitTerminalEvent();
        sub.assertNoErrors();

        Assert.assertEquals("Unexpected number of hosts in the pool.", 2, rule.strategy.hostsInPool);
        Assert.assertEquals("Unexpected number of Unusable hosts found count.", 0, rule.strategy.twoUnusableHosts);
        Assert.assertEquals("Unexpected number of no usable hosts count.", 0,rule.strategy.allUnusable);
    }

    public static class StrategyRule extends ExternalResource {

        private MockP2CStrategy strategy;

        @Override
        public Statement apply(final Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    strategy = new MockP2CStrategy();
                    base.evaluate();
                }
            };
        }

        public List<HostHolder<ByteBuf, ByteBuf>> newHostStream(int... weights) {
            List<HostHolder<ByteBuf, ByteBuf>> toReturn = new ArrayList<>();
            for (int weight : weights) {
                ConnectionProvider<ByteBuf, ByteBuf> dummy = new ConnectionProvider<ByteBuf, ByteBuf>() {
                    @Override
                    public Observable<Connection<ByteBuf, ByteBuf>> newConnectionRequest() {
                        return Observable.empty();
                    }
                };
                Host h = new Host(new InetSocketAddress(0));
                EventPublisher publisher = DisabledEventPublisher.DISABLED_EVENT_PUBLISHER;
                HostConnector<ByteBuf, ByteBuf> connector = new HostConnector<>(h, dummy, null, publisher, null);
                toReturn.add(new HostHolder<>(connector, new ClientListenerImpl(weight)));
            }
            return toReturn;
        }

        private static class ClientListenerImpl extends ClientEventListener {

            private volatile double weight;

            public ClientListenerImpl(double weight) {
                this.weight = weight;
            }

            public double getWeight() {
                return weight;
            }
        }

        private static class MockP2CStrategy extends AbstractP2CStrategy<ByteBuf, ByteBuf, ClientListenerImpl> {

            private volatile int allUnusable;
            private volatile int hostsInPool;
            private volatile int twoUnusableHosts;

            @Override
            protected ClientListenerImpl newListener(Host host) {
                return new ClientListenerImpl(0);
            }

            @Override
            protected double getWeight(ClientListenerImpl listener) {
                return listener.getWeight();
            }

            @Override
            protected void noUsableHostsFound() {
                allUnusable++;
            }

            @Override
            protected void foundTwoUnusableHosts() {
                twoUnusableHosts++;
            }

            @Override
            protected void newHostsList(int size) {
                hostsInPool = hostsInPool + size;
            }
        }
    }
}