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

public class HttpClientTest {
/*
    private static HttpServer server = null;
    private static String SERVICE_URI;

    private static int port;
    private static HttpClient client;

    private static class SingleEntityHandler extends HttpNettyPipelineConfiguratorAdapter<String> {
        @Override
        public void configureNewPipeline(ChannelPipeline pipeline) {
            pipeline.addAfter("http-response-decoder", "http-aggregator", new HttpObjectAggregator(Integer.MAX_VALUE));
            pipeline.addAfter("http-aggregator", "entity-decoder", new StringEntityDecoder());
        }
    }
    
    private static class StringEntityDecoder extends MessageToMessageDecoder<FullHttpResponse> {

        @Override
        protected void decode(ChannelHandlerContext ctx, FullHttpResponse msg,
                List<Object> out) throws Exception {
            ByteBuf buf = msg.content();
            String content = buf.toString(Charset.defaultCharset());
            out.add(content);
        }
        
    }

    @BeforeClass
    public static void init() {
        PackagesResourceConfig resourceConfig = new PackagesResourceConfig("io.reactivex.netty.protocol.http");
        port = (new Random()).nextInt(1000) + 4000;
        SERVICE_URI = "http://localhost:" + port + "/";
        try{
            server = HttpServerFactory.create(SERVICE_URI, resourceConfig);
            server.start();
        } catch(Exception e) {
            e.printStackTrace();
            fail("Unable to start server");
        }
        EventLoopGroup group = new NioEventLoopGroup();
        client = new HttpClient(new NettyClient.ServerInfo("localhost", port),
                                new Bootstrap(), new HttpClientPipelineConfigurator());
    }

    @AfterClass
    public static void shutDown() {
        server.stop(0);
    }
    
    
    @Test
    public void testChunkedStreaming() throws Exception {
        ValidatedFullHttpRequest request = ValidatedFullHttpRequest.get(SERVICE_URI + "test/stream");

        Observable<ObservableHttpResponse<SSEEvent>> response = client.execute(request, HttpNettyPipelineConfiguratorAdapter.SSE_HANDLER);
        
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
                // System.out.println(message);
                result.add(event.getEventData());
            }
        });
        assertEquals(EmbeddedResources.smallStreamContent, result);
    }
    
    @Test
    public void testMultipleChunks() throws Exception {
        ValidatedFullHttpRequest request = ValidatedFullHttpRequest.get(SERVICE_URI + "test/largeStream");

        Observable<ObservableHttpResponse<SSEEvent>> response = client.execute(request, HttpNettyPipelineConfiguratorAdapter.SSE_HANDLER);
        
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
                // System.out.println(message);
                result.add(event.getEventData());
            }
        });
        // Thread.sleep(5000);
        assertEquals(EmbeddedResources.largeStreamContent, result);
        
    }

    @Test
    public void testSingleEntity() throws Exception {
        ValidatedFullHttpRequest request = ValidatedFullHttpRequest.get(SERVICE_URI + "test/singleEntity");
        final List<String> result = new ArrayList<String>();

        Observable<ObservableHttpResponse<String>> response = client.execute(request, new SingleEntityHandler());
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
        ValidatedFullHttpRequest request = ValidatedFullHttpRequest.get(SERVICE_URI + "test/singleEntity");
        final List<String> result = new ArrayList<String>();

        Observable<ObservableHttpResponse<FullHttpResponse>> response = client.request(request);
        
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
        MockWebServer server = new MockWebServer();
        String content = "";
        for (String s: EmbeddedResources.largeStreamContent) {
            content += "data:" + s + "\n\n";
        }
        server.enqueue(new MockResponse().setResponseCode(200).setHeader("Content-type", "text/event-stream")
                .setBody(content)
                .removeHeader("Content-Length"));
        server.play();
        
        // TODO: this does not work for UriInfo: https://github.com/Netflix/RxNetty/issues/12
        // URI url = server.getUrl("/").toURI();

        URI url = new URI("http://localhost:" + server.getPort() + "/"); 
        
        System.err.println("Using URI: " + url);
        ValidatedFullHttpRequest request = ValidatedFullHttpRequest.get(url);
        Observable<ObservableHttpResponse<SSEEvent>> response = client.execute(request, HttpNettyPipelineConfiguratorAdapter.SSE_HANDLER);
        
        final List<String> result = new ArrayList<String>();

        response.flatMap(new Func1<ObservableHttpResponse<SSEEvent>, Observable<SSEEvent>>() {
            @Override
            public Observable<SSEEvent> call(ObservableHttpResponse<SSEEvent> observableHttpResponse) {
                return observableHttpResponse.content();
            }
        }).subscribe(new Action1<SSEEvent>() {
            @Override
            public void call(SSEEvent event
                    ) {
                result.add(event.getEventData());
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable t1) {
                t1.printStackTrace();
            }
        });
        Thread.sleep(2000);
        assertEquals(EmbeddedResources.largeStreamContent, result);
        server.shutdown();
    }
    
    @Test
    public void testConnectException() throws Exception {
        ValidatedFullHttpRequest request = ValidatedFullHttpRequest.get("http://www.google.com:81/");
        HttpClient timeoutClient = HttpClient.newBuilder()
        .withChannelOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10)
        .build(new NioEventLoopGroup());
        Observable<ObservableHttpResponse<FullHttpResponse>> response = timeoutClient.request(request);
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
        assertTrue(ex.get() instanceof io.netty.channel.ConnectTimeoutException);
    }
    
    @Test
    public void testConnectException2() throws Exception {
        ValidatedFullHttpRequest request = ValidatedFullHttpRequest.get("http://www.google.com:81/");
        HttpClient timeoutClient = HttpClient.newBuilder()
        .withChannelOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10)
        .build(new NioEventLoopGroup());
        Observable<ObservableHttpResponse<FullHttpResponse>> response = timeoutClient.request(request);
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
        assertTrue(ex.get() instanceof io.netty.channel.ConnectTimeoutException);
    }

    
    @Test
    public void testTimeout() throws Exception {
        ValidatedFullHttpRequest request = ValidatedFullHttpRequest.get(SERVICE_URI + "test/timeout?timeout=10000");
        Observable<ObservableHttpResponse<FullHttpResponse>> response = client.execute(request, new FullHttpResponseHandler(10));
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
            assertTrue(exception.get() instanceof io.netty.handler.timeout.ReadTimeoutException);
        }
    }
    
    @Test
    public void testNoReadTimeout() throws Exception {
        ValidatedFullHttpRequest request = ValidatedFullHttpRequest.get(SERVICE_URI + "test/singleEntity");
        // Set a read timeout of 2 seconds
        Observable<ObservableHttpResponse<FullHttpResponse>> response = client.execute(request, new FullHttpResponseHandler(2000));
        
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
*/

}
