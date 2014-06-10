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
package io.reactivex.netty.examples.java;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.text.sse.ServerSentEvent;
import rx.Observable;
import rx.functions.Action1;

import java.util.Map;

/**
 * @author Nitesh Kant
 */
public final class HttpSseClient {

    public static void main(String[] args) {
        HttpClient<ByteBuf, ServerSentEvent> client =
                RxNetty.createHttpClient("localhost", 8080, PipelineConfigurators.<ByteBuf>sseClientConfigurator());

        Observable<HttpClientResponse<ServerSentEvent>> response = client.submit(HttpClientRequest.createGet("/hello"));
        response.toBlocking().forEach(new Action1<HttpClientResponse<ServerSentEvent>>() {
            @Override
            public void call(HttpClientResponse<ServerSentEvent> response) {
                System.out.println("New response recieved.");
                System.out.println("========================");
                System.out.println(response.getHttpVersion().text() + ' ' + response.getStatus().code()
                                   + ' ' + response.getStatus().reasonPhrase());
                for (Map.Entry<String, String> header : response.getHeaders().entries()) {
                    System.out.println(header.getKey() + ": " + header.getValue());
                }

                response.getContent().subscribe(new Action1<ServerSentEvent>() {
                    @Override
                    public void call(ServerSentEvent event) {
                        System.out.println(event.getEventName() + ':' + event.getEventData());
                    }
                });
            }
        });
    }
}
