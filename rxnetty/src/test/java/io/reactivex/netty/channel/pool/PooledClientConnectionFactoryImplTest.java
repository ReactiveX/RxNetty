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

import io.netty.channel.embedded.EmbeddedChannel;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.client.TrackableMetricEventsListener;
import io.reactivex.netty.protocol.client.MaxConnectionsBasedStrategy;
import io.reactivex.netty.protocol.client.PoolExhaustedException;
import io.reactivex.netty.protocol.tcp.client.ClientConnectionFactory;
import io.reactivex.netty.protocol.tcp.client.ClientConnectionToChannelBridge;
import io.reactivex.netty.protocol.tcp.client.ClientState;
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

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
        pooledFactoryRule.assertNoIdleConnection();
    }

    @MaxConnections(1)
    @Test(timeout = 60000)
    public void testReuse() throws Exception {
        final PooledConnection<String, String> conn1 = pooledFactoryRule.getAConnection();

        pooledFactoryRule.returnToIdle(conn1);

        PooledConnection<String, String> conn2 = pooledFactoryRule.getAConnection();

        assertThat("Connection not reused.", conn2, is(conn1));
    }

    @Test(timeout = 60000)
    public void testRelease() throws Exception {
        _testRelease();
    }

    @Test(timeout = 60000)
    public void testDiscard() throws Exception {
        final Connection<String, String> connection = pooledFactoryRule.getAConnection();
        assertThat("Connection is null.", connection, notNullValue());

        pooledFactoryRule.assertNoIdleConnection();

        /*This attribute will discard on close*/
        connection.unsafeNettyChannel().attr(ClientConnectionToChannelBridge.DISCARD_CONNECTION).set(true);

        /* Close will discard */
        pooledFactoryRule.closeAndAwait(connection); /*Throw error or close quietly*/

        pooledFactoryRule.assertNoIdleConnection();
    }

    @Test(timeout = 60000)
    public void testExpired() throws Exception {
        final PooledConnection<String, String> connection = pooledFactoryRule.getAConnection();
        assertThat("Connection is null.", connection, notNullValue());

        pooledFactoryRule.assertNoIdleConnection();

        /*This attribute will discard on close*/
        connection.setLastReturnToPoolTimeMillis(System.currentTimeMillis() - 30000);

        /* Close will discard */
        pooledFactoryRule.closeAndAwait(connection); /*Throw error or close quietly*/

        pooledFactoryRule.assertNoIdleConnection();
    }

    @Test(timeout = 60000)
    public void testIdleConnectionCleanup() throws Exception {
        PooledConnection<String, String> idleConnection = _testRelease();

        /*Force discard by next idle connection reap*/
        idleConnection.unsafeNettyChannel().attr(ClientConnectionToChannelBridge.DISCARD_CONNECTION).set(true);

        pooledFactoryRule.testScheduler.advanceTimeBy(1, TimeUnit.MINUTES);

        pooledFactoryRule.assertNoIdleConnection();
    }

    @MaxConnections(1)
    @Test(timeout = 60000)
    public void testPoolExhaustion() throws Exception {
        thrown.expectCause(isA(PoolExhaustedException.class));

        pooledFactoryRule.getAConnection();

        pooledFactoryRule.getFactory().connect().toBlocking().single();
    }

    @Test(timeout = 60000)
    public void testConnectFailed() throws Exception {
        PooledClientConnectionFactory<String, String> factory;
        factory = PooledClientConnectionFactoryImpl.create(pooledFactoryRule.holder,
                                                           new TestableClientConnectionFactory<String, String>(pooledFactoryRule.state) {
                                                               @Override
                                                               public Observable<? extends Connection<String, String>> connect() {
                                                                   return Observable.error(new IOException());
                                                               }
                                                           })
                                                   .call(pooledFactoryRule.state);

        TestSubscriber<Object> subscriber = new TestSubscriber<>();
        factory.connect().subscribe(subscriber);

        subscriber.assertTerminalEvent();

        assertThat("Error not returned to connect.", subscriber.getOnCompletedEvents(), is(empty()));
        assertThat("Error not returned to connect.", subscriber.getOnNextEvents(), is(empty()));

    }

    @Test(timeout = 60000)
    public void testMetricEventCallback() throws Throwable {
        final PooledConnection<String, String> connection = pooledFactoryRule.getAConnection();

        assertThat("Unexpected acquire attempted count.", pooledFactoryRule.eventsListener.getAcquireAttemptedCount(),
                   is(1L));
        assertThat("Unexpected acquire succedded count.", pooledFactoryRule.eventsListener.getAcquireSucceededCount(),
                   is(1L));
        assertThat("Unexpected acquire failed count.", pooledFactoryRule.eventsListener.getAcquireFailedCount(),
                   is(0L));
        assertThat("Unexpected creation count.", pooledFactoryRule.eventsListener.getCreationCount(),
                   is(1L));

        pooledFactoryRule.returnToIdle(connection);

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

    private PooledConnection<String, String> _testRelease() throws Exception {
        final Connection<String, String> connection = pooledFactoryRule.getAConnection();

        pooledFactoryRule.assertNoIdleConnection();

        /* Close will release */
        pooledFactoryRule.closeAndAwait(connection); /*Throw error or close quietly*/

        PooledConnection<String, String> connIdle =
                pooledFactoryRule.holder.peek().defaultIfEmpty(null).toBlocking().single();

        assertThat("Release did not add to idle.", connIdle, not(nullValue()));

        return connIdle;
    }

    public static class PooledFactoryRule extends ExternalResource {

        private PooledClientConnectionFactory<String, String> factory;
        private TestableClientConnectionFactory<String, String> delegateFactory;
        private TestScheduler testScheduler;
        private FIFOIdleConnectionsHolder<String, String> holder;
        private TrackableMetricEventsListener eventsListener;
        private ClientState<String, String> state;

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
                    state = ClientState.<String, String>create()
                                       .idleConnectionCleanupTimer(idleConnCleaner)
                                       .maxConnections(maxConnections);
                    delegateFactory = new TestableClientConnectionFactory<>(state);
                    holder = new FIFOIdleConnectionsHolder<>();
                    factory = PooledClientConnectionFactoryImpl.create(holder, delegateFactory).call(state);
                    eventsListener = new TrackableMetricEventsListener();
                    state.getEventPublisher().subscribe(eventsListener);
                    base.evaluate();
                }
            };
        }

        public void tickIdleCleanupTicker() {
            testScheduler.triggerActions();
        }

        public PooledClientConnectionFactory<String, String> getFactory() {
            return factory;
        }

        public PooledConnection<String, String> getAConnection()
                throws InterruptedException, ExecutionException, TimeoutException {

            TestSubscriber<Connection<String, String>> connSub = new TestSubscriber<>();
            getFactory().connect().subscribe(connSub);

            connSub.awaitTerminalEvent();
            connSub.assertNoErrors();
            assertThat("Unexpected connections returned on connect.", connSub.getOnNextEvents(), hasSize(1));

            Connection<String, String> connection = connSub.getOnNextEvents().get(0);
            assertThat("Connection is null.", connection, notNullValue());

            return (PooledConnection<String, String>) connection;
        }

        public void closeAndAwait(Connection<String, String> toClose) throws Exception {
            EmbeddedChannel embeddedChannel= (EmbeddedChannel) toClose.unsafeNettyChannel();

            final TestSubscriber<Void> testSubscriber = new TestSubscriber<>();

            toClose.close().subscribe(testSubscriber);

            embeddedChannel.runPendingTasks();

            testSubscriber.awaitTerminalEvent();

            testSubscriber.assertNoErrors();
        }

        public void assertNoIdleConnection() {
            final TestSubscriber<PooledConnection<String, String>> subscriber = new TestSubscriber<>();
            holder.peek().subscribe(subscriber);

            subscriber.awaitTerminalEvent();
            subscriber.assertNoErrors();

            assertThat("Idle connection available", subscriber.getOnNextEvents(), is(empty()));
        }

        public void returnToIdle(PooledConnection<String, String> conn1) {
            conn1.closeNow();
            EmbeddedChannel embeddedChannel= (EmbeddedChannel) conn1.unsafeNettyChannel();
            embeddedChannel.runPendingTasks();

            TestSubscriber<PooledConnection<String, String>> subscriber = new TestSubscriber<>();
            holder.peek().take(1).subscribe(subscriber);
            subscriber.awaitTerminalEvent();

            assertThat("Unexpected number of idle connections post release.", subscriber.getOnNextEvents(), hasSize(1));
            assertThat("Connection not returned to idle holder on close.",
                       subscriber.getOnNextEvents().get(0), is(conn1));
        }
    }

    public static class TestableClientConnectionFactory<W, R> extends ClientConnectionFactory<W, R> {

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
                    EmbeddedChannel channel = new EmbeddedChannel();
                    ClientConnectionToChannelBridge.addToPipeline(subscriber, channel.pipeline(),
                                                                  state.getEventPublisher(), false);
                    channel.connect(new InetSocketAddress("localhost", 0));
                    channel.pipeline().fireChannelActive();
                }
            });
        }

        @Override
        protected <WW, RR> ClientConnectionFactory<WW, RR> doCopy(ClientState<WW, RR> newState) {
            throw new UnsupportedOperationException("Copy not supported as it breaks test assumptions.");
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(METHOD)
    public @interface MaxConnections {
        int value() default MaxConnectionsBasedStrategy.DEFAULT_MAX_CONNECTIONS;
    }
}