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
package io.reactivex.netty.client.pool;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.logging.LoggingHandler;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.channel.ConnectionImpl;
import io.reactivex.netty.client.ClientConnectionToChannelBridge;
import io.reactivex.netty.client.ConnectionProvider;
import io.reactivex.netty.client.Host;
import io.reactivex.netty.client.HostConnector;
import io.reactivex.netty.client.events.ClientEventListener;
import io.reactivex.netty.events.EventAttributeKeys;
import io.reactivex.netty.test.util.DisabledEventPublisher;
import io.reactivex.netty.test.util.TrackableMetricEventsListener;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.lang.annotation.ElementType.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class PooledConnectionProviderImplTest {

    @Rule
    public ExpectedException thrown= ExpectedException.none();

    @Rule
    public final PooledFactoryRule pooledFactoryRule = new PooledFactoryRule();

    @Test(timeout = 6000000)
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

    @Test(timeout = 60000000)
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

        pooledFactoryRule.getProvider().newConnectionRequest().toBlocking().single();
    }

    @Test(timeout = 60000)
    public void testConnectFailed() throws Exception {
        PooledConnectionProvider<String, String> factory;
        PoolConfig<String, String> config = new PoolConfig<>();
        config.idleConnectionsHolder(pooledFactoryRule.holder);

        DisabledEventPublisher<ClientEventListener> publisher = new DisabledEventPublisher<>();
        EmbeddedConnectionProvider connectionProvider = new EmbeddedConnectionProvider(publisher, true);
        ClientEventListener listener = new ClientEventListener();
        Host host = new Host(new InetSocketAddress("127.0.0.1", 0));
        HostConnector<String, String> connector = new HostConnector<>(host, connectionProvider,
                                                                      publisher, publisher, listener);

        factory = new PooledConnectionProviderImpl<>(config, connector);

        TestSubscriber<Object> subscriber = new TestSubscriber<>();
        factory.newConnectionRequest().subscribe(subscriber);

        subscriber.assertTerminalEvent();

        assertThat("Error not returned to connect.", subscriber.getOnCompletedEvents(), is(empty()));
        assertThat("Error not returned to connect.", subscriber.getOnNextEvents(), is(empty()));

    }

    //@Test(timeout = 60000) // TODO: Fix me
    public void testMetricEventCallback() throws Throwable {
        TrackableMetricEventsListener eventsListener = new TrackableMetricEventsListener();

        final PooledConnection<String, String> connection = pooledFactoryRule.getAConnection();

        assertThat("Unexpected acquire attempted count.", eventsListener.getAcquireAttemptedCount(),
                   is(1L));
        assertThat("Unexpected acquire succedded count.", eventsListener.getAcquireSucceededCount(),
                   is(1L));
        assertThat("Unexpected acquire failed count.", eventsListener.getAcquireFailedCount(),
                   is(0L));
        assertThat("Unexpected creation count.", eventsListener.getCreationCount(),
                   is(1L));

        pooledFactoryRule.returnToIdle(connection);

        assertThat("Unexpected release attempted count.", eventsListener.getReleaseAttemptedCount(),
                   is(1L));
        assertThat("Unexpected release succeeded count.", eventsListener.getReleaseSucceededCount(),
                   is(1L));
        assertThat("Unexpected release failed count.", eventsListener.getReleaseFailedCount(), is(0L));
        assertThat("Unexpected create connection count.", eventsListener.getCreationCount(), is(1L));

        final PooledConnection<String, String> reusedConn = pooledFactoryRule.getAConnection();

        Assert.assertEquals("Reused connection not same as original.", connection, reusedConn);

        assertThat("Unexpected acquire attempted count.", eventsListener.getAcquireAttemptedCount(),
                   is(2L));
        assertThat("Unexpected acquire succedded count.", eventsListener.getAcquireSucceededCount(),
                   is(2L));
        assertThat("Unexpected acquire failed count.", eventsListener.getAcquireFailedCount(),
                   is(0L));
        assertThat("Unexpected creation count.", eventsListener.getCreationCount(),
                   is(1L));
        assertThat("Unexpected reuse count.", eventsListener.getReuseCount(),
                   is(1L));

        pooledFactoryRule.closeAndAwait(reusedConn);

        assertThat("Unexpected release attempted count.", eventsListener.getReleaseAttemptedCount(),
                   is(2L));
        assertThat("Unexpected release succeeded count.", eventsListener.getReleaseSucceededCount(),
                   is(2L));
        assertThat("Unexpected release failed count.", eventsListener.getReleaseFailedCount(),
                   is(0L));
        assertThat("Unexpected create connection count.", eventsListener.getCreationCount(),
                   is(1L));

        pooledFactoryRule.provider.discard(reusedConn).toBlocking().lastOrDefault(null);

        assertThat("Unexpected release attempted count.", eventsListener.getReleaseAttemptedCount(),
                   is(2L));
        assertThat("Unexpected release succeeded count.", eventsListener.getReleaseSucceededCount(),
                   is(2L));
        assertThat("Unexpected release failed count.", eventsListener.getReleaseFailedCount(),
                   is(0L));
        assertThat("Unexpected create connection count.", eventsListener.getCreationCount(),
                   is(1L));
        assertThat("Unexpected create connection count.", eventsListener.getEvictionCount(),
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

        private PooledConnectionProvider<String, String> provider;
        private TestScheduler testScheduler;
        private FIFOIdleConnectionsHolder<String, String> holder;

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
                    holder = new FIFOIdleConnectionsHolder<>();
                    PoolConfig<String, String> config = new PoolConfig<>();
                    config.idleConnectionsCleanupTimer(idleConnCleaner)
                          .maxConnections(maxConnections)
                          .idleConnectionsHolder(holder);
                    ClientEventListener listener = new ClientEventListener();
                    Host host = new Host(new InetSocketAddress("127.0.0.1", 0));
                    final DisabledEventPublisher<ClientEventListener> publisher = new DisabledEventPublisher<>();
                    ConnectionProvider<String, String> cp = new EmbeddedConnectionProvider(publisher);
                    HostConnector<String, String> connector = new HostConnector<>(host, cp, publisher, publisher,
                                                                                  listener);
                    provider = new PooledConnectionProviderImpl<>(config, connector);
                    base.evaluate();
                }
            };
        }

        public PooledConnectionProvider<String, String> getProvider() {
            return provider;
        }

        public PooledConnection<String, String> getAConnection(Observable<Connection<String, String>> connectionObservable)
                throws InterruptedException, ExecutionException, TimeoutException {
            TestSubscriber<Connection<String, String>> connSub = new TestSubscriber<>();
            connectionObservable.subscribe(connSub);

            connSub.awaitTerminalEvent();
            connSub.assertNoErrors();
            assertThat("Unexpected connections returned on connect.", connSub.getOnNextEvents(), hasSize(1));

            Connection<String, String> connection = connSub.getOnNextEvents().get(0);
            assertThat("Connection is null.", connection, notNullValue());

            return (PooledConnection<String, String>) connection;
        }

        public PooledConnection<String, String> getAConnection()
                throws InterruptedException, ExecutionException, TimeoutException {
            return getAConnection(getProvider().newConnectionRequest());
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

    private static class EmbeddedConnectionProvider implements ConnectionProvider<String, String> {

        private final DisabledEventPublisher<ClientEventListener> publisher;
        private final boolean failConnect;

        public EmbeddedConnectionProvider(DisabledEventPublisher<ClientEventListener> publisher, boolean failConnect) {
            this.publisher = publisher;
            this.failConnect = failConnect;
        }

        public EmbeddedConnectionProvider(DisabledEventPublisher<ClientEventListener> publisher) {
            this(publisher, false);
        }

        @Override
        public Observable<Connection<String, String>> newConnectionRequest() {
            if (failConnect) {
                return Observable.error(new IllegalStateException("Deliberate connect failure"));
            }

            return Observable.create(new OnSubscribe<Connection<String, String>>() {
                @Override
                public void call(Subscriber<? super Connection<String, String>> s) {
                    EmbeddedChannel c = new EmbeddedChannel(new LoggingHandler());
                    c.attr(EventAttributeKeys.EVENT_PUBLISHER).set(publisher);
                    ClientConnectionToChannelBridge.addToPipeline(c.pipeline(), false);
                    s.onNext(ConnectionImpl.<String, String>fromChannel(c));
                    s.onCompleted();
                }
            });
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(METHOD)
    public @interface MaxConnections {
        int value() default MaxConnectionsBasedStrategy.DEFAULT_MAX_CONNECTIONS;
    }
}