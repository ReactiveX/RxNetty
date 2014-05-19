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
package io.reactivex.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.reactivex.netty.protocol.http.client.ContentSource;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.http.client.RawContentSource;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import io.reactivex.netty.serialization.ContentTransformer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import rx.Notification;
import rx.Observable;
import rx.functions.Func1;

/**
 * @author Nitesh Kant
 */
public class RxNettyHttpShorthandsTest {

    public static final String METHOD_HEADER = "METHOD";
    public static final String CONTENT_RECEIEVED_HEADER = "CONTENT-RECEIEVED";
    private HttpServer<ByteBuf, ByteBuf> mockServer;

    @Before
    public void setUp() throws Exception {
        mockServer = RxNetty.createHttpServer(0, new RequestHandler<ByteBuf, ByteBuf>() {
            @Override
            public Observable<Void> handle(HttpServerRequest<ByteBuf> request, final HttpServerResponse<ByteBuf> response) {
                response.getHeaders().add(METHOD_HEADER, request.getHttpMethod().name());
                return request.getContent().materialize()
                              .flatMap(new Func1<Notification<ByteBuf>, Observable<Void>>() {
                                  @Override
                                  public Observable<Void> call(Notification<ByteBuf> notification) {
                                      switch (notification.getKind()) {
                                          case OnNext:
                                              response.getHeaders().set(CONTENT_RECEIEVED_HEADER, "true");
                                              return Observable.empty();
                                          case OnError:
                                              return Observable.error(notification.getThrowable());
                                          case OnCompleted:
                                              return Observable.empty();
                                      }
                                      return Observable.error(new IllegalArgumentException("Unknown notification type."));
                                  }
                              });
            }
        }).start();
    }

    @After
    public void tearDown() throws Exception {
        mockServer.shutdown();
        mockServer.waitTillShutdown();
    }

    @Test
    public void testGet() throws Exception {
        HttpClientResponse<ByteBuf> response = RxNetty.createHttpGet("http://localhost:" + mockServer.getServerPort()
                                                                     + '/').toBlockingObservable().last();
        Assert.assertEquals("Unexpected HTTP method sent.", "GET", response.getHeaders().get(METHOD_HEADER));
    }

    @Test
    public void testDelete() throws Exception {
        HttpClientResponse<ByteBuf> response = RxNetty.createHttpDelete("http://localhost:" + mockServer.getServerPort()
                                                                        + '/').toBlockingObservable().last();
        Assert.assertEquals("Unexpected HTTP method sent.", "DELETE", response.getHeaders().get(METHOD_HEADER));
    }

    @Test
    public void testPost() throws Exception {
        ContentSource.SingletonSource<ByteBuf> content = new ContentSource.SingletonSource<ByteBuf>(
                Unpooled.buffer().writeBytes("Hello!".getBytes()));
        HttpClientResponse<ByteBuf> response =
                RxNetty.createHttpPost("http://localhost:" + mockServer.getServerPort() + '/', content)
                       .toBlockingObservable().last();
        Assert.assertEquals("Unexpected HTTP method sent.", "POST", response.getHeaders().get(METHOD_HEADER));
        Assert.assertEquals("Content not sent by the client.", "true", response.getHeaders().get(CONTENT_RECEIEVED_HEADER));
    }

    @Test
    public void testPut() throws Exception {
        ContentSource.SingletonSource<ByteBuf> content = new ContentSource.SingletonSource<ByteBuf>(
                Unpooled.buffer().writeBytes("Hello!".getBytes()));
        HttpClientResponse<ByteBuf> response =
                RxNetty.createHttpPut("http://localhost:" + mockServer.getServerPort() + '/', content)
                       .toBlockingObservable().last();
        Assert.assertEquals("Unexpected HTTP method sent.", "PUT", response.getHeaders().get(METHOD_HEADER));
        Assert.assertEquals("Content not sent by the client.", "true", response.getHeaders().get(CONTENT_RECEIEVED_HEADER));
    }

    @Test
    public void testPostRawContent() throws Exception {
        RawContentSource<String> content = getRawContentSource();
        HttpClientResponse<ByteBuf> response =
                RxNetty.createHttpPost("http://localhost:" + mockServer.getServerPort() + '/', content)
                       .toBlockingObservable().last();
        Assert.assertEquals("Unexpected HTTP method sent.", "POST", response.getHeaders().get(METHOD_HEADER));
        Assert.assertEquals("Content not sent by the client.", "true", response.getHeaders().get(CONTENT_RECEIEVED_HEADER));
    }

    @Test
    public void testPutRawContent() throws Exception {
        RawContentSource<String> content = getRawContentSource();
        HttpClientResponse<ByteBuf> response =
                RxNetty.createHttpPut("http://localhost:" + mockServer.getServerPort() + '/', content)
                       .toBlockingObservable().last();
        Assert.assertEquals("Unexpected HTTP method sent.", "PUT", response.getHeaders().get(METHOD_HEADER));
        Assert.assertEquals("Content not sent by the client.", "true", response.getHeaders().get(CONTENT_RECEIEVED_HEADER));
    }

    private static RawContentSource<String> getRawContentSource() {
        return new RawContentSource.SingletonRawSource<String>("Hello!", new ContentTransformer<String>() {
            @Override
            public ByteBuf transform(String toTransform, ByteBufAllocator byteBufAllocator) {
                return byteBufAllocator.buffer().writeBytes(toTransform.getBytes());
            }
        });
    }
}
