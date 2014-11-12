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

package io.reactivex.netty.protocol.http.server;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;
import rx.functions.Func1;

import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

/**
 * @author Nitesh Kant
 */
public class Http10Test {

    public static final String WELCOME_SERVER_MSG = "Welcome!";
    private HttpServer<ByteBuf, ByteBuf> mockServer;
    private int mockServerPort;

    @Before
    public void setUp() throws Exception {
        mockServer = RxNetty.createHttpServer(0, new RequestHandler<ByteBuf, ByteBuf>() {
            @Override
            public Observable<Void> handle(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response) {
                return response.writeStringAndFlush(WELCOME_SERVER_MSG);
            }
        });
        mockServer.start();
        mockServerPort = mockServer.getServerPort();
    }

    @After
    public void tearDown() throws Exception {
        if (null != mockServer) {
            mockServer.shutdown();
            mockServer.waitTillShutdown(1, TimeUnit.MINUTES);
        }
    }

    @Test
    public void testHttp1_0Response() throws Exception {
        tearDown(); // ugly but used to shutdown the existing server.
        mockServer = new HttpServerBuilder<ByteBuf, ByteBuf>(0, new RequestHandler<ByteBuf, ByteBuf>() {
            @Override
            public Observable<Void> handle(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response) {
                return response.writeStringAndFlush(WELCOME_SERVER_MSG);
            }
        }, true).build().start();
        HttpClientRequest<ByteBuf> request = HttpClientRequest.create(HttpVersion.HTTP_1_0, HttpMethod.GET, "/");
        HttpClientResponse<ByteBuf> response =
                RxNetty.<ByteBuf, ByteBuf>newHttpClientBuilder("localhost", mockServer.getServerPort())
                       .enableWireLogging(LogLevel.ERROR).build().submit(request).toBlocking()
                       .toFuture().get(1, TimeUnit.MINUTES);
        HttpVersion httpVersion = response.getHttpVersion();
        Assert.assertEquals("Unexpected HTTP version.", HttpVersion.HTTP_1_0, httpVersion);
        Assert.assertFalse("Unexpected Connection header.", response.getHeaders().isKeepAlive());
        Assert.assertFalse("Unexpected Transfer encoding.", response.getHeaders().isTransferEncodingChunked());
    }

    @Test
    public void testHttp1_0Request() throws Exception {
        HttpClientRequest<ByteBuf> request = HttpClientRequest.create(HttpVersion.HTTP_1_0, HttpMethod.GET, "/");
        HttpClientResponse<ByteBuf> response = RxNetty.<ByteBuf, ByteBuf>newHttpClientBuilder("localhost", mockServerPort)
                                                      .enableWireLogging(LogLevel.ERROR).build()
                                                      .submit(request).toBlocking()
                                                      .toFuture().get(1, TimeUnit.MINUTES);
        HttpVersion httpVersion = response.getHttpVersion();
        Assert.assertEquals("Unexpected HTTP version.", HttpVersion.HTTP_1_1, httpVersion);
        Assert.assertFalse("Unexpected Connection header.", response.getHeaders().isKeepAlive());
        Assert.assertFalse("Unexpected Transfer encoding.", response.getHeaders().isTransferEncodingChunked());
    }

    @Test
    public void testHttp1_0RequestWithContent() throws Exception {
        HttpClientRequest<ByteBuf> request = HttpClientRequest.create(HttpVersion.HTTP_1_0, HttpMethod.GET, "/");
        final ByteBuf response = RxNetty.<ByteBuf, ByteBuf>newHttpClientBuilder("localhost", mockServerPort)
                                                      .enableWireLogging(LogLevel.ERROR).build()
                                                      .submit(request)
                                                      .flatMap(
                                                              new Func1<HttpClientResponse<ByteBuf>, Observable<ByteBuf>>() {
                                                                  @Override
                                                                  public Observable<ByteBuf> call(
                                                                          HttpClientResponse<ByteBuf> response) {
                                                                      return response.getContent();
                                                                  }
                                                              })
                                                      .map(new Func1<ByteBuf, ByteBuf>() {
                                                          @Override
                                                          public ByteBuf call(ByteBuf byteBuf) {
                                                              return byteBuf.retain();
                                                          }
                                                      }).toBlocking().toFuture().get(1, TimeUnit.MINUTES);
        Assert.assertEquals("Unexpected Content.", WELCOME_SERVER_MSG, response.toString(Charset.defaultCharset()));
        response.release();
    }
}
