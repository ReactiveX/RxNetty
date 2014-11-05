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
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.HttpServerBuilder;
import io.reactivex.netty.server.RxServerThreadFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import rx.Observable;
import rx.functions.Action0;
import rx.functions.Func1;

import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Nitesh Kant
 */
public class HttpRedirectTest {

    private static HttpServer<ByteBuf, ByteBuf> server;

    private static int port;

    @BeforeClass
    public static void init() {
        server = new HttpServerBuilder<ByteBuf, ByteBuf>(port, new RequestProcessor())
                .eventLoop(new NioEventLoopGroup(10, new RxServerThreadFactory()))
                .enableWireLogging(LogLevel.DEBUG).build().start();
        port = server.getServerPort(); // Using ephemeral ports
        System.out.println("Mock server using ephemeral port; " + port);
    }

    @AfterClass
    public static void shutDown() throws InterruptedException {
        server.shutdown();
    }

    @Test(expected = HttpRedirectException.class)
    public void testTooManyRedirect() throws Throwable {
        HttpClient.HttpClientConfig.Builder builder = new HttpClient.HttpClientConfig.Builder(null);
        HttpClient.HttpClientConfig config = builder.readTimeout(20000, TimeUnit.MILLISECONDS)
                                                    .followRedirect(2).build();
        HttpClient<ByteBuf, ByteBuf> client = new HttpClientBuilder<ByteBuf, ByteBuf>("localhost", port)
                .config(config).enableWireLogging(LogLevel.ERROR)
                .build();
        String content = invokeBlockingCall(client, HttpClientRequest.createGet("test/redirectLimited?redirectsRequested=6"));
        assertEquals("Hello world", content);
    }

    @Test(expected = HttpRedirectException.class)
    public void testRedirectLoop() throws Throwable {
        HttpClient.HttpClientConfig.Builder builder = new HttpClient.HttpClientConfig.Builder(null);
        HttpClient.HttpClientConfig config = builder.readTimeout(20000, TimeUnit.MILLISECONDS)
                                                    .followRedirect(2).build();
        HttpClient<ByteBuf, ByteBuf> client = new HttpClientBuilder<ByteBuf, ByteBuf>("localhost", port)
                .config(config).enableWireLogging(LogLevel.ERROR)
                .build();
        String content = invokeBlockingCall(client, HttpClientRequest.createGet("test/redirectLoop?redirectsRequested=6"));
        assertEquals("Hello world", content);
    }

    @Test
    public void testAbsoluteRedirect() throws Throwable {
        HttpClient.HttpClientConfig.Builder builder = new HttpClient.HttpClientConfig.Builder(null);
        HttpClient.HttpClientConfig config = builder.readTimeout(20000, TimeUnit.MILLISECONDS)
                                                    .build();
        HttpClient<ByteBuf, ByteBuf> client = new HttpClientBuilder<ByteBuf, ByteBuf>("localhost", port)
                .config(config).enableWireLogging(LogLevel.ERROR)
                .build();
        HttpClientRequest<ByteBuf> request = HttpClientRequest.createGet("test/redirectAbsolute");
        HttpClientResponse<ByteBuf> response = client.submit(request)
                                                      .map(new Func1<HttpClientResponse<ByteBuf>, HttpClientResponse<ByteBuf>>() {
                                                          @Override
                                                          public HttpClientResponse<ByteBuf> call(
                                                                  HttpClientResponse<ByteBuf> response) {
                                                              response.ignoreContent();
                                                              return response;
                                                          }
                                                      })
                                                      .doOnCompleted(new Action0() {
                                                          @Override
                                                          public void call() {
                                                              System.out.println("HttpRedirectTest.call");
                                                          }
                                                      })
                                                      .toBlocking().toFuture().get(1, TimeUnit.MINUTES);
        assertEquals("Unexpected response code.", HttpResponseStatus.MOVED_PERMANENTLY, response.getStatus());
        String locationHeader = response.getHeaders().get("Location");
        assertNotNull("Location header not found.", locationHeader);
        assertTrue("Location was not absolute.", locationHeader.startsWith("http"));
    }

    @Test
    public void testRedirectNoConnPool() throws Throwable {
        HttpClient.HttpClientConfig.Builder builder = new HttpClient.HttpClientConfig.Builder(null);
        HttpClient.HttpClientConfig config = builder.readTimeout(20000, TimeUnit.MILLISECONDS)
                                                    .build();
        HttpClient<ByteBuf, ByteBuf> client = new HttpClientBuilder<ByteBuf, ByteBuf>("localhost", port)
                .config(config).enableWireLogging(LogLevel.ERROR)
                .build();
        String content = invokeBlockingCall(client, HttpClientRequest.createGet("test/redirect"));
        assertEquals("Hello world", content);
    }

    @Test
    public void testRedirectWithConnPool() throws Throwable {
        HttpClient.HttpClientConfig.Builder builder = new HttpClient.HttpClientConfig.Builder(null);
        HttpClient.HttpClientConfig config = builder.readTimeout(20000, TimeUnit.MILLISECONDS)
                                                    .build();
        HttpClient<ByteBuf, ByteBuf> client = new HttpClientBuilder<ByteBuf, ByteBuf>("localhost", port)
                .config(config).enableWireLogging(LogLevel.ERROR).withMaxConnections(10).build();
        String content = invokeBlockingCall(client, HttpClientRequest.createGet("test/redirect"));
        assertEquals("Hello world", content);
    }

    @Test
    public void testNoRedirect() {
        HttpClient.HttpClientConfig.Builder builder = new HttpClient.HttpClientConfig.Builder(null).setFollowRedirect(false);
        HttpClientRequest<ByteBuf> request = HttpClientRequest.createGet("test/redirect");
        HttpClient.HttpClientConfig config = builder.readTimeout(20000, TimeUnit.MILLISECONDS)
                                                    .build();
        HttpClient<ByteBuf, ByteBuf> client = new HttpClientBuilder<ByteBuf, ByteBuf>("localhost", port)
                .config(config)
                .build();
        HttpClientResponse<ByteBuf> response = client.submit(request).toBlocking().single();
        assertEquals(HttpResponseStatus.MOVED_PERMANENTLY.code(), response.getStatus().code());
    }


    @Test
    public void testRedirectPost() throws Throwable {
        HttpClient.HttpClientConfig.Builder builder = new HttpClient.HttpClientConfig.Builder(null);
        HttpClient.HttpClientConfig config = builder.setFollowRedirect(true).build();
        HttpClientRequest<ByteBuf> request = HttpClientRequest.createPost("test/redirectPost")
                                                              .withContent("Hello world");
        HttpClient<ByteBuf, ByteBuf> client = new HttpClientBuilder<ByteBuf, ByteBuf>("localhost", port)
                .config(config)
                .build();
        String content = invokeBlockingCall(client, request);
        assertEquals("Hello world", content);
    }

    @Test
    public void testNoRedirectPost() {
        HttpClient.HttpClientConfig.Builder builder = new HttpClient.HttpClientConfig.Builder(null);
        HttpClient.HttpClientConfig config = builder.build();
        HttpClientRequest<ByteBuf> request = HttpClientRequest.createPost("test/redirectPost")
                                                              .withContent("Hello world");
        HttpClient<ByteBuf, ByteBuf> client = new HttpClientBuilder<ByteBuf, ByteBuf>("localhost", port)
                .config(config)
                .build();
        HttpClientResponse<ByteBuf> response = client.submit(request).toBlocking().single();
        assertEquals(HttpResponseStatus.MOVED_PERMANENTLY.code(), response.getStatus().code());
    }

    private static String invokeBlockingCall(HttpClient<ByteBuf, ByteBuf> client, HttpClientRequest<ByteBuf> request)
            throws Throwable {
        Observable<HttpClientResponse<ByteBuf>> response = client.submit(request);
        try {
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
            }).toBlocking().toFuture().get(1, TimeUnit.MINUTES);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof HttpRedirectException) {
                throw e.getCause();
            }
            throw e;
        }
    }
}
