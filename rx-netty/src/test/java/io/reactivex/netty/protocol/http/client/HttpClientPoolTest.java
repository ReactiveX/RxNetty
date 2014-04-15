package io.reactivex.netty.protocol.http.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.timeout.ReadTimeoutException;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.client.ChannelCloseListener;
import io.reactivex.netty.client.PoolConfig;
import io.reactivex.netty.client.PoolStats;
import io.reactivex.netty.client.TrackableStateChangeListener;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.pipeline.PipelineConfiguratorComposite;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.protocol.http.server.HttpServer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import rx.Observable;
import rx.functions.Action0;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author Nitesh Kant
 */
public class HttpClientPoolTest {

    private static HttpServer<ByteBuf, ByteBuf> mockServer;
    private static int port;

    private final ChannelCloseListener channelCloseListener = new ChannelCloseListener();
    private HttpClientImpl<ByteBuf,ByteBuf> client;
    private TrackableStateChangeListener stateChangeListener;

    @BeforeClass
    public static void init() throws Exception {
        port = 9998;
        mockServer = RxNetty.createHttpServer(port, new RequestProcessor()).start();
    }

    @AfterClass
    public static void shutdown() throws InterruptedException {
        mockServer.waitTillShutdown();
    }

    @After
    public void tearDown() throws Exception {
        if (null != client) {
            client.shutdown();
        }
    }

    @Test
    public void testBasicAcquireRelease() throws Exception {

        client = newHttpClient(2, PoolConfig.DEFAULT_CONFIG.getMaxIdleTimeMillis());

        PoolStats stats = client.getConnectionPool().getStats();

        HttpClientResponse<ByteBuf> response = submitAndWaitForCompletion(client, HttpClientRequest.createGet("/"), null);

        Assert.assertEquals("Unexpected HTTP response code.", 200, response.getStatus().code());
        Assert.assertEquals("Unexpected Idle connection count.", 1, stats.getIdleCount());
        Assert.assertEquals("Unexpected in use connection count.", 0, stats.getInUseCount());
        Assert.assertEquals("Unexpected total connection count.", 1, stats.getTotalConnectionCount());
    }

    @Test
    public void testBasicAcquireReleaseWithServerClose() throws Exception {

        client = newHttpClient(2, PoolConfig.DEFAULT_CONFIG.getMaxIdleTimeMillis());

        final PoolStats stats = client.getConnectionPool().getStats();
        final long[] idleCountOnComplete = {0};
        final long[] inUseCountOnComplete = {0};
        final long[] totalCountOnComplete = {0};

        Action0 onComplete = new Action0() {
            @Override
            public void call() {
                idleCountOnComplete[0] = stats.getIdleCount();
                inUseCountOnComplete[0] = stats.getInUseCount();
                totalCountOnComplete[0] = stats.getTotalConnectionCount();
            }
        };

        HttpClientResponse<ByteBuf> response = submitAndWaitForCompletion(client,
                                                                          HttpClientRequest.createGet("test/closeConnection"),
                                                                          onComplete);

        Assert.assertEquals("Unexpected idle connection count on completion of submit. ", 0, idleCountOnComplete[0]);
        Assert.assertEquals("Unexpected in-use connection count on completion of submit. ", 1, inUseCountOnComplete[0]);
        Assert.assertEquals("Unexpected total connection count on completion of submit. ", 1, totalCountOnComplete[0]);

        Assert.assertEquals("Unexpected HTTP response code.", 200, response.getStatus().code());
        Assert.assertEquals("Unexpected Idle connection count.", 0, stats.getIdleCount());
        Assert.assertEquals("Unexpected in use connection count.", 0, stats.getInUseCount());
        Assert.assertEquals("Unexpected total connection count.", 0, stats.getTotalConnectionCount());
        Assert.assertEquals("Unexpected connection eviction count.", 1, stateChangeListener.getEvictionCount());
    }

    @Test
    public void testReuse() throws Exception {
        client = newHttpClient(1, 1000000);
        PoolStats stats = client.getConnectionPool().getStats();

        HttpClientResponse<ByteBuf> response = submitAndWaitForCompletion(client, HttpClientRequest.createGet("/"), null);

        Assert.assertEquals("Unexpected HTTP response code.", 200, response.getStatus().code());
        Assert.assertEquals("Unexpected Idle connection count.", 1, stats.getIdleCount());
        Assert.assertEquals("Unexpected in use connection count.", 0, stats.getInUseCount());
        Assert.assertEquals("Unexpected total connection count.", 1, stats.getTotalConnectionCount());
        Assert.assertEquals("Unexpected reuse connection count.", 0, stateChangeListener.getReuseCount());

        response = submitAndWaitForCompletion(client, HttpClientRequest.createGet("/"), null);

        Assert.assertEquals("Unexpected HTTP response code.", 200, response.getStatus().code());
        Assert.assertEquals("Unexpected Idle connection count.", 1, stats.getIdleCount());
        Assert.assertEquals("Unexpected in use connection count.", 0, stats.getInUseCount());
        Assert.assertEquals("Unexpected total connection count.", 1, stats.getTotalConnectionCount());
        Assert.assertEquals("Unexpected reuse connection count.", 1, stateChangeListener.getReuseCount());
    }

    @Test
    public void testReadtimeoutCloseConnection() throws Exception {
        client = newHttpClient(1, PoolConfig.DEFAULT_CONFIG.getMaxIdleTimeMillis());
        final PoolStats stats = client.getConnectionPool().getStats();
        try {
            submitAndWaitForCompletion(client, HttpClientRequest.createGet("test/closeConnection"), null);
            throw new AssertionError("Expected read timeout error.");
        } catch (ReadTimeoutException e) {
            waitForClose();
            Assert.assertEquals("Unexpected Idle connection count.", 0, stats.getIdleCount());
            Assert.assertEquals("Unexpected in use connection count.", 0, stats.getInUseCount());
            Assert.assertEquals("Unexpected total connection count.", 0, stats.getTotalConnectionCount());
            Assert.assertEquals("Unexpected connection eviction count.", 1, stateChangeListener.getEvictionCount());
        }
    }

    @Test
    public void testCloseOnKeepAliveTimeout() throws Exception {
        client = newHttpClient(2, PoolConfig.DEFAULT_CONFIG.getMaxIdleTimeMillis());

        PoolStats stats = client.getConnectionPool().getStats();

        HttpClientResponse<ByteBuf> response = submitAndWaitForCompletion(client,
                                                                          HttpClientRequest.createGet("test/keepAliveTimeout"),
                                                                          null);

        Assert.assertEquals("Unexpected HTTP response code.", 200, response.getStatus().code());
        Assert.assertEquals("Unexpected Idle connection count.", 1, stats.getIdleCount());
        Assert.assertEquals("Unexpected in use connection count.", 0, stats.getInUseCount());
        Assert.assertEquals("Unexpected total connection count.", 1, stats.getTotalConnectionCount());
        Assert.assertEquals("Unexpected reuse connection count.", 0, stateChangeListener.getReuseCount());

        Thread.sleep(RequestProcessor.KEEP_ALIVE_TIMEOUT_SECONDS * 1000); // Waiting for keep-alive timeout to expire.

        response = submitAndWaitForCompletion(client, HttpClientRequest.createGet("/"), null);

        Assert.assertEquals("Unexpected HTTP response code.", 200, response.getStatus().code());
        Assert.assertEquals("Unexpected Idle connection count.", 1, stats.getIdleCount());
        Assert.assertEquals("Unexpected in use connection count.", 0, stats.getInUseCount());
        Assert.assertEquals("Unexpected total connection count.", 1, stats.getTotalConnectionCount());
        Assert.assertEquals("Unexpected reuse connection count.", 0, stateChangeListener.getReuseCount());
        Assert.assertEquals("Unexpected reuse connection count.", 1, stateChangeListener.getEvictionCount());
    }

    private static HttpClientResponse<ByteBuf> submitAndWaitForCompletion(HttpClientImpl<ByteBuf, ByteBuf> client,
                                                                          HttpClientRequest<ByteBuf> request,
                                                                          final Action0 onComplete)
            throws InterruptedException {
        final CountDownLatch completionLatch = new CountDownLatch(1);
        Observable<HttpClientResponse<ByteBuf>> submit = client.submit(request);
        if (null != onComplete) {
            submit = submit.doOnCompleted(onComplete);
        }
        HttpClientResponse<ByteBuf> response = submit.finallyDo(new Action0() {
            @Override
            public void call() {
                completionLatch.countDown();
            }
        }).toBlockingObservable().last();

        completionLatch.await(1, TimeUnit.MINUTES);

        return response;
    }

    private void waitForClose() throws InterruptedException {
        if (!channelCloseListener.waitForClose(1, TimeUnit.MINUTES)) {
            throw new AssertionError("Client channel not closed after sufficient wait.");
        }
    }

    private HttpClientImpl<ByteBuf, ByteBuf> newHttpClient(int maxConnections, long idleTimeout) {
        PipelineConfigurator<HttpClientResponse<ByteBuf>, HttpClientRequest<ByteBuf>> configurator =
                new PipelineConfiguratorComposite<HttpClientResponse<ByteBuf>, HttpClientRequest<ByteBuf>>(
                        PipelineConfigurators.httpClientConfigurator(),
                        new PipelineConfigurator() {
                            @Override
                            public void configureNewPipeline(ChannelPipeline pipeline) {
                                channelCloseListener.reset();
                                pipeline.addFirst(channelCloseListener);
                            }
                        });
        stateChangeListener = new TrackableStateChangeListener();
        return (HttpClientImpl<ByteBuf, ByteBuf>) new HttpClientBuilder<ByteBuf, ByteBuf>("localhost", port)
                .withMaxConnections(maxConnections)
                .withPoolStateChangeListener(stateChangeListener)
                .withIdleConnectionsTimeoutMillis(idleTimeout)
                .pipelineConfigurator(configurator).build();
    }
}
