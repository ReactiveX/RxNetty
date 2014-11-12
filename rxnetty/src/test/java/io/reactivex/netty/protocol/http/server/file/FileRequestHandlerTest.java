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
package io.reactivex.netty.protocol.http.server.file;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.http.server.HttpServer;
import org.junit.Assert;
import org.junit.Test;
import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class FileRequestHandlerTest {
    @Test
    public void shouldNotAllowRoot() throws Exception {
        HttpServer<ByteBuf, ByteBuf> server = RxNetty.createHttpServer(0, new WebappFileRequestHandler())
                .start();
        
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
        Assert.assertEquals("Request failed.", HttpResponseStatus.FORBIDDEN, response.getStatus());
    }
    
    @Test
    public void shouldReturnFile() throws Exception {
        HttpServer<ByteBuf, ByteBuf> server = RxNetty.createHttpServer(0, new WebappFileRequestHandler())
                .start();
        
        final CountDownLatch finishLatch = new CountDownLatch(1);
        final AtomicReference<HttpResponseStatus> status = new AtomicReference<HttpResponseStatus>();
        final AtomicReference<String> contentType = new AtomicReference<String>();
        final AtomicReference<Integer> contentLength = new AtomicReference<Integer>();
        ByteBuf response = RxNetty.createHttpClient("localhost", server.getServerPort())
                      .submit(HttpClientRequest.createGet("/sample.json"))
                      .doOnNext(new Action1<HttpClientResponse<ByteBuf>>() {
                          @Override
                          public void call(HttpClientResponse<ByteBuf> response) {
                              status.set(response.getStatus());
                              contentType.set(response.getHeaders().getHeader(HttpHeaders.Names.CONTENT_TYPE));
                              contentLength.set(Integer.parseInt(response.getHeaders().get(HttpHeaders.Names.CONTENT_LENGTH)));
                          }
                      })
                      .flatMap(new Func1<HttpClientResponse<ByteBuf>, Observable<ByteBuf>>() {
                          @Override
                          public Observable<ByteBuf> call(HttpClientResponse<ByteBuf> response) {
                              return response.getContent();
                          }
                      })
                      .finallyDo(new Action0() {
                          @Override
                          public void call() {
                              finishLatch.countDown();
                          }
                      }).toBlocking().toFuture().get(10, TimeUnit.SECONDS);
        
        Assert.assertEquals("Request failed.", HttpResponseStatus.OK, status.get());
        Assert.assertEquals("Request failed.", "application/json", contentType.get());
        Assert.assertEquals("Invalid content length", (Integer)17, contentLength.get());
        Assert.assertTrue("The returned observable did not finish.", finishLatch.await(1, TimeUnit.MINUTES));
        Assert.assertEquals("Unexpected actual number of bytes", 17, response.capacity());
    }
    
    @Test
    public void shouldReturnNotFound() throws Exception {
        HttpServer<ByteBuf, ByteBuf> server = RxNetty.createHttpServer(0, new WebappFileRequestHandler())
                .start();
        
        final CountDownLatch finishLatch = new CountDownLatch(1);
        HttpClientResponse<ByteBuf> response = RxNetty.createHttpClient("localhost", server.getServerPort())
                                                      .submit(HttpClientRequest.createGet("/notfound.json"))
                                                      .finallyDo(new Action0() {
                                                          @Override
                                                          public void call() {
                                                              finishLatch.countDown();
                                                          }
                                                      }).toBlocking().toFuture().get(10, TimeUnit.SECONDS);
        
        Assert.assertTrue("The returned observable did not finish.", finishLatch.await(1, TimeUnit.MINUTES));
        Assert.assertEquals("Request failed.", HttpResponseStatus.NOT_FOUND, response.getStatus());
    }
    
    @Test
    public void shouldFailBasicHackAttempt() throws Exception {
        HttpServer<ByteBuf, ByteBuf> server = RxNetty.createHttpServer(0, new WebappFileRequestHandler())
                .start();
        
        final CountDownLatch finishLatch = new CountDownLatch(1);
        HttpClientResponse<ByteBuf> response = RxNetty.createHttpClient("localhost", server.getServerPort())
                                                      .submit(HttpClientRequest.createGet("/../badfile.json"))
                                                      .finallyDo(new Action0() {
                                                          @Override
                                                          public void call() {
                                                              finishLatch.countDown();
                                                          }
                                                      }).toBlocking().toFuture().get(10, TimeUnit.SECONDS);
        
        Assert.assertTrue("The returned observable did not finish.", finishLatch.await(1, TimeUnit.MINUTES));
        Assert.assertEquals("Request failed.", HttpResponseStatus.FORBIDDEN, response.getStatus());
    }

}
