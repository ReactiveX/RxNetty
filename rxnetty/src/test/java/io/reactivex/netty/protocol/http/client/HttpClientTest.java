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

package io.reactivex.netty.protocol.http.client;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelOption;
import io.netty.channel.ConnectTimeoutException;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.timeout.ReadTimeoutException;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.channel.StringTransformer;
import io.reactivex.netty.client.RxClient;
import io.reactivex.netty.client.RxClient.ClientConfig.Builder;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.HttpServerBuilder;
import io.reactivex.netty.protocol.http.sse.ServerSentEvent;
import io.reactivex.netty.server.RxServerThreadFactory;
import org.junit.AfterClass;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class HttpClientTest {
    private static HttpServer<ByteBuf, ByteBuf> server;

    private static int port;

    @BeforeClass
    public static void init() {
        HttpServerBuilder<ByteBuf, ByteBuf> builder
            = new HttpServerBuilder<ByteBuf, ByteBuf>(new ServerBootstrap().group(new NioEventLoopGroup(10, new RxServerThreadFactory())), port, new RequestProcessor());
        server = builder.enableWireLogging(LogLevel.ERROR).build();
        server.start();
        port = server.getServerPort(); // Using ephemeral ports
        System.out.println("Mock server using ephemeral port; " + port);
    }

    @AfterClass
    public static void shutDown() throws InterruptedException {
        server.shutdown();
    }

    @Test
    public void testConnectionClose() throws Exception {
        HttpClientImpl<ByteBuf, ByteBuf> client = (HttpClientImpl<ByteBuf, ByteBuf>) RxNetty.createHttpClient(
                "localhost", port);
        Observable<ObservableConnection<HttpClientResponse<ByteBuf>,HttpClientRequest<ByteBuf>>> connectionObservable = client.connect().cache();

        final Observable<ByteBuf> content =
                client.submit(HttpClientRequest.createGet("test/singleEntity"), connectionObservable)
                      .flatMap(
                              new Func1<HttpClientResponse<ByteBuf>, Observable<ByteBuf>>() {
                                  @Override
                                  public Observable<ByteBuf> call(HttpClientResponse<ByteBuf> response) {
                                      return response.getContent();
                                  }
                              });
        ObservableConnection<HttpClientResponse<ByteBuf>, HttpClientRequest<ByteBuf>> conn = connectionObservable.toBlocking().last();
        assertFalse("Connection already closed.", conn.isCloseIssued());

        final CountDownLatch responseCompleteLatch = new CountDownLatch(1);
        content.finallyDo(new Action0() {
            @Override
            public void call() {
                responseCompleteLatch.countDown();
            }
        }).subscribe();

        responseCompleteLatch.await(1, TimeUnit.MINUTES);

        assertTrue("Connection not closed after content recieved.", conn.isCloseIssued());
    }

    @Test
    public void testChunkedStreaming() throws Exception {
        HttpClient<ByteBuf, ServerSentEvent> client = RxNetty.createHttpClient("localhost", port,
                                                                        PipelineConfigurators.<ByteBuf>clientSseConfigurator());
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
                                                                                .<ByteBuf>clientSseConfigurator());
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
                                                                                .<ByteBuf>clientSseConfigurator());
        Observable<HttpClientResponse<ServerSentEvent>> response =
                client.submit(HttpClientRequest.createGet("test/largeStream"));
        Observable<String> transformed = response.flatMap(new Func1<HttpClientResponse<ServerSentEvent>, Observable<String>>() {
            @Override
            public Observable<String> call(HttpClientResponse<ServerSentEvent> httpResponse) {
                if (httpResponse.getStatus().equals(HttpResponseStatus.OK)) {
                    return httpResponse.getContent().map(new Func1<ServerSentEvent, String>() {
                        @Override
                        public String call(ServerSentEvent sseEvent) {
                            return sseEvent.contentAsString();
                        }
                    });
                }
                return Observable.error(new RuntimeException("Unexpected response"));
            }
        });

       final List<String> result = new ArrayList<String>();
        transformed.toBlocking().forEach(new Action1<String>() {

            @Override
            public void call(String t1) {
                result.add(t1);
            }
        });
        assertEquals(RequestProcessor.largeStreamContent, result);
    }

    @Test
    public void testSingleEntity() throws Exception {
        HttpClient<ByteBuf, ByteBuf> client = RxNetty.<ByteBuf, ByteBuf>newHttpClientBuilder("localhost", port)
                                                     .enableWireLogging(LogLevel.ERROR).build();
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
        }).toBlocking().forEach(new Action1<String>() {

            @Override
            public void call(String t1) {
                result.add(t1);
            }
        });
        assertEquals("Response not found.", 1, result.size());
        assertEquals("Hello world", result.get(0));
    }

    @Test
    public void testPost() throws Exception {
        HttpClient<ByteBuf, ByteBuf> client = RxNetty.createHttpClient("localhost", port);
        HttpClientRequest<ByteBuf> request = HttpClientRequest.createPost("test/post")
                .withContent("Hello world");
        Observable<HttpClientResponse<ByteBuf>> response = client.submit(request);
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
        }).toBlocking().forEach(new Action1<String>() {

            @Override
            public void call(String t1) {
                result.add(t1);
            }
        });
        assertEquals(1, result.size());
        assertEquals("Hello world", result.get(0));
        
        // resend the same request to make sure it is repeatable
        response = client.submit(request);
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
        }).toBlocking().forEach(new Action1<String>() {

            @Override
            public void call(String t1) {
                result.add(t1);
            }
        });
        assertEquals(1, result.size());
        assertEquals("Hello world", result.get(0));
    }
    
    @Test
    public void testPostWithRawContentSource() {
        PipelineConfigurator<HttpClientResponse<ByteBuf>, HttpClientRequest<String>> pipelineConfigurator
                = PipelineConfigurators.httpClientConfigurator();

        HttpClient<String, ByteBuf> client = RxNetty.createHttpClient("localhost", port, pipelineConfigurator);
        HttpClientRequest<String> request = HttpClientRequest.create(HttpMethod.POST, "test/post");
        request.withRawContentSource(Observable.just("Hello world"),
                                     StringTransformer.DEFAULT_INSTANCE);
        Observable<HttpClientResponse<ByteBuf>> response = client.submit(request);
        String result = response.flatMap(new Func1<HttpClientResponse<ByteBuf>, Observable<String>>() {
            @Override
            public Observable<String> call(HttpClientResponse<ByteBuf> response) {
                return response.getContent().map(new Func1<ByteBuf, String>() {
                    @Override
                    public String call(ByteBuf byteBuf) {
                        return byteBuf.toString(Charset.defaultCharset());
                    }
                });
            }
        }).toBlocking().single();
        assertEquals("Hello world", result);
    }
    
    @Test
    public void testNonChunkingStream() throws Exception {
        HttpClient<ByteBuf, ServerSentEvent> client = RxNetty.createHttpClient("localhost", port,
                                                                        PipelineConfigurators.<ByteBuf>clientSseConfigurator());
        Observable<HttpClientResponse<ServerSentEvent>> response =
                client.submit(HttpClientRequest.createGet("test/nochunk_stream"));
        final List<String> result = new ArrayList<String>();
        response.flatMap(new Func1<HttpClientResponse<ServerSentEvent>, Observable<ServerSentEvent>>() {
            @Override
            public Observable<ServerSentEvent> call(HttpClientResponse<ServerSentEvent> httpResponse) {
                return httpResponse.getContent();
            }
        }).toBlocking().forEach(new Action1<ServerSentEvent>() {
            @Override
            public void call(ServerSentEvent event) {
                result.add(event.contentAsString());
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
        int timeoutMillis = 10;
        RxClient.ClientConfig clientConfig = new Builder(null)
                .readTimeout(timeoutMillis, TimeUnit.MILLISECONDS).build();
        HttpClient<ByteBuf, ByteBuf> client = new HttpClientBuilder<ByteBuf, ByteBuf>("localhost", port)
                .config(clientConfig).build();
        Observable<HttpClientResponse<ByteBuf>> response =
                client.submit(HttpClientRequest.createGet("test/timeout?timeout=" + timeoutMillis * 2 /*Create bigger wait than timeout*/));

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
        if (!latch.await(1, TimeUnit.MINUTES)) {
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

    @Test(expected = TooLongFrameException.class)
    public void testLargeHeaders() throws Exception {
        HttpClient<ByteBuf, ByteBuf> client = RxNetty.<ByteBuf, ByteBuf>newHttpClientBuilder("localhost", port)
                                                     .pipelineConfigurator(
                                                             new HttpClientPipelineConfigurator<ByteBuf, ByteBuf>(1024, 10, 1024))
                                                     .enableWireLogging(LogLevel.ERROR).build();
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
        }).toBlocking().forEach(new Action1<String>() {

            @Override
            public void call(String t1) {
                result.add(t1);
            }
        });
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
                .toBlocking().forEach(new Action1<ServerSentEvent>() {
            @Override
            public void call(ServerSentEvent serverSentEvent) {
                result.add(serverSentEvent.contentAsString());
            }
        });
    }

}
