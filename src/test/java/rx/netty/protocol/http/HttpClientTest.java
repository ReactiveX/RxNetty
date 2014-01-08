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
package rx.netty.protocol.http;

import static org.junit.Assert.*;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpObjectAggregator;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import rx.Observable;
import rx.netty.protocol.http.ObservableHttpClient.HttpClientBuilder;
import rx.util.functions.Action1;
import rx.util.functions.Func1;

import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;
import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.net.httpserver.HttpServer;

public class HttpClientTest {
    private static HttpServer server = null;
    private static String SERVICE_URI;

    private static int port;
    private static ObservableHttpClient client;

    private static class SingleEntityHandler extends HttpProtocolHandlerAdapter<String> {

        @Override
        public void configure(ChannelPipeline pipeline) {
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
        PackagesResourceConfig resourceConfig = new PackagesResourceConfig("rx.netty.protocol.http");
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
        client = new HttpClientBuilder().build(group);
    }

    @AfterClass
    public static void shutDown() {
        server.stop(0);
    }
    
    
    @Test
    public void testChunkedStreaming() throws Exception {
        ValidatedFullHttpRequest request = ValidatedFullHttpRequest.get(SERVICE_URI + "test/stream");

        Observable<ObservableHttpResponse<Message>> response = client.execute(request, HttpProtocolHandlerAdapter.SSE_HANDLER);
        
        final List<String> result = new ArrayList<String>();

        response.flatMap(new Func1<ObservableHttpResponse<Message>, Observable<Message>>() {
            @Override
            public Observable<Message> call(ObservableHttpResponse<Message> observableHttpResponse) {
                return observableHttpResponse.content();
            }
        }).toBlockingObservable().forEach(new Action1<Message>() {
            @Override
            public void call(Message message
                    ) {
                // System.out.println(message);
                result.add(message.getEventData());
            }
        });
        assertEquals(EmbeddedResources.smallStreamContent, result);
    }
    
    @Test
    public void testMultipleChunks() throws Exception {
        ValidatedFullHttpRequest request = ValidatedFullHttpRequest.get(SERVICE_URI + "test/largeStream");

        Observable<ObservableHttpResponse<Message>> response = client.execute(request, HttpProtocolHandlerAdapter.SSE_HANDLER);
        
        final List<String> result = new ArrayList<String>();

        response.flatMap(new Func1<ObservableHttpResponse<Message>, Observable<Message>>() {
            @Override
            public Observable<Message> call(ObservableHttpResponse<Message> observableHttpResponse) {
                return observableHttpResponse.content();
            }
        }).toBlockingObservable().forEach(new Action1<Message>() {
            @Override
            public void call(Message message
                    ) {
                // System.out.println(message);
                result.add(message.getEventData());
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
            content += "data:" + s + "\n";
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
        Observable<ObservableHttpResponse<Message>> response = client.execute(request, HttpProtocolHandlerAdapter.SSE_HANDLER);
        
        final List<String> result = new ArrayList<String>();

        response.flatMap(new Func1<ObservableHttpResponse<Message>, Observable<Message>>() {
            @Override
            public Observable<Message> call(ObservableHttpResponse<Message> observableHttpResponse) {
                return observableHttpResponse.content();
            }
        }).subscribe(new Action1<Message>() {
            @Override
            public void call(Message message
                    ) {
                result.add(message.getEventData());
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
}
