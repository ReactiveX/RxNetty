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
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import rx.functions.Action1;

import java.nio.charset.Charset;
import java.util.Map;

/**
 * @author Nitesh Kant
 */
public class HelloWorldClient {

    private final int port;
    volatile HttpClientResponse<ByteBuf> lastResponse;

    public HelloWorldClient(int port) {
        this.port = port;
    }

    public void sendHelloRequest() {
        RxNetty.createHttpGet("http://localhost:" + port + "/hello").toBlocking().forEach(new Action1<HttpClientResponse<ByteBuf>>() {

            @Override
            public void call(HttpClientResponse<ByteBuf> response) {
                HelloWorldClient.this.lastResponse = response;

                System.out.println("New response received.");
                System.out.println("========================");
                System.out.println(response.getHttpVersion().text() + ' ' + response.getStatus().code()
                        + ' ' + response.getStatus().reasonPhrase());
                for (Map.Entry<String, String> header : response.getHeaders().entries()) {
                    System.out.println(header.getKey() + ": " + header.getValue());
                }

                response.getContent().subscribe(new Action1<ByteBuf>() {
                    @Override
                    public void call(ByteBuf content) {
                        System.out.print(content.toString(Charset.defaultCharset()));
                        System.out.println("========================");
                    }
                });
            }
        });
    }

    public static void main(String[] args) {
        new HelloWorldClient(8080).sendHelloRequest();
    }
}
