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
package io.reactivex.netty.protocol.tcp.client;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.concurrent.Future;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.channel.ConnectionImpl;
import io.reactivex.netty.client.ClientChannelMetricEventProvider;
import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.client.MaxConnectionsBasedStrategy;
import io.reactivex.netty.client.PoolConfig;
import io.reactivex.netty.client.PreferCurrentEventLoopGroup;
import io.reactivex.netty.metrics.MetricEventsSubject;
import io.reactivex.netty.protocol.tcp.client.PooledConnection.Owner;
import io.reactivex.netty.protocol.tcp.client.PreferCurrentEventLoopHolder.IdleConnectionsHolderFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class PreferCurrentEventLoopHolderTest {

    @Rule
    public final PreferCurrentELHolderRule preferCurrentELHolderRule = new PreferCurrentELHolderRule();

    @Test(timeout = 60000)
    public void testPollOutOfEventloop() throws Exception {
        PooledConnection<String, String> connection1 = preferCurrentELHolderRule.addConnection();

        /*Make sure the connection is not available in the eventloop*/
        PooledConnection<String, String> connection = preferCurrentELHolderRule.holder.pollThisEventLoopConnections()
                                                                                      .defaultIfEmpty(null)
                                                                                      .toBlocking()
                                                                                      .single();
        assertThat("Connection available in the eventloop.", connection, is(nullValue()));

        connection = preferCurrentELHolderRule.holder.poll()
                                                     .defaultIfEmpty(null).toBlocking().single();

        assertThat("Unexpected connection.", connection, is(connection1));
    }

    @Test(timeout = 60000)
    public void testPollInEventloop() throws Exception {
        final PooledConnection<String, String> connection1 = preferCurrentELHolderRule.addConnection();

        preferCurrentELHolderRule.runFromChannelEventLoop(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                PooledConnection<String, String> connection = preferCurrentELHolderRule.holder.poll()
                                                                                              .defaultIfEmpty(null)
                                                                                              .toBlocking()
                                                                                              .single();
                assertThat("Connection available in the eventloop.", connection, is(connection1));
                return null;
            }
        }).get(1, TimeUnit.MINUTES);
    }

    @Test
    public void testPollThisEventLoopConnectionsInEl() throws Exception {
        final PooledConnection<String, String> connection1 = preferCurrentELHolderRule.addConnection();
        preferCurrentELHolderRule.runFromChannelEventLoop(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                PooledConnection<String, String> connection =
                        preferCurrentELHolderRule.holder.pollThisEventLoopConnections()
                                                        .defaultIfEmpty(null)
                                                        .toBlocking().single();
                assertThat("Connection available in the eventloop.", connection, is(connection1));
                return null;
            }
        }).get(1, TimeUnit.MINUTES);
    }

    @Test(timeout = 60000)
    public void testPollThisEventLoopConnectionsOutOfEl() throws Exception {
        final PooledConnection<String, String> connection1 = preferCurrentELHolderRule.addConnection();
        PooledConnection<String, String> connection = preferCurrentELHolderRule.holder.pollThisEventLoopConnections()
                                                                                      .defaultIfEmpty(null)
                                                                                      .toBlocking().single();
        assertThat("Connection available out of the eventloop.", connection, is(nullValue()));

        connection = preferCurrentELHolderRule.holder.poll()
                                                     .defaultIfEmpty(null)
                                                     .toBlocking().single();
        assertThat("Connection not available with poll.", connection, is(connection1));
    }

    @Test(timeout = 60000)
    public void testPollRemovesItem() throws Exception {
        PooledConnection<String, String> connection1 = preferCurrentELHolderRule.addConnection();
        PooledConnection<String, String> connection = preferCurrentELHolderRule.holder.poll()
                                                                                      .defaultIfEmpty(null)
                                                                                      .toBlocking().single();

        assertThat("Connection not available with poll.", connection, is(connection1));

        connection = preferCurrentELHolderRule.holder.poll()
                                                     .defaultIfEmpty(null)
                                                     .toBlocking().single();

        assertThat("Connection available after poll.", connection, is(nullValue()));
    }

    @Test(timeout = 60000)
    public void testPeek() throws Exception {
        PooledConnection<String, String> connection1 = preferCurrentELHolderRule.addConnection();
        PooledConnection<String, String> connection = preferCurrentELHolderRule.holder.peek()
                                                                                      .defaultIfEmpty(null)
                                                                                      .toBlocking().single();

        assertThat("Connection not available with peek.", connection, is(connection1));

        connection = preferCurrentELHolderRule.holder.peek().defaultIfEmpty(null)
                                                     .toBlocking().single();

        assertThat("Connection not available after peek.", connection, not(nullValue()));
        assertThat("Unexpected connection on peek.", connection, is(connection1));
    }

    @Test(timeout = 60000)
    public void testRemove() throws Exception {
        PooledConnection<String, String> connection1 = preferCurrentELHolderRule.addConnection();
        PooledConnection<String, String> connection = preferCurrentELHolderRule.holder.peek()
                                                                                      .defaultIfEmpty(null)
                                                                                      .toBlocking().single();

        assertThat("Connection not available with peek.", connection, is(connection1));

        preferCurrentELHolderRule.holder.remove(connection1);

        connection = preferCurrentELHolderRule.holder.peek().defaultIfEmpty(null).toBlocking().single();

        assertThat("Connection not removed.", connection, is(nullValue()));
    }

    public static class PreferCurrentELHolderRule extends ExternalResource implements Owner<String, String> {

        private PreferCurrentEventLoopHolder<String, String> holder;
        private MetricEventsSubject<ClientMetricsEvent<?>> eventSubject;
        private PoolConfig<String, String> poolConfig;
        private ConcurrentLinkedQueue<PooledConnection<String, String>> discarded;
        private ConcurrentLinkedQueue<PooledConnection<String, String>> released;
        private ExecutorService eventLoopThread;
        private EmbeddedChannel channel;

        @Override
        public Statement apply(final Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    eventLoopThread = Executors.newFixedThreadPool(1);
                    channel = new EmbeddedChannel();
                    PreferCurrentEventLoopGroup eventLoopGroup = new PreferCurrentEventLoopGroup(channel.eventLoop());
                    eventSubject = new MetricEventsSubject<>();
                    holder = new PreferCurrentEventLoopHolder<String, String>(eventLoopGroup,
                                                                              new IdleConnectionsHolderFactoryImpl());
                    poolConfig = new PoolConfig<>(TimeUnit.DAYS.toMillis(1),
                                                  Observable.timer(1, TimeUnit.DAYS, Schedulers.test()),
                                                  new MaxConnectionsBasedStrategy(1), holder);
                    discarded = new ConcurrentLinkedQueue<>();
                    released = new ConcurrentLinkedQueue<>();
                    base.evaluate();
                }
            };
        }

        public PooledConnection<String, String> addConnection() throws Exception {
            Connection<String, String> connection = ConnectionImpl.create(channel, eventSubject,
                                                                          ClientChannelMetricEventProvider.INSTANCE);
            PooledConnection<String, String> pooledConnection = PooledConnection.create(this, poolConfig, connection);
            holder.add(pooledConnection);

            runAllPendingTasksOnChannel();

            return pooledConnection;
        }

        @Override
        public Observable<Void> release(PooledConnection<String, String> connection) {
            released.add(connection);
            return Observable.empty();
        }

        @Override
        public Observable<Void> discard(PooledConnection<String, String> connection) {
            discarded.add(connection);
            return Observable.empty();
        }

        public Future<Void> runFromChannelEventLoop(Callable<Void> runnable) throws Exception {
            Future<Void> toReturn = channel.eventLoop().submit(runnable);
            runAllPendingTasksOnChannel();
            return toReturn;
        }

        public void runAllPendingTasksOnChannel() throws Exception {
            eventLoopThread.submit(new Runnable() {
                @Override
                public void run() {
                    channel.runPendingTasks();
                }
            }).get(1, TimeUnit.MINUTES);
        }

        private class IdleConnectionsHolderFactoryImpl implements IdleConnectionsHolderFactory<String, String> {

            @Override
            public <WW, RR> IdleConnectionsHolderFactory<WW, RR> copy(ClientState<WW, RR> newState) {
                throw new UnsupportedOperationException("Copy not supported as it breaks test assumptions");
            }

            @Override
            public IdleConnectionsHolder<String, String> call() {
                return new FIFOIdleConnectionsHolder<>();
            }
        }
    }
}