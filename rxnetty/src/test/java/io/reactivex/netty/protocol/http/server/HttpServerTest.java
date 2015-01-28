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

package io.reactivex.netty.protocol.http.server;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static io.reactivex.netty.protocol.http.AbstractHttpConfigurator.*;

/**
 * @author Nitesh Kant
 */
public class HttpServerTest {

    private static HttpServer<ByteBuf, ByteBuf> mockServer;

    @BeforeClass
    public static void setUp() throws Exception {
        mockServer = RxNetty.newHttpServerBuilder(0, new RequestHandler<ByteBuf, ByteBuf>() {
            @Override
            public Observable<Void> handle(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response) {
                return response.writeStringAndFlush("Welcome!");
            }
        }).enableWireLogging(LogLevel.DEBUG).build().start();
    }

    @AfterClass
    public static void tearDown() throws InterruptedException {
        if (null != mockServer) {
            mockServer.shutdown();
            mockServer.waitTillShutdown(1, TimeUnit.MINUTES);
        }
    }

    @Test
    public void testNoContentWrite() throws Exception {
        HttpServer<ByteBuf, ByteBuf> server = RxNetty.newHttpServerBuilder(0, new RequestHandler<ByteBuf, ByteBuf>() {
            @Override
            public Observable<Void> handle(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response) {
                response.setStatus(HttpResponseStatus.NOT_FOUND);
                return Observable.empty();
            }
        }).enableWireLogging(LogLevel.ERROR).build().start();
        final CountDownLatch finishLatch = new CountDownLatch(1);
        HttpClientResponse<ByteBuf> response = RxNetty.createHttpClient("localhost", server.getServerPort())
                                                      .submit(HttpClientRequest.createGet("/"))
                                                      .finallyDo(new Action0() {
                                                          @Override
                                                          public void call() {
                                                              finishLatch.countDown();
                                                          }
                                                      }).toBlocking().toFuture().get(10, TimeUnit.SECONDS);
        Assert.assertTrue("The returned observable did not finish.", finishLatch.await(1, TimeUnit.MINUTES));
        Assert.assertEquals("Request failed.", response.getStatus(), HttpResponseStatus.NOT_FOUND);
    }

    @Test
    public void testInvalidRequest() throws Exception {
        HttpServer<ByteBuf, ByteBuf> server = RxNetty.newHttpServerBuilder(0, new RequestHandler<ByteBuf, ByteBuf>() {
            @Override
            public Observable<Void> handle(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response) {
                return Observable.empty();
            }
        }).pipelineConfigurator(new HttpServerPipelineConfigurator<ByteBuf, ByteBuf>(10, MAX_CHUNK_SIZE_DEFAULT,
                                                                                     MAX_HEADER_SIZE_DEFAULT))
                                                     .enableWireLogging(LogLevel.ERROR).build().start();
        final CountDownLatch finishLatch = new CountDownLatch(1);
        HttpClientRequest<ByteBuf> req = HttpClientRequest.createGet("/abckdeeeeeeeeeeeeee"); // URI larger than 10 bytes.
        HttpClientResponse<ByteBuf> response = RxNetty.<ByteBuf, ByteBuf>newHttpClientBuilder("localhost", server.getServerPort())
                                                      //.pipelineConfigurator(new HttpClientPipelineConfigurator<ByteBuf, ByteBuf>())
                                                      .enableWireLogging(LogLevel.ERROR)
                                                      .build()
                                                      .submit(req)
                                                      .doOnNext(new Action1<HttpClientResponse<ByteBuf>>() {
                                                          @Override
                                                          public void call(HttpClientResponse<ByteBuf> response) {
                                                              System.out.println("response = " + response);
                                                          }
                                                      })
                                                      .finallyDo(new Action0() {
                                                          @Override
                                                          public void call() {
                                                              finishLatch.countDown();
                                                          }
                                                      }).toBlocking().toFuture().get(10, TimeUnit.SECONDS);
        Assert.assertTrue("The returned observable did not finish.", finishLatch.await(1, TimeUnit.MINUTES));
        Assert.assertEquals("Request failed.", HttpResponseStatus.REQUEST_HEADER_FIELDS_TOO_LARGE, response.getStatus());
    }

    @Test
    public void testProcessingInADifferentThread() throws Exception {
        HttpServer<ByteBuf, ByteBuf> server = RxNetty.newHttpServerBuilder(0, new RequestHandler<ByteBuf, ByteBuf>() {
            @Override
            public Observable<Void> handle(HttpServerRequest<ByteBuf> request, final HttpServerResponse<ByteBuf> serverResponse) {
                return Observable.just(1L).subscribeOn(Schedulers.computation())
                                 .flatMap(new Func1<Long, Observable<Void>>() {
                                     @Override
                                     public Observable<Void> call(Long aLong) {
                                         serverResponse.setStatus(HttpResponseStatus.NOT_FOUND);
                                         return serverResponse.writeStringAndFlush("Invalid URI.");
                                     }
                                 });
            }
        }).enableWireLogging(LogLevel.ERROR).build().start();
        final CountDownLatch finishLatch = new CountDownLatch(1);
        HttpClientResponse<ByteBuf> response = RxNetty.createHttpClient("localhost", server.getServerPort())
                                                      .submit(HttpClientRequest.createGet("/"))
                                                      .finallyDo(new Action0() {
                                                          @Override
                                                          public void call() {
                                                              finishLatch.countDown();
                                                          }
                                                      }).toBlocking().toFuture().get(10, TimeUnit.SECONDS);
        Assert.assertTrue("The returned observable did not finish.", finishLatch.await(1, TimeUnit.MINUTES));
        Assert.assertEquals("Request failed.", response.getStatus(), HttpResponseStatus.NOT_FOUND);
    }

    @Test
    public void testProcessingInADifferentThreadWithNoWrite() throws Exception {
        HttpServer<ByteBuf, ByteBuf> server = RxNetty.newHttpServerBuilder(0, new RequestHandler<ByteBuf, ByteBuf>() {
            @Override
            public Observable<Void> handle(HttpServerRequest<ByteBuf> request, final HttpServerResponse<ByteBuf> serverResponse) {
                return Observable.just(1L).subscribeOn(Schedulers.computation())
                                 .flatMap(new Func1<Long, Observable<Void>>() {
                                     @Override
                                     public Observable<Void> call(Long aLong) {
                                         serverResponse.setStatus(HttpResponseStatus.NOT_FOUND);
                                         return serverResponse.close(false);
                                     }
                                 });
            }
        }).enableWireLogging(LogLevel.ERROR).build().start();
        final CountDownLatch finishLatch = new CountDownLatch(1);
        HttpClientResponse<ByteBuf> response = RxNetty.createHttpClient("localhost", server.getServerPort())
                                                      .submit(HttpClientRequest.createGet("/"))
                                                      .finallyDo(new Action0() {
                                                          @Override
                                                          public void call() {
                                                              finishLatch.countDown();
                                                          }
                                                      }).toBlocking().toFuture().get(10, TimeUnit.SECONDS);
        Assert.assertTrue("The returned observable did not finish.", finishLatch.await(1, TimeUnit.MINUTES));
        Assert.assertEquals("Request failed.", response.getStatus(), HttpResponseStatus.NOT_FOUND);
    }

    @Test
    public void testProxy() throws Exception {
        HttpServer<ByteBuf, ByteBuf> server = RxNetty.newHttpServerBuilder(0, new RequestHandler<ByteBuf, ByteBuf>() {
            @Override
            public Observable<Void> handle(HttpServerRequest<ByteBuf> request, final HttpServerResponse<ByteBuf> serverResponse) {
                return RxNetty.<ByteBuf, ByteBuf>newHttpClientBuilder("localhost", mockServer.getServerPort())
                              .enableWireLogging(LogLevel.DEBUG).build()
                              .submit(HttpClientRequest.createGet("/hello"))
                              .flatMap(new Func1<HttpClientResponse<ByteBuf>, Observable<Void>>() {
                                  @Override
                                  public Observable<Void> call(HttpClientResponse<ByteBuf> response) {
                                      serverResponse.setStatus(response.getStatus());
                                      return serverResponse.close();
                                  }
                              });
            }
        }).enableWireLogging(LogLevel.ERROR).build().start();
        final CountDownLatch finishLatch = new CountDownLatch(1);
        HttpClientResponse<ByteBuf> response = RxNetty.<ByteBuf, ByteBuf>newHttpClientBuilder("localhost", server.getServerPort())
                                                      .enableWireLogging(LogLevel.DEBUG).build()
                                                      .submit(HttpClientRequest.createGet("/"))
                                                      .finallyDo(new Action0() {
                                                          @Override
                                                          public void call() {
                                                              finishLatch.countDown();
                                                          }
                                                      }).toBlocking().toFuture().get(10, TimeUnit.SECONDS);
        Assert.assertTrue("The returned observable did not finish.", finishLatch.await(10, TimeUnit.SECONDS));
        Assert.assertEquals("Request failed.", response.getStatus(), HttpResponseStatus.OK);
    }

    @Test
    public void testFlushOnlyOnReadComplete() throws Exception {
        HttpServer<ByteBuf, ByteBuf> server = RxNetty.newHttpServerBuilder(0, new RequestHandler<ByteBuf, ByteBuf>() {
            @Override
            public Observable<Void> handle(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response) {
                response.flushOnlyOnChannelReadComplete(true);
                response.setStatus(HttpResponseStatus.NOT_FOUND);
                return Observable.empty();
            }
        }).enableWireLogging(LogLevel.ERROR).build().start();
        final CountDownLatch finishLatch = new CountDownLatch(1);
        HttpClientResponse<ByteBuf> response = RxNetty.createHttpClient("localhost", server.getServerPort())
                                                      .submit(HttpClientRequest.createGet("/"))
                                                      .finallyDo(new Action0() {
                                                          @Override
                                                          public void call() {
                                                              finishLatch.countDown();
                                                          }
                                                      }).toBlocking().toFuture().get(10, TimeUnit.SECONDS);
        Assert.assertTrue("The returned observable did not finish.", finishLatch.await(1, TimeUnit.MINUTES));
        Assert.assertEquals("Request failed.", response.getStatus(), HttpResponseStatus.NOT_FOUND);
    }
}
