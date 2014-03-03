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
import io.netty.channel.ChannelOption;
import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.timeout.ReadTimeoutException;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.client.RxClient;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.text.sse.ServerSentEvent;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import rx.Observable;
import rx.Observer;
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

import static io.reactivex.netty.client.RxClient.ClientConfig.Builder;
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
        server = RxNetty.createHttpServer(port, new RequestProcessor());
        server.start();
    }

    @AfterClass
    public static void shutDown() throws InterruptedException {
        server.shutdown();
    }
    
    
    @Test
    public void testChunkedStreaming() throws Exception {
        HttpClient<ByteBuf, ServerSentEvent> client = RxNetty.createHttpClient("localhost", port,
                                                                        PipelineConfigurators.<ByteBuf>sseClientConfigurator());
        Observable<HttpResponse<ServerSentEvent>> response =
                client.submit(HttpRequest.createGet("test/stream"));

        final List<String> result = new ArrayList<String>();
        readResponseContent(response, result);
        assertEquals(RequestProcessor.smallStreamContent, result);
    }

    @Test
    public void testMultipleChunks() throws Exception {
        HttpClient<ByteBuf, ServerSentEvent> client = RxNetty.createHttpClient("localhost", port,
                                                                        PipelineConfigurators
                                                                                .<ByteBuf>sseClientConfigurator());
        Observable<HttpResponse<ServerSentEvent>> response =
                client.submit(HttpRequest.createDelete("test/largeStream"));

        final List<String> result = new ArrayList<String>();
        readResponseContent(response, result);
        assertEquals(RequestProcessor.largeStreamContent, result);
    }

    @Test
    public void testMultipleChunksWithTransformation() throws Exception {
        HttpClient<ByteBuf, ServerSentEvent> client = RxNetty.createHttpClient("localhost", port,
                                                                        PipelineConfigurators
                                                                                .<ByteBuf>sseClientConfigurator());
        Observable<HttpResponse<ServerSentEvent>> response =
                client.submit(HttpRequest.createGet("test/largeStream"));
        Observable<String> transformed = response.flatMap(new Func1<HttpResponse<ServerSentEvent>, Observable<String>>() {
            @Override
            public Observable<String> call(HttpResponse<ServerSentEvent> httpResponse) {
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
        Observable<HttpResponse<ByteBuf>> response = client.submit(HttpRequest.createGet("test/singleEntity"));
        final List<String> result = new ArrayList<String>();
        response.flatMap(new Func1<HttpResponse<ByteBuf>, Observable<String>>() {
            @Override
            public Observable<String> call(HttpResponse<ByteBuf> response) {
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
        HttpRequest<ByteBuf> request = HttpRequest.createPost("test/post")
                .withContent("Hello world");
        RepeatableContentHttpRequest<ByteBuf> repeatable = new RepeatableContentHttpRequest<ByteBuf>(request);
        Observable<HttpResponse<ByteBuf>> response = client.submit(repeatable);
        final List<String> result = new ArrayList<String>();
        response.flatMap(new Func1<HttpResponse<ByteBuf>, Observable<String>>() {
            @Override
            public Observable<String> call(HttpResponse<ByteBuf> response) {
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
        response.flatMap(new Func1<HttpResponse<ByteBuf>, Observable<String>>() {
            @Override
            public Observable<String> call(HttpResponse<ByteBuf> response) {
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
        Observable<HttpResponse<ServerSentEvent>> response =
                client.submit(HttpRequest.createGet("test/nochunk_stream"));
        final List<String> result = new ArrayList<String>();
        response.flatMap(new Func1<HttpResponse<ServerSentEvent>, Observable<ServerSentEvent>>() {
            @Override
            public Observable<ServerSentEvent> call(HttpResponse<ServerSentEvent> httpResponse) {
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
        Observable<HttpResponse<ByteBuf>> response =
                client.submit(HttpRequest.createGet("/"));
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Throwable> ex = new AtomicReference<Throwable>();
        response.subscribe(new Observer<HttpResponse<ByteBuf>>() {
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
            public void onNext(HttpResponse<ByteBuf> args) {
            }
        });
        latch.await();
        if (ex.get() != null) {
            ex.get().printStackTrace();
        }
        assertNotNull(ex.get());
        assertTrue(ex.get() instanceof ConnectException);
    }

    @Test
    public void testConnectException2() throws Exception {
        HttpClientBuilder<ByteBuf, ByteBuf> clientBuilder = new HttpClientBuilder<ByteBuf, ByteBuf>("www.google.com", 81);
        HttpClient<ByteBuf, ByteBuf> client = clientBuilder.channelOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10).build();
        Observable<HttpResponse<ByteBuf>> response = client.submit(HttpRequest.createGet("/"));
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Throwable> ex = new AtomicReference<Throwable>();
        response.subscribe(new Observer<HttpResponse<ByteBuf>>() {
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
            public void onNext(HttpResponse<ByteBuf> args) {
            }
        });
        latch.await(10, TimeUnit.SECONDS);
        assertTrue(ex.get() instanceof ConnectTimeoutException);
    }

    @Test
    public void testTimeout() throws Exception {
        RxClient.ClientConfig clientConfig = new Builder(RxClient.ClientConfig.DEFAULT_CONFIG)
                .readTimeout(10, TimeUnit.MILLISECONDS).build();
        HttpClient<ByteBuf, ByteBuf> client = new HttpClientBuilder<ByteBuf, ByteBuf>("localhost", port).config(
                clientConfig).build();
        Observable<HttpResponse<ByteBuf>> response =
                client.submit(HttpRequest.createGet("test/timeout?timeout=10000"));

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Throwable> exception = new AtomicReference<Throwable>();
        response.subscribe(new Observer<HttpResponse<ByteBuf>>() {
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
            public void onNext(HttpResponse<ByteBuf> response) {
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
        RxClient.ClientConfig clientConfig = new Builder(RxClient.ClientConfig.DEFAULT_CONFIG)
                .readTimeout(2, TimeUnit.SECONDS).build();
        HttpClient<ByteBuf, ByteBuf> client = new HttpClientBuilder<ByteBuf, ByteBuf>("localhost", port).config(clientConfig).build();
        Observable<HttpResponse<ByteBuf>> response =
                client.submit(HttpRequest.createGet("test/singleEntity"));

        final AtomicReference<Throwable> exceptionHolder = new AtomicReference<Throwable>();
        final int[] status = {0};
        response.subscribe(new Observer<HttpResponse<ByteBuf>>() {
            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
                exceptionHolder.set(e);
            }

            @Override
            public void onNext(HttpResponse<ByteBuf> response) {
                status[0] = response.getStatus().code();
            }
        });
        Thread.sleep(3000);
        assertNull(exceptionHolder.get());
        assertEquals(200, status[0]);
    }

    private static void readResponseContent(Observable<HttpResponse<ServerSentEvent>> response,
                                            final List<String> result) {
        response.flatMap(
                new Func1<HttpResponse<ServerSentEvent>, Observable<ServerSentEvent>>() {
                    @Override
                    public Observable<ServerSentEvent> call(HttpResponse<ServerSentEvent> sseEventHttpResponse) {
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
