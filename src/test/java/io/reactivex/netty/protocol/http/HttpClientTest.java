/**
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.reactivex.netty.protocol.http;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.timeout.ReadTimeoutException;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.client.HttpClientBuilder;
import io.reactivex.netty.client.RxClient;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.protocol.text.sse.SSEEvent;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import rx.Observable;
import rx.Observer;
import rx.util.functions.Action1;
import rx.util.functions.Func1;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class HttpClientTest {
    private static HttpServer<FullHttpRequest, Object> server;

    private static int port;

    private static class SingleEntityConfigurator implements PipelineConfigurator<String, FullHttpRequest> {

        private final HttpObjectAggregationConfigurator<FullHttpResponse, FullHttpRequest> configurator;

        public SingleEntityConfigurator(HttpObjectAggregationConfigurator<FullHttpResponse, FullHttpRequest> configurator) {
            this.configurator = configurator;
        }

        @Override
        public void configureNewPipeline(ChannelPipeline pipeline) {
            configurator.configureNewPipeline(pipeline);
            pipeline.addLast("entity-decoder", new StringEntityDecoder());
        }
    }
    
    private static class StringEntityDecoder extends MessageToMessageDecoder<FullHttpResponse> {

        @Override
        protected void decode(ChannelHandlerContext ctx, FullHttpResponse msg, List<Object> out) throws Exception {
            ByteBuf buf = msg.content();
            String content = buf.toString(Charset.defaultCharset());
            out.add(content);
            out.add(new DefaultLastHttpContent()); // Since, this is publishing custom objects in the pipeline,
                                                   // it should indicate that the response has ended.
        }
    }

    @BeforeClass
    public static void init() {
        port = new Random().nextInt(1000) + 4000;
        server = RxNetty.createHttpServer(port, new HttpObjectAggregationConfigurator<FullHttpRequest, Object>(new HttpServerPipelineConfigurator<HttpObject, Object>()));
        server.start(new RequestProcessor());
    }

    @AfterClass
    public static void shutDown() throws InterruptedException {
        server.shutdown();
    }
    
    
    @Test
    public void testChunkedStreaming() throws Exception {
        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "test/stream");
        HttpClient<FullHttpRequest, SSEEvent> client = RxNetty.createSseClient("localhost", port);
        Observable<ObservableHttpResponse<SSEEvent>> response = client.submit(request);
        
        final List<String> result = new ArrayList<String>();

        response.flatMap(
                new Func1<ObservableHttpResponse<SSEEvent>, Observable<SSEEvent>>() {
                    @Override
                    public Observable<SSEEvent> call(ObservableHttpResponse<SSEEvent> observableHttpResponse) {
                        return observableHttpResponse.content();
                    }
                })
                .toBlockingObservable()
                .forEach(new Action1<SSEEvent>() {
                    @Override
                    public void call(SSEEvent sseEvent) {
                        result.add(sseEvent.getEventData());
                    }
                });
        assertEquals(RequestProcessor.smallStreamContent, result);
    }
    
    @Test
    public void testMultipleChunks() throws Exception {
        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "test/largeStream");
        HttpClient<FullHttpRequest, SSEEvent> client = RxNetty.createSseClient("localhost", port);
        Observable<ObservableHttpResponse<SSEEvent>> response = client.submit(request);
        
        final List<String> result = new ArrayList<String>();

        response.flatMap(new Func1<ObservableHttpResponse<SSEEvent>, Observable<SSEEvent>>() {
            @Override
            public Observable<SSEEvent> call(ObservableHttpResponse<SSEEvent> observableHttpResponse) {
                return observableHttpResponse.content();
            }
        }).toBlockingObservable().forEach(new Action1<SSEEvent>() {
            @Override
            public void call(SSEEvent event
                    ) {
                result.add(event.getEventData());
            }
        });
        assertEquals(RequestProcessor.largeStreamContent, result);
        
    }

    @Test
    public void testSingleEntity() throws Exception {
        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "test/singleEntity");
        final List<String> result = new ArrayList<String>();

        HttpObjectAggregationConfigurator<FullHttpResponse, FullHttpRequest> aggregationConfigurator =
                new HttpObjectAggregationConfigurator<FullHttpResponse, FullHttpRequest>(new HttpClientPipelineConfigurator<FullHttpRequest, FullHttpResponse>());
        HttpClient<FullHttpRequest, String> client =
                RxNetty.createHttpClient("localhost", port,
                                         new SingleEntityConfigurator(aggregationConfigurator));
        Observable<ObservableHttpResponse<String>> response = client.submit(request);
        response.flatMap(new Func1<ObservableHttpResponse<String>, Observable<String>>() {

            @Override
            public Observable<String> call(ObservableHttpResponse<String> t1) {
                return t1.content();
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
    public void testFullHttpResponse() throws Exception {
        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "test/singleEntity");
        final List<String> result = new ArrayList<String>();
        HttpClient<FullHttpRequest, FullHttpResponse> client = RxNetty.createHttpClient("localhost", port);
        Observable<ObservableHttpResponse<FullHttpResponse>> response = client.submit(request);
        
        response.flatMap(new Func1<ObservableHttpResponse<FullHttpResponse>, Observable<FullHttpResponse>>() {
            @Override
            public Observable<FullHttpResponse> call(
                    ObservableHttpResponse<FullHttpResponse> t1) {
                return t1.content();
            }
        }).toBlockingObservable().forEach(new Action1<FullHttpResponse>() {

            @Override
            public void call(FullHttpResponse t1) {
                result.add(t1.content().toString(Charset.defaultCharset()));
            }
            
        });
        assertEquals(1, result.size());
        assertEquals("Hello world", result.get(0));
    }
    
    @Test
    public void testNonChunkingStream() throws Exception {
        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "test/nochunk_stream");
        HttpClient<FullHttpRequest, SSEEvent> client = RxNetty.createSseClient("localhost", port);
        final List<String> result = new ArrayList<String>();
        Observable<ObservableHttpResponse<SSEEvent>> response = client.submit(request);

        response.flatMap(new Func1<ObservableHttpResponse<SSEEvent>, Observable<SSEEvent>>() {
            @Override
            public Observable<SSEEvent> call(ObservableHttpResponse<SSEEvent> observableHttpResponse) {
                return observableHttpResponse.content();
            }
        }).toBlockingObservable().forEach(new Action1<SSEEvent>() {
            @Override
            public void call(SSEEvent event
            ) {
                result.add(event.getEventData());
            }
        });
        assertEquals(RequestProcessor.smallStreamContent, result);
    }
    
    @Test
    public void testConnectException() throws Exception {
        HttpClientBuilder<FullHttpRequest, FullHttpResponse> clientBuilder =
                new HttpClientBuilder<FullHttpRequest, FullHttpResponse>("www.google.com", 81);
        HttpClient<FullHttpRequest, FullHttpResponse> client = clientBuilder.channelOption(
                ChannelOption.CONNECT_TIMEOUT_MILLIS, 10).build();
        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
        Observable<ObservableHttpResponse<FullHttpResponse>> response = client.submit(request);
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Throwable> ex = new AtomicReference<Throwable>();
        response.subscribe(new Action1<ObservableHttpResponse<FullHttpResponse>>() {
            @Override
            public void call(ObservableHttpResponse<FullHttpResponse> t1) {
                latch.countDown();
            }
            
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable t1) {
                ex.set(t1);
                latch.countDown();
            }
        });
        latch.await(2, TimeUnit.SECONDS);
        assertTrue(ex.get() instanceof ConnectTimeoutException);
    }
    
    @Test
    public void testConnectException2() throws Exception {
        HttpClientBuilder<FullHttpRequest, FullHttpResponse> clientBuilder =
                new HttpClientBuilder<FullHttpRequest, FullHttpResponse>("www.google.com", 81);
        HttpClient<FullHttpRequest, FullHttpResponse> client = clientBuilder.channelOption(
                ChannelOption.CONNECT_TIMEOUT_MILLIS, 10).build();
        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
        Observable<ObservableHttpResponse<FullHttpResponse>> response = client.submit(request);
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Throwable> ex = new AtomicReference<Throwable>();
        response.flatMap(new Func1<ObservableHttpResponse<FullHttpResponse>, Observable<FullHttpResponse>>() {
            @Override
            public Observable<FullHttpResponse> call(
                    ObservableHttpResponse<FullHttpResponse> t1) {
                return t1.content();
            }
        }).subscribe(new Action1<FullHttpResponse>() {
            @Override
            public void call(FullHttpResponse t1) {
                latch.countDown();
            }
            
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable t1) {
                ex.set(t1);
                latch.countDown();
            }
        });
        latch.await(100000, TimeUnit.SECONDS);
        assertTrue(ex.get() instanceof ConnectTimeoutException);
    }

    
    @Test
    public void testTimeout() throws Exception {
        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "test/timeout?timeout=10000");
        PipelineConfigurator<FullHttpResponse, FullHttpRequest> pipelineConfigurator =
                PipelineConfigurators.fullHttpMessageClientConfigurator();
        RxClient.ClientConfig clientConfig = new RxClient.ClientConfig.Builder(RxClient.ClientConfig.DEFAULT_CONFIG)
                .readTimeout(10, TimeUnit.MILLISECONDS).build();
        HttpClient<FullHttpRequest, FullHttpResponse> client =
                new HttpClientBuilder<FullHttpRequest, FullHttpResponse>("localhost", port)
                .pipelineConfigurator(pipelineConfigurator).config(clientConfig).build();
        Observable<ObservableHttpResponse<FullHttpResponse>> response = client.submit(request);
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Throwable> exception = new AtomicReference<Throwable>();
        response.flatMap(new Func1<ObservableHttpResponse<FullHttpResponse>, Observable<FullHttpResponse>>() {
            @Override
            public Observable<FullHttpResponse> call(
                    ObservableHttpResponse<FullHttpResponse> t1) {
                return t1.content();
            }
        }).subscribe(new Observer<FullHttpResponse>() {
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
            public void onNext(FullHttpResponse args) {
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
        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "test/singleEntity");
        PipelineConfigurator<FullHttpResponse, FullHttpRequest> pipelineConfigurator =
                PipelineConfigurators.fullHttpMessageClientConfigurator();
        RxClient.ClientConfig clientConfig = new RxClient.ClientConfig.Builder(RxClient.ClientConfig.DEFAULT_CONFIG)
                .readTimeout(2, TimeUnit.SECONDS).build();
        HttpClient<FullHttpRequest, FullHttpResponse> client =
                new HttpClientBuilder<FullHttpRequest, FullHttpResponse>("localhost", port)
                        .pipelineConfigurator(pipelineConfigurator).config(clientConfig).build();

        Observable<ObservableHttpResponse<FullHttpResponse>> response = client.submit(request);
        
        final AtomicReference<Throwable> exceptionHolder = new AtomicReference<Throwable>();
        final AtomicReference<FullHttpResponse> responseHolder = new AtomicReference<FullHttpResponse>(); 
        response.flatMap(new Func1<ObservableHttpResponse<FullHttpResponse>, Observable<FullHttpResponse>>() {
            @Override
            public Observable<FullHttpResponse> call(
                    ObservableHttpResponse<FullHttpResponse> t1) {
                return t1.content();
            }
        }).subscribe(new Observer<FullHttpResponse>() {
            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
                exceptionHolder.set(e);
            }

            @Override
            public void onNext(FullHttpResponse args) {
                responseHolder.set(args);
            }
        });
        Thread.sleep(3000);
        assertNull(exceptionHolder.get());
        assertEquals(200, responseHolder.get().getStatus().code());
    }

}
