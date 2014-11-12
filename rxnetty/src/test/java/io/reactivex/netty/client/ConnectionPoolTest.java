/*
 * Copyright 2014 Netflix, Inc.
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

package io.reactivex.netty.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.reactivex.netty.ChannelCloseListener;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.ConnectionHandler;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.metrics.MetricEventsSubject;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.pipeline.PipelineConfiguratorComposite;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.server.RxServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static io.reactivex.netty.client.RxClient.ClientConfig.Builder.newDefaultConfig;

/**
 * @author Nitesh Kant
 */
public class ConnectionPoolTest {

    @Rule public TestName name = new TestName();

    public static final int MAX_IDLE_TIME_MILLIS = 10000;

    private ConnectionPoolImpl<String, String> pool;
    private RxClient.ServerInfo serverInfo;
    private Bootstrap clientBootstrap;
    private TrackableMetricEventsListener metricEventsListener;
    private MaxConnectionsBasedStrategy strategy;
    private RxServer<String,String> server;
    private final ChannelCloseListener channelCloseListener = new ChannelCloseListener();
    private PoolStats stats;
    private ConnectionHandlerImpl serverConnHandler;
    private PipelineConfigurator<String,String> pipelineConfigurator;
    private String testId;
    private ClientChannelFactoryImpl<String,String> factory;
    private PoolConfig poolConfig;

    @Before
    public void setUp() throws Exception {
        testId = name.getMethodName();
        long currentTime = System.currentTimeMillis();
        System.out.println("Time: " + currentTime + ". Setting up test id: " + testId);
        serverConnHandler = new ConnectionHandlerImpl(testId);
        server = RxNetty.createTcpServer(0, PipelineConfigurators.textOnlyConfigurator(),
                                         serverConnHandler).start();
        serverInfo = new RxClient.ServerInfo("localhost", server.getServerPort());
        metricEventsListener = new TrackableMetricEventsListener();
        strategy = new MaxConnectionsBasedStrategy(1);
        clientBootstrap = new Bootstrap().group(new NioEventLoopGroup(4))
                                         .channel(NioSocketChannel.class);
        pipelineConfigurator = new PipelineConfiguratorComposite<String, String>(
                PipelineConfigurators.textOnlyConfigurator(), new PipelineConfigurator() {
            @Override
            public void configureNewPipeline(ChannelPipeline pipeline) {
                channelCloseListener.reset();
                pipeline.addFirst(channelCloseListener);
            }
        });

        pipelineConfigurator = PipelineConfigurators.createClientConfigurator(pipelineConfigurator,
                                                                              newDefaultConfig());

        clientBootstrap.handler(new ChannelInitializer<Channel>() {
            @Override
            public void initChannel(Channel ch) throws Exception {
                pipelineConfigurator.configureNewPipeline(ch.pipeline());
            }
        });

        poolConfig = new PoolConfig(MAX_IDLE_TIME_MILLIS);
        MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject = new MetricEventsSubject<ClientMetricsEvent<?>>();
        factory = new ClientChannelFactoryImpl<String, String>(clientBootstrap, eventsSubject);
        pool = new ConnectionPoolImpl<String, String>(serverInfo, poolConfig, strategy, null, factory, eventsSubject);
        pool.subscribe(metricEventsListener);
        stats = new PoolStats();
        pool.subscribe(stats);
    }

    @After
    public void tearDown() throws Exception {
        long currentTime = System.currentTimeMillis();
        System.out.println("Time: " + currentTime + ". Tearing down test id: " + testId);
        if (null != pool) {
            pool.shutdown();
        }
        if (null != clientBootstrap) {
            clientBootstrap.group().shutdownGracefully();
        }
        if (null != server) {
            serverConnHandler.closeNewConnectionsOnReceive(false); // reset state after test. Close New should be explicit.
            try {
                serverConnHandler.closeAllClientConnections();
            } catch (IllegalStateException e) {
                // Do nothing if there are no connections
            }
            server.shutdown();
            server.waitTillShutdown(1, TimeUnit.MINUTES);
        }
    }

    @Test
    public void testAcquireRelease() throws Exception {
        serverConnHandler.closeNewConnectionsOnReceive(true);
        ObservableConnection<String, String> conn = acquireAndTestStats();
        conn.close();
        waitForClose();
        assertAllConnectionsReturned();
    }

    @Test
    public void testReleaseAfterClose() throws Exception {
        serverConnHandler.closeNewConnectionsOnReceive(true);
        ObservableConnection<String, String> conn = acquireAndTestStats();
        waitForClose();
        conn.close();
        assertAllConnectionsReturned();
    }

    @Test
    public void testReuse() throws Exception {
        serverConnHandler.closeNewConnectionsOnReceive(false);
        ObservableConnection<String, String> connection = acquireAndTestStats();

        connection.close();

        ObservableConnection<String, String> reusedConn = acquireAndTestStats();

        Assert.assertEquals("Connection reuse callback not received.", 1, metricEventsListener.getReuseCount());
        Assert.assertEquals("Connection not reused.", connection, reusedConn);

        serverConnHandler.closeAllClientConnections();

        waitForClose();
        assertAllConnectionsReturned();
    }

    @Test
    public void testCloseExpiredConnection() throws Exception {
        serverConnHandler.closeNewConnectionsOnReceive(false);
        PooledConnection<String, String> connection = (PooledConnection<String, String>) acquireAndTestStats();

        connection.setLastReturnToPoolTimeMillis(System.currentTimeMillis() - PoolConfig.DEFAULT_CONFIG.getMaxIdleTimeMillis());

        connection.close();

        assertAllConnectionsReturned();
    }

    @Test
    public void testDiscard() throws Exception {
        serverConnHandler.closeNewConnectionsOnReceive(false);
        PooledConnection<String, String> connection = (PooledConnection<String, String>) acquireAndTestStats();

        pool.discard(connection);

        Assert.assertEquals("Unexpected pool idle count.", 0, stats.getIdleCount());
        Assert.assertEquals("Unexpected pool in-use count.", 1, stats.getInUseCount());
        Assert.assertEquals("Unexpected pool total connections count.", 1, stats.getTotalConnectionCount());
        Assert.assertEquals("Unexpected eviction count post close.", 0, metricEventsListener.getEvictionCount()); // Since it wasn't idle, there isn't an eviction.
    }

    @Test
    public void testDiscardPostRelease() throws Exception {
        serverConnHandler.closeNewConnectionsOnReceive(false);
        PooledConnection<String, String> connection = (PooledConnection<String, String>) acquireAndTestStats();

        connection.close();

        Assert.assertEquals("Unexpected pool idle count.", 1, stats.getIdleCount());
        Assert.assertEquals("Unexpected pool in-use count.", 0, stats.getInUseCount());
        Assert.assertEquals("Unexpected pool total connections count.", 1, stats.getTotalConnectionCount());
        Assert.assertEquals("Unexpected eviction count post close.", 0, metricEventsListener.getEvictionCount());

        pool.discard(connection);

        Assert.assertEquals("Unexpected pool idle count post discard.", 0, stats.getIdleCount());
        Assert.assertEquals("Unexpected pool in-use count post discard.", 0, stats.getInUseCount());
        Assert.assertEquals("Unexpected pool total connections count post discard.", 0, stats.getTotalConnectionCount());
        Assert.assertEquals("Unexpected eviction count post discard.", 1, metricEventsListener.getEvictionCount());

        assertAllConnectionsReturned();
    }

    @Test
    public void testShutdown() throws Exception {
        serverConnHandler.closeNewConnectionsOnReceive(false);
        strategy.incrementMaxConnections(2);

        ObservableConnection<String, String> connection1 = pool.acquire().toBlocking().last();
        ObservableConnection<String, String> connection2 = pool.acquire().toBlocking().last();
        ObservableConnection<String, String> connection3 = pool.acquire().toBlocking().last();

        Assert.assertEquals("Unexpected pool idle count.", 0, stats.getIdleCount());
        Assert.assertEquals("Unexpected pool in-use count.", 3, stats.getInUseCount());
        Assert.assertEquals("Unexpected pool total connections count.", 3, stats.getTotalConnectionCount());

        connection1.close();

        Assert.assertEquals("Unexpected pool idle count.", 1, stats.getIdleCount());
        Assert.assertEquals("Unexpected pool in-use count.", 2, stats.getInUseCount());
        Assert.assertEquals("Unexpected pool total connections count.", 3, stats.getTotalConnectionCount());

        connection2.close();
        connection3.close();

        Assert.assertEquals("Unexpected pool idle count post shutdown.", 3, stats.getIdleCount());
        Assert.assertEquals("Unexpected pool in-use count post shutdown.", 0, stats.getInUseCount());
        Assert.assertEquals("Unexpected pool total connections count post shutdown.", 3,
                            stats.getTotalConnectionCount());

        pool.shutdown();

        assertAllConnectionsReturned();
    }

    @Test
    public void testConnectFail() throws Exception {
        serverConnHandler.closeNewConnectionsOnReceive(false);
        RxClient.ServerInfo unavailableServer = new RxClient.ServerInfo("trampledunderfoot", 999);
        MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject = new MetricEventsSubject<ClientMetricsEvent<?>>();
        factory = new ClientChannelFactoryImpl<String, String>(clientBootstrap, eventsSubject);

        pool = new ConnectionPoolImpl<String, String>(unavailableServer, poolConfig, strategy, null, factory,
                                                      eventsSubject);
        pool.subscribe(metricEventsListener);

        try {
            pool.acquire().toBlocking().last();
            throw new AssertionError("Connect to a nonexistent server did not fail.");
        } catch (Exception e) {
            // Expected.
            Assert.assertEquals("Unexpected idle connections count.", 0, stats.getIdleCount());
            Assert.assertEquals("Unexpected in-use connections count.", 0, stats.getInUseCount());
            Assert.assertEquals("Unexpected total connections count.", 0, stats.getTotalConnectionCount());
            Assert.assertEquals("Did not receive a connect failed callback.", 1, metricEventsListener.getFailedCount());
        }
    }

    @Test
    public void testCallbacks() throws Exception {
        serverConnHandler.closeNewConnectionsOnReceive(false);
        strategy.incrementMaxConnections(1);

        PooledConnection<String, String> conn =
                (PooledConnection<String, String>) pool.acquire().toBlocking().last();
        Assert.assertEquals("Unexpected acquire attempted count.", 1, metricEventsListener.getAcquireAttemptedCount());
        Assert.assertEquals("Unexpected acquire succeeded count.", 1, metricEventsListener.getAcquireSucceededCount());
        Assert.assertEquals("Unexpected acquire failed count.", 0, metricEventsListener.getAcquireFailedCount());
        Assert.assertEquals("Unexpected create connection count.", 1, metricEventsListener.getCreationCount());

        conn.close();

        Assert.assertEquals("Unexpected release attempted count.", 1, metricEventsListener.getReleaseAttemptedCount());
        Assert.assertEquals("Unexpected release succeeded count.", 1, metricEventsListener.getReleaseSucceededCount());
        Assert.assertEquals("Unexpected release failed count.", 0, metricEventsListener.getReleaseFailedCount());
        Assert.assertEquals("Unexpected create connection count.", 1, metricEventsListener.getCreationCount());

        PooledConnection<String, String> reusedConn =
                (PooledConnection<String, String>) pool.acquire().toBlocking().last();

        Assert.assertEquals("Reused connection not same as original.", conn, reusedConn);

        Assert.assertEquals("Unexpected acquire attempted count.", 2, metricEventsListener.getAcquireAttemptedCount());
        Assert.assertEquals("Unexpected acquire succeeded count.", 2, metricEventsListener.getAcquireSucceededCount());
        Assert.assertEquals("Unexpected acquire failed count.", 0, metricEventsListener.getAcquireFailedCount());
        Assert.assertEquals("Unexpected create connection count.", 1, metricEventsListener.getCreationCount());
        Assert.assertEquals("Unexpected connection reuse count.", 1, metricEventsListener.getReuseCount());

        reusedConn.close();

        Assert.assertEquals("Unexpected release attempted count.", 2, metricEventsListener.getReleaseAttemptedCount());
        Assert.assertEquals("Unexpected release succeeded count.", 2, metricEventsListener.getReleaseSucceededCount());
        Assert.assertEquals("Unexpected release failed count.", 0, metricEventsListener.getReleaseFailedCount());
        Assert.assertEquals("Unexpected create connection count.", 1, metricEventsListener.getCreationCount());

        pool.discard(reusedConn);

        Assert.assertEquals("Unexpected release attempted count.", 2, metricEventsListener.getReleaseAttemptedCount());
        Assert.assertEquals("Unexpected release succeeded count.", 2, metricEventsListener.getReleaseSucceededCount());
        Assert.assertEquals("Unexpected release failed count.", 0, metricEventsListener.getReleaseFailedCount());
        Assert.assertEquals("Unexpected create connection count.", 1, metricEventsListener.getCreationCount());
        Assert.assertEquals("Unexpected evict connection count.", 1, metricEventsListener.getEvictionCount());

    }

    @Test
    public void testPoolExhaustion() throws Exception {
        serverConnHandler.closeNewConnectionsOnReceive(false);

        PooledConnection<String, String> connection = (PooledConnection<String, String>) acquireAndTestStats();

        try {
            pool.acquire().toBlocking().last();
            throw new AssertionError("Pool did not exhaust.");
        } catch (Exception e) {
            // expected
            if(e instanceof PoolExhaustedException) {
                Assert.assertEquals("Unexpected idle connection count on pool exhaustion", 0, stats.getIdleCount());
                Assert.assertEquals("Unexpected used connection count on pool exhaustion", 1, stats.getInUseCount());
                Assert.assertEquals("Unexpected total connection count on pool exhaustion", 1, stats.getTotalConnectionCount());
            }
        }

        // try release and acquire now.
        connection.close();

        PooledConnection<String, String> connection1 = (PooledConnection<String, String>) acquireAndTestStats();
        connection1.close();
        pool.discard(connection1);
        waitForClose();

        assertAllConnectionsReturned();
    }

    @Test
    public void testIdleCleanupThread() throws Exception {
        serverConnHandler.closeNewConnectionsOnReceive(false);
        pool.shutdown();
        MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject = new MetricEventsSubject<ClientMetricsEvent<?>>();
        factory.useMetricEventsSubject(eventsSubject);
        pool = new ConnectionPoolImpl<String, String>(serverInfo, PoolConfig.DEFAULT_CONFIG, strategy,
                                                      Executors.newScheduledThreadPool(1), factory, eventsSubject);

        stats = new PoolStats();
        pool.subscribe(stats);

        ObservableConnection<String, String> connection = acquireAndTestStats();
        connection.close();

        serverConnHandler.closeAllClientConnections();

        waitForClose();

        assertAllConnectionsReturned();
    }

    @Test
    public void testIdleTimeout() throws Exception {
        serverConnHandler.closeNewConnectionsOnReceive(false);
        PooledConnection<String, String> connection = (PooledConnection<String, String>) acquireAndTestStats();

        connection.close();
        Assert.assertTrue("Pooled connection is unusable after close.", connection.isUsable());

        Thread.sleep(MAX_IDLE_TIME_MILLIS + 10); // Wait for idle timeout.

        Assert.assertFalse("Pooled connection should have been unusable after idle timeout", connection.isUsable());
    }

    private void waitForClose() throws InterruptedException {
        if (!channelCloseListener.waitForClose(3, TimeUnit.MINUTES)) {
            throw new AssertionError("Client channel not closed after sufficient wait.");
        }
    }

    private void assertAllConnectionsReturned() {
        Assert.assertEquals("Unexpected pool idle count, post release.", 0, stats.getIdleCount());
        Assert.assertEquals("Unexpected pool in-use count, post release.", 0, stats.getInUseCount());
        Assert.assertEquals("Unexpected pool total connections count, post release.", 0,
                            stats.getTotalConnectionCount());
    }

    private ObservableConnection<String, String> acquireAndTestStats() throws InterruptedException {
        ObservableConnection<String, String> conn = pool.acquire().toBlocking().last();
        Assert.assertEquals("Unexpected pool idle count.", 0, stats.getIdleCount());
        Assert.assertEquals("Unexpected pool in-use count.", 1, stats.getInUseCount());
        Assert.assertEquals("Unexpected pool total connections count.", 1, stats.getTotalConnectionCount());
        final CountDownLatch writeFinishLatch = new CountDownLatch(1);
        conn.writeAndFlush("Hi").finallyDo(new Action0() {
            @Override
            public void call() {
                writeFinishLatch.countDown();
            }
        });
        writeFinishLatch.await(1, TimeUnit.SECONDS);
        return conn;
    }

    private static class ConnectionHandlerImpl implements ConnectionHandler<String, String> {

        private final String testId;
        private volatile boolean closeConnectionOnReceive = true;
        private final ConcurrentLinkedQueue<ObservableConnection<String, String>> lastReceivedConnection =
                new ConcurrentLinkedQueue<ObservableConnection<String, String>>();

        private ConnectionHandlerImpl(String testId) {
            this.testId = testId;
        }

        @Override
        public Observable<Void> handle(final ObservableConnection<String, String> newConnection) {
            long currentTime = System.currentTimeMillis();
            lastReceivedConnection.add(newConnection);
            System.out.println("Time: " + currentTime + ". Test Id: " + testId + ". Added a new connection on the server.");
            if (closeConnectionOnReceive) {
                System.out.println("Time: " + currentTime + ". Test Id: " + testId + ". Closed the newly created connection on the server.");
                return newConnection.close();
            } else {
                return Observable.create(new Observable.OnSubscribe<Void>() {
                    @Override
                    public void call(Subscriber<? super Void> subscriber) {
                        // keeps the connection alive forever.
                    }
                });
            }
        }

        public void closeNewConnectionsOnReceive(boolean closeConnectionOnReceive) {
            this.closeConnectionOnReceive = closeConnectionOnReceive;
        }

        public void closeAllClientConnections() {
            long currentTime = System.currentTimeMillis();
            if (lastReceivedConnection.size() <= 0) {
                throw new IllegalStateException("Time: " + currentTime + ". No connections on the server to close.");
            }
            Iterator<ObservableConnection<String, String>> iterator = lastReceivedConnection.iterator();
            while (iterator.hasNext()) {
                ObservableConnection<String, String> next = iterator.next();
                next.close();
                System.out.println("Time: " + currentTime + ". Test Id: " + testId + ". Removed a connection from the server.");
                iterator.remove();
            }
        }
    }
}
