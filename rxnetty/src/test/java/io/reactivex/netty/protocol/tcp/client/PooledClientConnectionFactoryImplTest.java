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
import io.reactivex.netty.channel.ClientConnectionToChannelBridge;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.client.MaxConnectionsBasedStrategy;
import io.reactivex.netty.client.PoolExhaustedException;
import io.reactivex.netty.client.TrackableMetricEventsListener;
import io.reactivex.netty.protocol.http.client.ClientRequestResponseConverter;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.schedulers.TestScheduler;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import static java.lang.annotation.ElementType.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class PooledClientConnectionFactoryImplTest {

    @Rule
    public ExpectedException thrown= ExpectedException.none();

    @Rule
    public final PooledFactoryRule pooledFactoryRule = new PooledFactoryRule();

    @Test(timeout = 60000)
    public void testConnect() throws Exception {
        pooledFactoryRule.getAConnection();

        PooledConnection<String, String> connIdle = pooledFactoryRule.holder.peek().defaultIfEmpty(null)
                                                                            .toBlocking().single();

        assertThat("Idle connection available", connIdle, is(nullValue()));
    }

    @Test(timeout = 60000)
    public void testRelease() throws Throwable {
        _testRelease();
    }

    @Test(timeout = 60000)
    public void testDiscard() throws Throwable {
        final Connection<String, String> connection = pooledFactoryRule.getAConnection();
        assertThat("Connection is null.", connection, notNullValue());

        PooledConnection<String, String> connIdle = pooledFactoryRule.holder.peek().defaultIfEmpty(null)
                                                                            .toBlocking().single();
        assertThat("Idle connections available before discard.", connIdle, is(nullValue()));

        /*This attribute will discard on close*/
        connection.getNettyChannel().attr(ClientRequestResponseConverter.DISCARD_CONNECTION).set(true);

        /* Close will discard */
        pooledFactoryRule.closeAndAwait(connection); /*Throw error or close quietly*/

        connIdle = pooledFactoryRule.holder.peek().defaultIfEmpty(null).toBlocking().single();

        assertThat("Discard added the connection to idle.", connIdle, is(nullValue()));
    }

    @Test(timeout = 60000)
    public void testIdleConnectionCleanup() throws Throwable {
        PooledConnection<String, String> idleConnection = _testRelease();

        /*Force discard by next idle connection reap*/
        idleConnection.getNettyChannel().attr(ClientRequestResponseConverter.DISCARD_CONNECTION).set(true);

        pooledFactoryRule.testScheduler.advanceTimeBy(1, TimeUnit.MINUTES);

        idleConnection = pooledFactoryRule.holder.peek().defaultIfEmpty(null).toBlocking().single();

        assertThat("Connection available post idle cleanup.", idleConnection, is(nullValue()));

    }

    @MaxConnections(1)
    @Test
    public void testPoolExhaustion() throws Exception {
        thrown.expectCause(isA(PoolExhaustedException.class));

        pooledFactoryRule.getAConnection();

        pooledFactoryRule.getFactory().connect().toBlocking().single();
    }

    @Test
    public void testMetricEventCallback() throws Throwable {
        final Connection<String, String> connection = pooledFactoryRule.getAConnection();

        assertThat("Unexpected acquire attempted count.", pooledFactoryRule.eventsListener.getAcquireAttemptedCount(),
                   is(1L));
        assertThat("Unexpected acquire succedded count.", pooledFactoryRule.eventsListener.getAcquireSucceededCount(),
                   is(1L));
        assertThat("Unexpected acquire failed count.", pooledFactoryRule.eventsListener.getAcquireFailedCount(),
                   is(0L));
        assertThat("Unexpected creation count.", pooledFactoryRule.eventsListener.getCreationCount(),
                   is(1L));

        pooledFactoryRule.closeAndAwait(connection);

        assertThat("Unexpected release attempted count.", pooledFactoryRule.eventsListener.getReleaseAttemptedCount(),
                   is(1L));
        assertThat("Unexpected release succeeded count.", pooledFactoryRule.eventsListener.getReleaseSucceededCount(),
                   is(1L));
        assertThat("Unexpected release failed count.", pooledFactoryRule.eventsListener.getReleaseFailedCount(), is(0L));
        assertThat("Unexpected create connection count.", pooledFactoryRule.eventsListener.getCreationCount(), is(1L));

        final PooledConnection<String, String> reusedConn = pooledFactoryRule.getAConnection();

        Assert.assertEquals("Reused connection not same as original.", connection, reusedConn);

        assertThat("Unexpected acquire attempted count.", pooledFactoryRule.eventsListener.getAcquireAttemptedCount(),
                   is(2L));
        assertThat("Unexpected acquire succedded count.", pooledFactoryRule.eventsListener.getAcquireSucceededCount(),
                   is(2L));
        assertThat("Unexpected acquire failed count.", pooledFactoryRule.eventsListener.getAcquireFailedCount(),
                   is(0L));
        assertThat("Unexpected creation count.", pooledFactoryRule.eventsListener.getCreationCount(),
                   is(1L));
        assertThat("Unexpected reuse count.", pooledFactoryRule.eventsListener.getReuseCount(),
                   is(1L));

        pooledFactoryRule.closeAndAwait(reusedConn);

        assertThat("Unexpected release attempted count.", pooledFactoryRule.eventsListener.getReleaseAttemptedCount(),
                   is(2L));
        assertThat("Unexpected release succeeded count.", pooledFactoryRule.eventsListener.getReleaseSucceededCount(),
                   is(2L));
        assertThat("Unexpected release failed count.", pooledFactoryRule.eventsListener.getReleaseFailedCount(),
                   is(0L));
        assertThat("Unexpected create connection count.", pooledFactoryRule.eventsListener.getCreationCount(),
                   is(1L));

        pooledFactoryRule.factory.discard(reusedConn).toBlocking().lastOrDefault(null);

        assertThat("Unexpected release attempted count.", pooledFactoryRule.eventsListener.getReleaseAttemptedCount(),
                   is(2L));
        assertThat("Unexpected release succeeded count.", pooledFactoryRule.eventsListener.getReleaseSucceededCount(),
                   is(2L));
        assertThat("Unexpected release failed count.", pooledFactoryRule.eventsListener.getReleaseFailedCount(),
                   is(0L));
        assertThat("Unexpected create connection count.", pooledFactoryRule.eventsListener.getCreationCount(),
                   is(1L));
        assertThat("Unexpected create connection count.", pooledFactoryRule.eventsListener.getEvictionCount(),
                   is(1L));
    }

    private PooledConnection<String, String> _testRelease() throws Throwable {
        final Connection<String, String> connection = pooledFactoryRule.getAConnection();

        PooledConnection<String, String> connIdle = pooledFactoryRule.holder.peek().defaultIfEmpty(null)
                                                                            .toBlocking().single();
        assertThat("Idle connections available before release.", connIdle, is(nullValue()));

        /* Close will release */
        pooledFactoryRule.closeAndAwait(connection); /*Throw error or close quietly*/

        connIdle = pooledFactoryRule.holder.peek().defaultIfEmpty(null).toBlocking().single();

        assertThat("Release did not add to idle.", connIdle, not(nullValue()));

        return connIdle;
    }

    public static class PooledFactoryRule extends ExternalResource {

        private PooledClientConnectionFactoryImpl<String, String> factory;
        private TestableClientConnectionFactory<String, String> delegateFactory;
        private TestScheduler testScheduler;
        private FIFOIdleConnectionsHolder<String, String> holder;
        private TrackableMetricEventsListener eventsListener;

        @Override
        public Statement apply(final Statement base, final Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {

                    MaxConnections maxConnections1 = description.getAnnotation(MaxConnections.class);
                    int maxConnections = null == maxConnections1 ? MaxConnectionsBasedStrategy.DEFAULT_MAX_CONNECTIONS
                                                                 : maxConnections1.value();

                    testScheduler = Schedulers.test();
                    Observable<Long> idleConnCleaner = Observable.timer(1, TimeUnit.MINUTES, testScheduler);
                    ClientState<String, String> state = ClientState.<String, String>create()
                                                                   .idleConnectionCleanupTimer(idleConnCleaner)
                                                                   .maxConnections(maxConnections);
                    delegateFactory = new TestableClientConnectionFactory<>(state);
                    holder = new FIFOIdleConnectionsHolder<>();
                    factory = new PooledClientConnectionFactoryImpl<String, String>(state, holder, delegateFactory) {
                    };
                    eventsListener = new TrackableMetricEventsListener();
                    state.getEventsSubject().subscribe(eventsListener);
                    base.evaluate();
                }
            };
        }

        public void tickIdleCleanupTicker() {
            testScheduler.triggerActions();
        }

        public PooledClientConnectionFactoryImpl<String, String> getFactory() {
            return factory;
        }

        public PooledConnection<String, String> getAConnection() {
            Connection<String, String> connection = getFactory().connect().toBlocking().single();
            assertThat("Connection is null.", connection, notNullValue());
            return (PooledConnection<String, String>) connection;
        }

        public void closeAndAwait(Connection<String, String> toClose) throws Throwable {
            EmbeddedChannel embeddedChannel= (EmbeddedChannel) toClose.getNettyChannel();

            final TestSubscriber<Void> testSubscriber = new TestSubscriber<>();

            toClose.close().subscribe(testSubscriber);

            embeddedChannel.runPendingTasks();

            testSubscriber.awaitTerminalEvent(1, TimeUnit.MINUTES);

            testSubscriber.assertNoErrors();
        }

        private static class TestableClientConnectionFactory<W, R> extends ClientConnectionFactory<W, R> {

            private final ClientState<W, R> state;

            public TestableClientConnectionFactory(ClientState<W, R> state) {
                super(state);
                this.state = state;
            }

            @Override
            public Observable<? extends Connection<R, W>> connect() {
                return Observable.create(new OnSubscribe<Connection<R, W>>() {
                    @Override
                    public void call(Subscriber<? super Connection<R, W>> subscriber) {
                        ClientConnectionToChannelBridge<R, W> bridge =
                                new ClientConnectionToChannelBridge<>(subscriber, state.getEventsSubject(), false);
                        EmbeddedChannel channel = new EmbeddedChannel(bridge);
                        channel.connect(new InetSocketAddress("localhost", 0));
                    }
                });
            }

            @Override
            protected <WW, RR> ClientConnectionFactory<WW, RR> doCopy(ClientState<WW, RR> newState) {
                throw new UnsupportedOperationException("Copy not supported as it breaks test assumptions.");
            }
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(METHOD)
    public @interface MaxConnections {
        int value() default MaxConnectionsBasedStrategy.DEFAULT_MAX_CONNECTIONS;
    }
}