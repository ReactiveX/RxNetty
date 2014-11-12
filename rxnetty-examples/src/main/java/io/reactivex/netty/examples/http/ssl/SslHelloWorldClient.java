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
package io.reactivex.netty.examples.http.ssl;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.pipeline.ssl.DefaultFactories;
import io.reactivex.netty.protocol.http.client.FlatResponseOperator;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.ResponseHolder;
import rx.functions.Func1;

import java.nio.charset.Charset;

import static io.reactivex.netty.examples.http.ssl.SslHelloWorldServer.DEFAULT_PORT;

/**
 * @author Tomasz Bak
 */
public class SslHelloWorldClient {

    private final int port;

    public SslHelloWorldClient(int port) {
        this.port = port;
    }

    public HttpResponseStatus sendHelloRequest() throws Exception {
        HttpClient<ByteBuf, ByteBuf> rxClient = RxNetty.<ByteBuf, ByteBuf>newHttpClientBuilder("localhost", port)
                .withSslEngineFactory(DefaultFactories.trustAll())
                .build();

        HttpResponseStatus statusCode = rxClient.submit(HttpClientRequest.createGet("/hello"))
                                                .lift(FlatResponseOperator.<ByteBuf>flatResponse())
                                                .map(new Func1<ResponseHolder<ByteBuf>, ResponseHolder<ByteBuf>>() {
                                                    @Override
                                                    public ResponseHolder<ByteBuf> call(ResponseHolder<ByteBuf> holder) {
                                                        System.out.println(holder.getContent().toString(
                                                                Charset.defaultCharset()));
                                                        System.out.println("=======================");
                                                        return holder;
                                                    }
                                                })
                                                .toBlocking().single().getResponse().getStatus();
        return statusCode;
    }

    public static void main(String[] args) {
        try {
            new SslHelloWorldClient(DEFAULT_PORT).sendHelloRequest();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
