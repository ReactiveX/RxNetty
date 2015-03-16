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

package io.reactivex.netty.examples.http.streaming;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.http.clientNew.HttpClient;
import io.reactivex.netty.protocol.http.clientNew.HttpClientResponse;
import rx.Observable;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class StreamingClient {

    private final int port;

    public StreamingClient(int port) {
        this.port = port;
    }

    public void startStreaming() throws InterruptedException, ExecutionException, TimeoutException {
        HttpClient.newClient("localhost", port)
                  .createGet("/hello")
                  .<String>switchMap((HttpClientResponse<ByteBuf> resp) -> {
                      System.out.println(resp);
                      final AtomicInteger counter = new AtomicInteger();
                      return resp.getContent()
                                 .buffer(500)
                                 .flatMap(buffers -> {
                                     for (ByteBuf buffer : buffers) {
                                         buffer.release();
                                     }
                                     int batchNumber = counter.incrementAndGet();
                                     System.out.println("Processing batch: " + batchNumber);
                                     return Observable.interval(100, TimeUnit.MILLISECONDS).take(1)
                                                      .map(anInt -> batchNumber);
                                 }, 1)
                                 .map(bacthNo -> "Batch: " + bacthNo);
                  })
                  .toBlocking()
                  .forEach(System.out::println);
    }

    public static void main(String[] args) throws InterruptedException, TimeoutException, ExecutionException {
        new StreamingClient(StreamingServer.DEFAULT_PORT).startStreaming();
    }
}
