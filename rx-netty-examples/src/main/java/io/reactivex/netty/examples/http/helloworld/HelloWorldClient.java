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

package io.reactivex.netty.examples.http.helloworld;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.client.FlatResponseOperator;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.http.client.ResponseHolder;
import rx.functions.Func1;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static io.reactivex.netty.examples.http.helloworld.HelloWorldServer.DEFAULT_PORT;

/**
 * @author Nitesh Kant
 */
public class HelloWorldClient {

    private final int port;

    public HelloWorldClient(int port) {
        this.port = port;
    }

    public HttpClientResponse<ByteBuf> sendHelloRequest() throws InterruptedException, ExecutionException, TimeoutException {
        return RxNetty.createHttpGet("http://localhost:" + port + "/hello")
               .lift(FlatResponseOperator.<ByteBuf>flatResponse())
               .map(new Func1<ResponseHolder<ByteBuf>, HttpClientResponse<ByteBuf>>() {
                   @Override
                   public HttpClientResponse<ByteBuf> call(ResponseHolder<ByteBuf> holder) {
                       printResponseHeader(holder.getResponse());
                       System.out.println(holder.getContent().toString(Charset.defaultCharset()));
                       System.out.println("========================");
                       return holder.getResponse();
                   }
               }).toBlocking().toFuture().get(1, TimeUnit.MINUTES);
    }

    public void printResponseHeader(HttpClientResponse<ByteBuf> response) {
        System.out.println("New response received.");
        System.out.println("========================");
        System.out.println(response.getHttpVersion().text() + ' ' + response.getStatus().code()
                + ' ' + response.getStatus().reasonPhrase());
        for (Map.Entry<String, String> header : response.getHeaders().entries()) {
            System.out.println(header.getKey() + ": " + header.getValue());
        }
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException, TimeoutException {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }
        new HelloWorldClient(port).sendHelloRequest();
    }
}
