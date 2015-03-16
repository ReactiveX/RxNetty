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

package io.reactivex.netty.examples.http.helloworld;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.http.clientNew.HttpClient;
import io.reactivex.netty.protocol.http.clientNew.HttpClientResponse;
import rx.Observable;

import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static io.reactivex.netty.examples.http.helloworld.HelloWorldServer.*;

public class HelloWorldClient {

    private final int port;

    public HelloWorldClient(int port) {
        this.port = port;
    }

    public String sendHelloRequest() throws InterruptedException, ExecutionException, TimeoutException {
        return HttpClient.newClient("localhost", port)
                         .createGet("/hello")
                         .writeStringContent(Observable.just("Say Hello!"))
                         .switchMap((HttpClientResponse<ByteBuf> resp) -> {
                             System.out.println(resp);
                             return resp.getContent()
                                        .map(bb -> bb.toString(Charset.defaultCharset()));
                         })
                         .toBlocking()
                         .toFuture().get(1, TimeUnit.HOURS);
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException, TimeoutException {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }
        String value = new HelloWorldClient(port).sendHelloRequest();
        System.out.println(value);
    }
}
