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

package io.reactivex.netty.examples.http.sse;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.text.sse.ServerSentEvent;
import rx.Observable;
import rx.functions.Func1;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.reactivex.netty.examples.http.sse.HttpSseServer.DEFAULT_PORT;

/**
 * @author Nitesh Kant
 */
public final class HttpSseClient {
    static final int DEFAULT_NO_OF_EVENTS = 100;

    private final int port;
    private final int noOfEvents;

    public HttpSseClient(int port, int noOfEvents) {
        this.port = port;
        this.noOfEvents = noOfEvents;
    }

    public List<ServerSentEvent> readServerSideEvents() {
        HttpClient<ByteBuf, ServerSentEvent> client =
                RxNetty.createHttpClient("localhost", port, PipelineConfigurators.<ByteBuf>sseClientConfigurator());

        Iterable<ServerSentEvent> eventIterable = client.submit(HttpClientRequest.createGet("/hello")).
                flatMap(new Func1<HttpClientResponse<ServerSentEvent>, Observable<ServerSentEvent>>() {
                    @Override
                    public Observable<ServerSentEvent> call(HttpClientResponse<ServerSentEvent> response) {
                        printResponseHeader(response);
                        return response.getContent();
                    }
                }).take(noOfEvents).toBlocking().toIterable();

        List<ServerSentEvent> events = new ArrayList<ServerSentEvent>();
        for (ServerSentEvent event : eventIterable) {
            System.out.println(event);
            events.add(event);
        }

        return events;
    }

    private static void printResponseHeader(HttpClientResponse<ServerSentEvent> response) {
        System.out.println("New response received.");
        System.out.println("========================");
        System.out.println(response.getHttpVersion().text() + ' ' + response.getStatus().code()
                + ' ' + response.getStatus().reasonPhrase());
        for (Map.Entry<String, String> header : response.getHeaders().entries()) {
            System.out.println(header.getKey() + ": " + header.getValue());
        }
    }

    public static void main(String[] args) {
        new HttpSseClient(DEFAULT_PORT, DEFAULT_NO_OF_EVENTS).readServerSideEvents();
    }
}
