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

package io.reactivex.netty.examples.http.post;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.StringTransformer;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import rx.Observable;
import rx.functions.Func1;

import java.nio.charset.Charset;

import static io.reactivex.netty.examples.http.post.SimplePostServer.DEFAULT_PORT;

/**
 * @author Tomasz Bak
 */
public class SimplePostClient {

    static final String MESSAGE = "Hello there!!!";

    private final int port;

    public SimplePostClient(int port) {
        this.port = port;
    }

    public String postMessage() {
        PipelineConfigurator<HttpClientResponse<ByteBuf>, HttpClientRequest<String>> pipelineConfigurator
                = PipelineConfigurators.httpClientConfigurator();

        HttpClient<String, ByteBuf> client = RxNetty.<String, ByteBuf>newHttpClientBuilder("localhost", port)
                                                    .pipelineConfigurator(pipelineConfigurator)
                                                    .enableWireLogging(LogLevel.ERROR).build();

        HttpClientRequest<String> request = HttpClientRequest.create(HttpMethod.POST, "test/post");
        request.withRawContentSource(Observable.just(MESSAGE), StringTransformer.DEFAULT_INSTANCE);

        String result = client.submit(request).flatMap(new Func1<HttpClientResponse<ByteBuf>, Observable<String>>() {
            @Override
            public Observable<String> call(HttpClientResponse<ByteBuf> response) {
                return response.getContent().map(new Func1<ByteBuf, String>() {
                    @Override
                    public String call(ByteBuf byteBuf) {
                        return byteBuf.toString(Charset.defaultCharset());
                    }
                });
            }
        }).toBlocking().single();

        return result;
    }

    public static void main(String[] args) {
        System.out.println("Sending POST request to the server...");
        String replyMessage = new SimplePostClient(DEFAULT_PORT).postMessage();
        System.out.println("Received " + replyMessage);
    }
}
