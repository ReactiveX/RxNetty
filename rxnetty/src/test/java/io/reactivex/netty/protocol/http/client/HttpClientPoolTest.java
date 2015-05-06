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

package io.reactivex.netty.protocol.http.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.timeout.ReadTimeoutException;
import io.reactivex.netty.ChannelCloseListener;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.client.PoolConfig;
import io.reactivex.netty.client.PoolStats;
import io.reactivex.netty.client.TrackableMetricEventsListener;
import io.reactivex.netty.metrics.HttpClientMetricEventsListener;
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
import rx.functions.Action1;
import rx.functions.Func1;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Nitesh Kant
 */
public class HttpClientPoolTest {

    private static HttpServer<ByteBuf, ByteBuf> mockServer;
    private static int port;

    private final ChannelCloseListener channelCloseListener = new ChannelCloseListener();
    private HttpClientImpl<ByteBuf,ByteBuf> client;
    private TrackableMetricEventsListener stateChangeListener;
    private PoolStats stats;

    @BeforeClass
    public static void init() throws Exception {
        mockServer = RxNetty.newHttpServerBuilder(0, new RequestProcessor()).enableWireLogging(LogLevel.ERROR)
                            .build().start();
        port = mockServer.getServerPort();
    }

    @AfterClass
    public static void shutdown() throws InterruptedException {
        mockServer.shutdown();
        mockServer.waitTillShutdown(1, TimeUnit.MINUTES);
    }

    @After
    public void tearDown() throws Exception {
        if (null != client) {
            client.shutdown();
        }
    }

    @Test
    public void testBasicAcquireRelease() throws Exception {

        client = newHttpClient(2, PoolConfig.DEFAULT_CONFIG.getMaxIdleTimeMillis(), null);

        HttpClientResponse<ByteBuf> response = submitAndWaitForCompletion(client, HttpClientRequest.createGet("/"), null
        );

        Assert.assertEquals("Unexpected HTTP response code.", 200, response.getStatus().code());
        Assert.assertEquals("Unexpected Idle connection count.", 1, stats.getIdleCount());
        Assert.assertEquals("Unexpected in use connection count.", 0, stats.getInUseCount());
        Assert.assertEquals("Unexpected total connection count.", 1, stats.getTotalConnectionCount());
    }

    @Test
    public void testBasicAcquireReleaseWithServerClose() throws Exception {

        client = newHttpClient(2, PoolConfig.DEFAULT_CONFIG.getMaxIdleTimeMillis(), null);

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
        Assert.assertEquals("Unexpected in-use connection count on completion of submit. ", 0, inUseCountOnComplete[0]);
        Assert.assertEquals("Unexpected total connection count on completion of submit. ", 0, totalCountOnComplete[0]);

        Assert.assertEquals("Unexpected HTTP response code.", 200, response.getStatus().code());
        Assert.assertEquals("Unexpected Idle connection count.", 0, stats.getIdleCount());
        Assert.assertEquals("Unexpected in use connection count.", 0, stats.getInUseCount());
        Assert.assertEquals("Unexpected total connection count.", 0, stats.getTotalConnectionCount());
        Assert.assertEquals("Unexpected connection eviction count.", 1, stateChangeListener.getEvictionCount());
    }

    @Test
    public void testReadtimeoutCloseConnection() throws Exception {
        HttpClient.HttpClientConfig conf = new HttpClient.HttpClientConfig.Builder().readTimeout(1, TimeUnit.SECONDS).build();
        client = newHttpClient(1, PoolConfig.DEFAULT_CONFIG.getMaxIdleTimeMillis(), conf);
        try {
            submitAndWaitForCompletion(client, HttpClientRequest.createGet("test/timeout?timeout=60000"), null);
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
        client = newHttpClient(2, PoolConfig.DEFAULT_CONFIG.getMaxIdleTimeMillis(), null);

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

    @Test
    public void testReuseWithContent() throws Exception {
        client = newHttpClient(1, 1000000, null);

        List<String> content = submitAndConsumeContent(client, HttpClientRequest.createGet("/"));
        Assert.assertEquals("Unexpected content fragments count.", 1, content.size());
        Assert.assertEquals("Unexpected content fragment.", RequestProcessor.SINGLE_ENTITY_BODY, content.get(0));

        Assert.assertEquals("Unexpected Idle connection count.", 1, stats.getIdleCount());
        Assert.assertEquals("Unexpected in use connection count.", 0, stats.getInUseCount());
        Assert.assertEquals("Unexpected total connection count.", 1, stats.getTotalConnectionCount());
        Assert.assertEquals("Unexpected reuse connection count.", 0, stateChangeListener.getReuseCount());

        content = submitAndConsumeContent(client, HttpClientRequest.createGet("/"));

        Assert.assertEquals("Unexpected content fragments count.", 1, content.size());
        Assert.assertEquals("Unexpected content fragment.", RequestProcessor.SINGLE_ENTITY_BODY, content.get(0));

        Assert.assertEquals("Unexpected Idle connection count.", 1, stats.getIdleCount());
        Assert.assertEquals("Unexpected in use connection count.", 0, stats.getInUseCount());
        Assert.assertEquals("Unexpected total connection count.", 1, stats.getTotalConnectionCount());
        Assert.assertEquals("Unexpected reuse connection count.", 1, stateChangeListener.getReuseCount());
    }

    @Test
    public void testPoolEvictions() throws InterruptedException {
        client = newHttpClient(1, 1000, null);
        final CountDownLatch latch = new CountDownLatch(1);
        HttpClientMetricEventsListener listener = new HttpClientMetricEventsListener() {
            @Override
            protected void onPooledConnectionEviction() {
                latch.countDown();
            }
        };

        client.subscribe(listener);

        submitAndConsumeContent(client, HttpClientRequest.createGet("/"));
        HttpClientRequest<ByteBuf> request = HttpClientRequest.createGet("/");

        client.submit(request).subscribe();
        latch.await(20, TimeUnit.SECONDS); // Wait less than default idle timeout (30 seconds)
        Assert.assertEquals("Pooled connection not evicted after idle timeout.", 0, latch.getCount());
    }

    @Test(expected=ReadTimeoutException.class)
    public void testReadtimeoutForReusedConnection() throws Exception {
        HttpClient.HttpClientConfig conf = new HttpClient.HttpClientConfig.Builder()
                .readTimeout(1, TimeUnit.SECONDS).build();
        client = newHttpClient(1, PoolConfig.DEFAULT_CONFIG.getMaxIdleTimeMillis(), conf);
        //first make a normal request with no timeout to let it get to the pool
        submitAndWaitForCompletion(client, HttpClientRequest.createGet("/"), null);
        //now try the pooled connection with response delay > timeout
        submitAndWaitForCompletion(client, HttpClientRequest.createGet("test/timeout?timeout=10000"), null);
    }

    private static List<String> submitAndConsumeContent(HttpClientImpl<ByteBuf, ByteBuf> client,
                                                        HttpClientRequest<ByteBuf> request)
            throws InterruptedException {
        final List<String> toReturn = new ArrayList<String>();
        final CountDownLatch completionLatch = new CountDownLatch(1);
        Observable<HttpClientResponse<ByteBuf>> response = client.submit(request);
        response.flatMap(new Func1<HttpClientResponse<ByteBuf>, Observable<String>>() {
            @Override
            public Observable<String> call(HttpClientResponse<ByteBuf> response) {
                if (response.getStatus().code() == 200) {
                    return response.getContent().map(new Func1<ByteBuf, String>() {
                        @Override
                        public String call(ByteBuf byteBuf) {
                            return byteBuf.toString(Charset.defaultCharset());
                        }
                    });
                } else {
                    return Observable.error(new AssertionError("Unexpected response code: " + response.getStatus().code()));
                }
            }
        }).finallyDo(new Action0() {
            @Override
            public void call() {
                completionLatch.countDown();
            }
        }).toBlocking().forEach(new Action1<String>() {
            @Override
            public void call(String s) {
                toReturn.add(s);
            }
        });

        completionLatch.await(1, TimeUnit.MINUTES);
        return toReturn;
    }

    private static HttpClientResponse<ByteBuf> submitAndWaitForCompletion(HttpClientImpl<ByteBuf, ByteBuf> client,
                                                                          HttpClientRequest<ByteBuf> request,
                                                                          final Action0 onComplete)
            throws InterruptedException {
        final CountDownLatch completionLatch = new CountDownLatch(1);
        Observable<HttpClientResponse<ByteBuf>> submit = client.submit(request);
        final AtomicReference<HttpClientResponse<ByteBuf>> toReturnRef =
                new AtomicReference<HttpClientResponse<ByteBuf>>();

        Observable<ByteBuf> contentStream =
                submit.flatMap(new Func1<HttpClientResponse<ByteBuf>, Observable<ByteBuf>>() {
                    @Override
                    public Observable<ByteBuf> call(HttpClientResponse<ByteBuf> response) {
                        toReturnRef.set(response);
                        return response.getContent();
                    }
                }).finallyDo(new Action0() {
                    @Override
                    public void call() {
                        completionLatch.countDown();

                    }
                }).ignoreElements();

        if (null != onComplete) {
            contentStream = contentStream.doOnCompleted(onComplete);
        }

        contentStream.toBlocking().lastOrDefault(null);

        completionLatch.await(1, TimeUnit.MINUTES);

        return toReturnRef.get();
    }

    private void waitForClose() throws InterruptedException {
        if (!channelCloseListener.waitForClose(1, TimeUnit.MINUTES)) {
            throw new AssertionError("Client channel not closed after sufficient wait.");
        }
    }

    private HttpClientImpl<ByteBuf, ByteBuf> newHttpClient(int maxConnections, long idleTimeout,
                                                           HttpClient.HttpClientConfig clientConfig) {
        if (null == clientConfig) {
            clientConfig = HttpClient.HttpClientConfig.Builder.newDefaultConfig();
        }
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

        HttpClientImpl<ByteBuf, ByteBuf> client =
                (HttpClientImpl<ByteBuf, ByteBuf>) new HttpClientBuilder<ByteBuf, ByteBuf>("localhost", port)
                        .withMaxConnections(maxConnections)
                        .withIdleConnectionsTimeoutMillis(idleTimeout)
                        .config(clientConfig)
                        .enableWireLogging(LogLevel.DEBUG)
                        .pipelineConfigurator(configurator).build();
        stateChangeListener = new TrackableMetricEventsListener();
        client.subscribe(stateChangeListener);
        stats = new PoolStats();
        client.subscribe(stats);
        return client;
    }
}
