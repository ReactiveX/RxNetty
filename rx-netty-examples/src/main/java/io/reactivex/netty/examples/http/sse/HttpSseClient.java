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
import rx.Notification;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

/**
 * @author Nitesh Kant
 */
public final class HttpSseClient {
    private int port;
    private int noOfEvents;

    ConcurrentLinkedQueue<ServerSentEvent> events = new ConcurrentLinkedQueue<ServerSentEvent>();

    private CountDownLatch doneSignal;

    public HttpSseClient(int port, int noOfEvents) {
        this.port = port;
        this.noOfEvents = noOfEvents;
    }

    public void readServerSideEvents() {
        doneSignal = new CountDownLatch(1);

        HttpClient<ByteBuf, ServerSentEvent> client =
                RxNetty.createHttpClient("localhost", port, PipelineConfigurators.<ByteBuf>sseClientConfigurator());

        client.submit(HttpClientRequest.createGet("/hello")).flatMap(new Func1<HttpClientResponse<ServerSentEvent>, Observable<ServerSentEvent>>() {
            @Override
            public Observable<ServerSentEvent> call(HttpClientResponse<ServerSentEvent> response) {
                System.out.println("New response received.");
                System.out.println("========================");
                System.out.println(response.getHttpVersion().text() + ' ' + response.getStatus().code()
                        + ' ' + response.getStatus().reasonPhrase());
                for (Map.Entry<String, String> header : response.getHeaders().entries()) {
                    System.out.println(header.getKey() + ": " + header.getValue());
                }
                return response.getContent();
            }
        }).takeWhile(new Func1<ServerSentEvent, Boolean>() {
            @Override
            public Boolean call(ServerSentEvent serverSentEvent) {
                if (events.size() < noOfEvents) {
                    return true;
                }
                doneSignal.countDown();
                return false;
            }
        }).materialize().forEach(new Action1<Notification<ServerSentEvent>>() {
            @Override
            public void call(Notification<ServerSentEvent> notification) {
                if (notification.isOnNext()) {
                    ServerSentEvent event = notification.getValue();
                    System.out.println(event.getEventName() + ':' + event.getEventData());
                    events.add(event);
                } else {
                    if (notification.isOnError()) {
                        System.err.printf("ERROR: communication with HttpSseServer failed");
                    }
                    doneSignal.countDown();
                }
            }
        });

        try {
            doneSignal.await();
        } catch (InterruptedException e) {
            // IGNORE
        }
    }

    public static void main(String[] args) {
        new HttpSseClient(8080, 10).readServerSideEvents();
    }
}
