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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelOption;
import io.netty.channel.ConnectTimeoutException;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.timeout.ReadTimeoutException;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.client.RxClient;
import io.reactivex.netty.client.RxClient.ClientConfig.Builder;
import io.reactivex.netty.client.RxClient.ServerInfo;
import io.reactivex.netty.client.pool.AbstractQueueBasedChannelPool.PoolExhaustedException;
import io.reactivex.netty.client.pool.DefaultChannelPool;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.protocol.http.client.HttpClient.HttpClientConfig;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.HttpServerBuilder;
import io.reactivex.netty.protocol.text.sse.ServerSentEvent;
import io.reactivex.netty.server.RxServerThreadFactory;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import rx.Observable;
import rx.Observer;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;

import java.net.ConnectException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class HttpClientTest {
    private static HttpServer<ByteBuf, ByteBuf> server;

    private static int port;

    @BeforeClass
    public static void init() {
        port = new Random().nextInt(1000) + 4000;
        HttpServerBuilder<ByteBuf, ByteBuf> builder 
            = new HttpServerBuilder<ByteBuf, ByteBuf>(new ServerBootstrap().group(new NioEventLoopGroup(10, new RxServerThreadFactory())), port, new RequestProcessor());
        server = builder.build();
        server.start();
    }

    @AfterClass
    public static void shutDown() throws InterruptedException {
        server.shutdown();
    }
    private String invokeBlockingCall(HttpClient<ByteBuf, ByteBuf> client, String uri) {
        return invokeBlockingCall(client, HttpClientRequest.createGet(uri));
    }
    
    private String invokeBlockingCall(HttpClient<ByteBuf, ByteBuf> client, HttpClientRequest<ByteBuf> request) {
        Observable<HttpClientResponse<ByteBuf>> response = client.submit(request);
        return response.flatMap(new Func1<HttpClientResponse<ByteBuf>, Observable<String>>() {
            @Override
            public Observable<String> call(HttpClientResponse<ByteBuf> response) {
                return response.getContent().map(new Func1<ByteBuf, String>() {
                    @Override
                    public String call(ByteBuf byteBuf) {
                        return byteBuf.toString(Charset.defaultCharset());
                    }
                });
            }
        }).toBlockingObservable().single();
    }

    @Test
    public void testConnectionClose() throws Exception {
        HttpClientImpl<ByteBuf, ByteBuf> client = (HttpClientImpl<ByteBuf, ByteBuf>) RxNetty.createHttpClient( "localhost", port);
        Observable<ObservableConnection<HttpClientResponse<ByteBuf>,HttpClientRequest<ByteBuf>>> connectionObservable = client.connect().cache();

        final Observable<HttpClientResponse<ByteBuf>> response = client.submit(HttpClientRequest.createGet("test/singleEntity"), connectionObservable);
        ObservableConnection<HttpClientResponse<ByteBuf>, HttpClientRequest<ByteBuf>> conn = connectionObservable.toBlockingObservable().last();
        Assert.assertFalse("Connection already closed.", conn.isCloseIssued());
        final Object responseCompleteMonitor = new Object();
        response.finallyDo(new Action0() {
            @Override
            public void call() {
                synchronized (responseCompleteMonitor) {
                    responseCompleteMonitor.notifyAll();
                }
            }
        }).subscribe();
        synchronized (responseCompleteMonitor) {
            responseCompleteMonitor.wait(60*1000);
        }
        assertTrue("Connection not closed after response recieved.", conn.isCloseIssued());
    }

    @Test
    public void testChunkedStreaming() throws Exception {
        HttpClient<ByteBuf, ServerSentEvent> client = RxNetty.createHttpClient("localhost", port,
                                                                        PipelineConfigurators.<ByteBuf>sseClientConfigurator());
        Observable<HttpClientResponse<ServerSentEvent>> response =
                client.submit(HttpClientRequest.createGet("test/stream"));

        final List<String> result = new ArrayList<String>();
        readResponseContent(response, result);
        assertEquals(RequestProcessor.smallStreamContent, result);
    }

    @Test
    public void testMultipleChunks() throws Exception {
        HttpClient<ByteBuf, ServerSentEvent> client = RxNetty.createHttpClient("localhost", port,
                                                                        PipelineConfigurators
                                                                                .<ByteBuf>sseClientConfigurator());
        Observable<HttpClientResponse<ServerSentEvent>> response =
                client.submit(HttpClientRequest.createDelete("test/largeStream"));

        final List<String> result = new ArrayList<String>();
        readResponseContent(response, result);
        assertEquals(RequestProcessor.largeStreamContent, result);
    }

    @Test
    public void testMultipleChunksWithTransformation() throws Exception {
        HttpClient<ByteBuf, ServerSentEvent> client = RxNetty.createHttpClient("localhost", port,
                                                                        PipelineConfigurators
                                                                                .<ByteBuf>sseClientConfigurator());
        Observable<HttpClientResponse<ServerSentEvent>> response =
                client.submit(HttpClientRequest.createGet("test/largeStream"));
        Observable<String> transformed = response.flatMap(new Func1<HttpClientResponse<ServerSentEvent>, Observable<String>>() {
            @Override
            public Observable<String> call(HttpClientResponse<ServerSentEvent> httpResponse) {
                if (httpResponse.getStatus().equals(HttpResponseStatus.OK)) {
                    return httpResponse.getContent().map(new Func1<ServerSentEvent, String>() {
                        @Override
                        public String call(ServerSentEvent sseEvent) {
                            return sseEvent.getEventData();
                        }
                    });
                }
                return Observable.error(new RuntimeException("Unexpected response"));
            }
        });

       final List<String> result = new ArrayList<String>();
        transformed.toBlockingObservable().forEach(new Action1<String>() {

            @Override
            public void call(String t1) {
                result.add(t1);
            }
        });
        assertEquals(RequestProcessor.largeStreamContent, result);
    }

    @Test
    public void testSingleEntity() throws Exception {
        HttpClient<ByteBuf, ByteBuf> client = RxNetty.createHttpClient("localhost", port);
        Observable<HttpClientResponse<ByteBuf>> response = client.submit(HttpClientRequest.createGet("test/singleEntity"));
        final List<String> result = new ArrayList<String>();
        response.flatMap(new Func1<HttpClientResponse<ByteBuf>, Observable<String>>() {
            @Override
            public Observable<String> call(HttpClientResponse<ByteBuf> response) {
                return response.getContent().map(new Func1<ByteBuf, String>() {
                    @Override
                    public String call(ByteBuf byteBuf) {
                        return byteBuf.toString(Charset.defaultCharset());
                    }
                });
            }
        }).toBlockingObservable().forEach(new Action1<String>() {

            @Override
            public void call(String t1) {
                result.add(t1);
            }
        });
        assertEquals(1, result.size());
        assertEquals("Hello world", result.get(0));
    }

    @Test
    public void testPost() throws Exception {
        HttpClient<ByteBuf, ByteBuf> client = RxNetty.createHttpClient("localhost", port);
        HttpClientRequest<ByteBuf> request = HttpClientRequest.createPost("test/post")
                .withContent("Hello world");
        RepeatableContentHttpRequest<ByteBuf> repeatable = new RepeatableContentHttpRequest<ByteBuf>(request);
        Observable<HttpClientResponse<ByteBuf>> response = client.submit(repeatable);
        final List<String> result = new ArrayList<String>();
        response.flatMap(new Func1<HttpClientResponse<ByteBuf>, Observable<String>>() {
            @Override
            public Observable<String> call(HttpClientResponse<ByteBuf> response) {
                return response.getContent().map(new Func1<ByteBuf, String>() {
                    @Override
                    public String call(ByteBuf byteBuf) {
                        return byteBuf.toString(Charset.defaultCharset());
                    }
                });
            }
        }).toBlockingObservable().forEach(new Action1<String>() {

            @Override
            public void call(String t1) {
                result.add(t1);
            }
        });
        assertEquals(1, result.size());
        assertEquals("Hello world", result.get(0));
        
        // resend the same request to make sure it is repeatable
        response = client.submit(repeatable);
        result.clear();
        response.flatMap(new Func1<HttpClientResponse<ByteBuf>, Observable<String>>() {
            @Override
            public Observable<String> call(HttpClientResponse<ByteBuf> response) {
                return response.getContent().map(new Func1<ByteBuf, String>() {
                    @Override
                    public String call(ByteBuf byteBuf) {
                        return byteBuf.toString(Charset.defaultCharset());
                    }
                });
            }
        }).toBlockingObservable().forEach(new Action1<String>() {

            @Override
            public void call(String t1) {
                result.add(t1);
            }
        });
        assertEquals(1, result.size());
        assertEquals("Hello world", result.get(0));
    }
    
    @Test
    public void testNonChunkingStream() throws Exception {
        HttpClient<ByteBuf, ServerSentEvent> client = RxNetty.createHttpClient("localhost", port,
                                                                        PipelineConfigurators.<ByteBuf>sseClientConfigurator());
        Observable<HttpClientResponse<ServerSentEvent>> response =
                client.submit(HttpClientRequest.createGet("test/nochunk_stream"));
        final List<String> result = new ArrayList<String>();
        response.flatMap(new Func1<HttpClientResponse<ServerSentEvent>, Observable<ServerSentEvent>>() {
            @Override
            public Observable<ServerSentEvent> call(HttpClientResponse<ServerSentEvent> httpResponse) {
                return httpResponse.getContent();
            }
        }).toBlockingObservable().forEach(new Action1<ServerSentEvent>() {
            @Override
            public void call(ServerSentEvent event) {
                result.add(event.getEventData());
            }
        });
        assertEquals(RequestProcessor.smallStreamContent, result);
    }

    @Test
    public void testConnectException() throws Exception {
        HttpClientBuilder<ByteBuf, ByteBuf> clientBuilder = new HttpClientBuilder<ByteBuf, ByteBuf>("localhost", 8182);
        HttpClient<ByteBuf, ByteBuf> client = clientBuilder.channelOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 100).build();
        Observable<HttpClientResponse<ByteBuf>> response =
                client.submit(HttpClientRequest.createGet("/"));
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Throwable> ex = new AtomicReference<Throwable>();
        response.subscribe(new Observer<HttpClientResponse<ByteBuf>>() {
            @Override
            public void onCompleted() {
                latch.countDown();
            }

            @Override
            public void onError(Throwable e) {
                ex.set(e);
                latch.countDown();
            }

            @Override
            public void onNext(HttpClientResponse<ByteBuf> args) {
            }
        });
        latch.await();
        assertNotNull(ex.get());
        assertTrue(ex.get() instanceof ConnectException);
    }

    @Test
    public void testConnectException2() throws Exception {
        HttpClientBuilder<ByteBuf, ByteBuf> clientBuilder = new HttpClientBuilder<ByteBuf, ByteBuf>("www.google.com", 81);
        HttpClient<ByteBuf, ByteBuf> client = clientBuilder.channelOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10).build();
        Observable<HttpClientResponse<ByteBuf>> response = client.submit(HttpClientRequest.createGet("/"));
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Throwable> ex = new AtomicReference<Throwable>();
        response.subscribe(new Observer<HttpClientResponse<ByteBuf>>() {
            @Override
            public void onCompleted() {
                latch.countDown();
            }

            @Override
            public void onError(Throwable e) {
                ex.set(e);
                latch.countDown();
            }

            @Override
            public void onNext(HttpClientResponse<ByteBuf> args) {
            }
        });
        latch.await(10, TimeUnit.SECONDS);
        assertTrue(ex.get() instanceof ConnectTimeoutException);
    }

    @Test
    public void testTimeout() throws Exception {
        RxClient.ClientConfig clientConfig = new Builder(null)
                .readTimeout(10, TimeUnit.MILLISECONDS).build();
        HttpClient<ByteBuf, ByteBuf> client = new HttpClientBuilder<ByteBuf, ByteBuf>("localhost", port).config(
                clientConfig).build();
        Observable<HttpClientResponse<ByteBuf>> response =
                client.submit(HttpClientRequest.createGet("test/timeout?timeout=10000"));

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Throwable> exception = new AtomicReference<Throwable>();
        response.subscribe(new Observer<HttpClientResponse<ByteBuf>>() {
            @Override
            public void onCompleted() {
                latch.countDown();
            }

            @Override
            public void onError(Throwable e) {
                exception.set(e);
                latch.countDown();
            }

            @Override
            public void onNext(HttpClientResponse<ByteBuf> response) {
                latch.countDown();
            }
        });
        if (!latch.await(2, TimeUnit.SECONDS)) {
            fail("Observer is not called without timeout");
        } else {
            assertTrue(exception.get() instanceof ReadTimeoutException);
        }
    }

    

    @Test
    public void testNoReadTimeout() throws Exception {
        RxClient.ClientConfig clientConfig = new Builder(null)
                .readTimeout(2, TimeUnit.SECONDS).build();
        HttpClient<ByteBuf, ByteBuf> client = new HttpClientBuilder<ByteBuf, ByteBuf>("localhost", port).config(clientConfig).build();
        Observable<HttpClientResponse<ByteBuf>> response =
                client.submit(HttpClientRequest.createGet("test/singleEntity"));

        final AtomicReference<Throwable> exceptionHolder = new AtomicReference<Throwable>();
        final int[] status = {0};
        response.subscribe(new Observer<HttpClientResponse<ByteBuf>>() {
            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
                exceptionHolder.set(e);
            }

            @Override
            public void onNext(HttpClientResponse<ByteBuf> response) {
                status[0] = response.getStatus().code();
            }
        });
        Thread.sleep(3000);
        if (exceptionHolder.get() != null) {
            exceptionHolder.get().printStackTrace();
        }
        assertEquals(200, status[0]);
        assertNull(exceptionHolder.get());
    }

    
    @Test
    public void testChannelPool() throws Exception {
        HttpClientBuilder<ByteBuf, ByteBuf> clientBuilder = new HttpClientBuilder<ByteBuf, ByteBuf>("www.google.com", 80);
        DefaultChannelPool pool = new DefaultChannelPool(10);
        clientBuilder.channelPool(pool);
        HttpClient<ByteBuf, ByteBuf> client = clientBuilder.build();
        String content = invokeBlockingCall(client, "/");
        assertNotNull(content);
        // connection recycling happen asynchronously
        Thread.sleep(1000);
        assertEquals(1, pool.getIdleChannels());
        
        content = invokeBlockingCall(client, "/");
        assertNotNull(content);
        // connection recycling happen asynchronously
        Thread.sleep(1000);
        assertEquals(1, pool.getIdleChannels());
        assertEquals(1, pool.getTotalChannelsInPool());
        assertEquals(1, pool.getCreationCount());
        assertEquals(1, pool.getReuseCount());
        assertEquals(2, pool.getSuccessfulRequestCount());
        assertEquals(2, pool.getReleaseCount());
        assertEquals(0, pool.getDeletionCount());
        
        // create a new client using the same pool
        clientBuilder = new HttpClientBuilder<ByteBuf, ByteBuf>("www.google.com", 80);
        clientBuilder.channelPool(pool);        
        client = clientBuilder.build();
        content = invokeBlockingCall(client, "/");
        assertNotNull(content);
        Thread.sleep(1000);
        assertEquals(1, pool.getIdleChannels());
        assertEquals(1, pool.getTotalChannelsInPool());
        assertEquals(1, pool.getCreationCount());
        assertEquals(2, pool.getReuseCount());
        assertEquals(3, pool.getSuccessfulRequestCount());
        assertEquals(3, pool.getReleaseCount());
        assertEquals(0, pool.getDeletionCount());
    }
    
    @Test
    public void testChannelPoolIdleTimeout() throws Exception {
        HttpClientBuilder<ByteBuf, ByteBuf> clientBuilder = new HttpClientBuilder<ByteBuf, ByteBuf>("www.google.com", 80);
        // idle timeout after 500 ms
        DefaultChannelPool pool = new DefaultChannelPool(10, 500);
        clientBuilder.channelPool(pool);
        HttpClient<ByteBuf, ByteBuf> client = clientBuilder.build();
        String content = invokeBlockingCall(client, "/");
        assertNotNull(content);
        Thread.sleep(1000);
        
        content = invokeBlockingCall(client, "/");
        // previous connection should timed out, no reuse
        Thread.sleep(1000);
        assertEquals(1, pool.getIdleChannels());
        assertEquals(1, pool.getTotalChannelsInPool());
        assertEquals(2, pool.getCreationCount());
        assertEquals(0, pool.getReuseCount());
        assertEquals(2, pool.getSuccessfulRequestCount());
        assertEquals(2, pool.getReleaseCount());
        assertEquals(1, pool.getDeletionCount());
    }
    
    @Test
    public void testCloseConnection() throws Exception {
        HttpClientBuilder<ByteBuf, ByteBuf> clientBuilder = new HttpClientBuilder<ByteBuf, ByteBuf>("localhost", port);
        // idle timeout after 500 ms
        DefaultChannelPool pool = new DefaultChannelPool(10);
        clientBuilder.channelPool(pool);
        HttpClient<ByteBuf, ByteBuf> client = clientBuilder.build();
        String content = invokeBlockingCall(client, "test/closeConnection");
        assertNotNull(content);
        Thread.sleep(1000);
        assertEquals(0, pool.getIdleChannels());
        assertEquals(0, pool.getTotalChannelsInPool());
        assertEquals(1, pool.getCreationCount());
        assertEquals(1, pool.getSuccessfulRequestCount());
        assertEquals(1, pool.getReleaseCount());
        assertEquals(1, pool.getDeletionCount());
    }
    
    @Test
    public void testKeepAliveTimeout() throws Exception {
        HttpClientBuilder<ByteBuf, ByteBuf> clientBuilder = new HttpClientBuilder<ByteBuf, ByteBuf>("localhost", port);
        // idle timeout after 500 ms
        DefaultChannelPool pool = new DefaultChannelPool(10);
        clientBuilder.channelPool(pool);
        HttpClient<ByteBuf, ByteBuf> client = clientBuilder.build();
        // issue two requests and the second one should reuse the first connection
        String content = invokeBlockingCall(client, "test/keepAliveTimeout");
        assertNotNull(content);
        // wait until connection is released
        Thread.sleep(200);
        content = invokeBlockingCall(client, "test/keepAliveTimeout");
        assertNotNull(content);
        Thread.sleep(200);
        assertEquals(1, pool.getReuseCount());
        assertEquals(1, pool.getIdleChannels());
        assertEquals(1, pool.getTotalChannelsInPool());
        assertEquals(1, pool.getCreationCount());
        assertEquals(2, pool.getSuccessfulRequestCount());
        assertEquals(2, pool.getReleaseCount());
        assertEquals(0, pool.getDeletionCount());
        
        // try again, this will not reuse the connection as Keep-Alive header from previous response
        Thread.sleep(1000);
        content = invokeBlockingCall(client, "test/keepAliveTimeout");
        Thread.sleep(1000);
        // no increase in the channel reuse
        assertEquals(1, pool.getReuseCount());
        assertEquals(1, pool.getDeletionCount());
        assertEquals(1, pool.getIdleChannels());
        assertEquals(1, pool.getTotalChannelsInPool());
        assertEquals(2, pool.getCreationCount());
    }

    private Throwable waitForError(HttpClient<ByteBuf, ByteBuf> client, String uri) throws Exception {
        final Throwable[] error = new Throwable[1];
        final CountDownLatch latch = new CountDownLatch(1);
        Observable<HttpClientResponse<ByteBuf>> response = client.submit(HttpClientRequest.createGet(uri));
        response.subscribe(new Action1<HttpClientResponse<ByteBuf>>() {
            @Override
            public void call(HttpClientResponse<ByteBuf> t1) {
            }
        }, new Action1<Throwable>() {

            @Override
            public void call(Throwable t1) {
                error[0] = t1;
                latch.countDown();
            }
        });
        latch.await(30, TimeUnit.SECONDS);
        return error[0];
    }
    
    @Test
    public void testReadtimeoutCloseConnection() throws Exception {
        RxClient.ClientConfig clientConfig = new Builder(null)
                .readTimeout(100, TimeUnit.MILLISECONDS).build();
        DefaultChannelPool pool = new DefaultChannelPool(10);
        HttpClient<ByteBuf, ByteBuf> client = new HttpClientBuilder<ByteBuf, ByteBuf>("localhost", port).config(
                clientConfig).channelPool(pool).build();
        Throwable error = waitForError(client, "test/timeout?timeout=1000");
        assertNotNull(error);
        assertTrue(error instanceof ReadTimeoutException);
        Thread.sleep(1200);
        assertEquals(1, pool.getIdleChannels());
        assertEquals(1, pool.getTotalChannelsInPool());
        assertEquals(1, pool.getCreationCount());
        assertEquals(1, pool.getSuccessfulRequestCount());
        assertEquals(1, pool.getReleaseCount());
        //assertEquals(1, pool.getDeletionCount()); TODO: We should listen to channel close events, which should trigger this deletion.
    }
    
    @Test
    public void testPoolExhaustedException() throws Exception {
        DefaultChannelPool pool = new DefaultChannelPool(2);
        HttpClient<ByteBuf, ByteBuf> client = new HttpClientBuilder<ByteBuf, ByteBuf>("localhost", port).channelPool(pool).build();
        client.submit(HttpClientRequest.createGet("test/timeout?timeout=2000")).subscribe(new Action1<HttpClientResponse<ByteBuf>>() {
            @Override
            public void call(HttpClientResponse<ByteBuf> t1) {
            }
        });
        client.submit(HttpClientRequest.createGet("test/timeout?timeout=2000")).subscribe(new Action1<HttpClientResponse<ByteBuf>>() {
            @Override
            public void call(HttpClientResponse<ByteBuf> t1) {
            }
        });
        Thread.sleep(1000);
        assertEquals(2, pool.getTotalChannelsInPool());
        Throwable error = waitForError(client, "test/timeout?timeout=2000");
        assertNotNull(error);
        assertTrue(error instanceof PoolExhaustedException);
        assertEquals(1, pool.getFailedRequestCount());
    }
    
    @Test
    public void testConnectExceptionFromPool() throws Exception {
        DefaultChannelPool pool = new DefaultChannelPool(2);
        HttpClient<ByteBuf, ByteBuf> client = new HttpClientBuilder<ByteBuf, ByteBuf>("localhost", 12345).channelPool(pool).build();
        Throwable error = waitForError(client, "/");
        assertNotNull(error);
        assertTrue(error instanceof ConnectException);
        assertEquals(1, pool.getFailedRequestCount());
    }

    @Test
    public void testIdleChannelsRemoval() throws Exception {
        DefaultChannelPool pool = new DefaultChannelPool(2);
        HttpClient<ByteBuf, ByteBuf> client = new HttpClientBuilder<ByteBuf, ByteBuf>("localhost", port).channelPool(pool).build();
        client.submit(HttpClientRequest.createGet("test/timeout?timeout=1000")).subscribe(new Action1<HttpClientResponse<ByteBuf>>() {
            @Override
            public void call(HttpClientResponse<ByteBuf> t1) {
            }
        });
        client.submit(HttpClientRequest.createGet("test/timeout?timeout=1000")).subscribe(new Action1<HttpClientResponse<ByteBuf>>() {
            @Override
            public void call(HttpClientResponse<ByteBuf> t1) {
            }
        });
        Thread.sleep(1500);
        assertEquals(2, pool.getTotalChannelsInPool());
        assertEquals(2, pool.getIdleChannels());
        // pool has reached to its capacity, but we should be able to create new channel since there are
        // idle channels that can be removed
        client = new HttpClientBuilder<ByteBuf, ByteBuf>("www.google.com", 80).channelPool(pool).build();
        String content = invokeBlockingCall(client, "/");
        assertNotNull(content);
        Thread.sleep(1000);
        assertEquals(2, pool.getIdleChannels());
        assertEquals(2, pool.getTotalChannelsInPool());
        assertEquals(3, pool.getCreationCount());
        assertEquals(3, pool.getSuccessfulRequestCount());
        assertEquals(3, pool.getReleaseCount());
        assertEquals(1, pool.getDeletionCount());
        assertEquals(1, pool.getIdleQueue(new ServerInfo("localhost", port)).size());
        assertEquals(1, pool.getIdleQueue(new ServerInfo("www.google.com", 80)).size());
        
        int count = pool.cleanUpIdleChannels();
        assertEquals(2, count);
        assertEquals(0, pool.getIdleChannels());
        assertEquals(0, pool.getTotalChannelsInPool());
    }
    
    @Test
    public void testRedirect() {
        DefaultChannelPool pool = new DefaultChannelPool(2);
        HttpClientConfig.Builder builder = new HttpClientConfig.Builder(null);
        HttpClientConfig config = builder.readTimeout(20000, TimeUnit.MILLISECONDS)
                .build();
        HttpClient<ByteBuf, ByteBuf> client = new HttpClientBuilder<ByteBuf, ByteBuf>("localhost", port)
                .channelPool(pool)
                .config(config)
                .build();
        String content = invokeBlockingCall(client, "test/redirect?port=" + port);
        assertEquals("Hello world", content);
    }
    
    @Test
    public void testNoRedirect() {
        DefaultChannelPool pool = new DefaultChannelPool(2);
        HttpClientConfig.Builder builder = new HttpClientConfig.Builder(null).setFollowRedirect(false);
        HttpClientRequest<ByteBuf> request = HttpClientRequest.createGet("test/redirect?port=" + port);
        HttpClientConfig config = builder.readTimeout(20000, TimeUnit.MILLISECONDS)
                .build();
        HttpClient<ByteBuf, ByteBuf> client = new HttpClientBuilder<ByteBuf, ByteBuf>("localhost", port)
                .channelPool(pool)
                .config(config)
                .build();
        HttpClientResponse<ByteBuf> response = client.submit(request).toBlockingObservable().single();
        assertEquals(HttpResponseStatus.MOVED_PERMANENTLY.code(), response.getStatus().code());
    }


    @Test
    public void testRedirectPost() {
        HttpClientConfig.Builder builder = new HttpClientConfig.Builder(null);
        HttpClientConfig config = builder.setFollowRedirect(true).build();
        HttpClientRequest<ByteBuf> request = HttpClientRequest.createPost("test/redirectPost?port=" + port)
                .withContent("Hello world");
        HttpClient<ByteBuf, ByteBuf> client = new HttpClientBuilder<ByteBuf, ByteBuf>("localhost", port)
                .config(config)
                .build();
        String content = invokeBlockingCall(client, request);
        assertEquals("Hello world", content);
    }
    
    @Test
    public void testNoRedirectPost() {
        HttpClientConfig.Builder builder = new HttpClientConfig.Builder(null);
        HttpClientConfig config = builder.build();
        HttpClientRequest<ByteBuf> request = HttpClientRequest.createPost("test/redirectPost?port=" + port)
                .withContent("Hello world");
        HttpClient<ByteBuf, ByteBuf> client = new HttpClientBuilder<ByteBuf, ByteBuf>("localhost", port)
                .config(config)
                .build();
        HttpClientResponse<ByteBuf> response = client.submit(request).toBlockingObservable().single();
        assertEquals(HttpResponseStatus.MOVED_PERMANENTLY.code(), response.getStatus().code());
    }


    private static void readResponseContent(Observable<HttpClientResponse<ServerSentEvent>> response,
                                            final List<String> result) {
        response.flatMap(
                new Func1<HttpClientResponse<ServerSentEvent>, Observable<ServerSentEvent>>() {
                    @Override
                    public Observable<ServerSentEvent> call(HttpClientResponse<ServerSentEvent> sseEventHttpResponse) {
                        return sseEventHttpResponse.getContent();
                    }
                })
                .toBlockingObservable().forEach(new Action1<ServerSentEvent>() {
            @Override
            public void call(ServerSentEvent serverSentEvent) {
                result.add(serverSentEvent.getEventData());
            }
        });
    }

}
