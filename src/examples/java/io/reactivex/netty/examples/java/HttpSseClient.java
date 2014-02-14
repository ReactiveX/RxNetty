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
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.pipeline.PipelineConfiguratorComposite;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpRequest;
import io.reactivex.netty.protocol.http.client.HttpResponse;
import io.reactivex.netty.protocol.text.sse.SSEEvent;
import rx.Observable;
import rx.Observer;
import rx.util.functions.Action1;

import java.util.Map;

/**
 * @author Nitesh Kant
 */
public final class HttpSseClient {

    public static void main(String[] args) {
        HttpClient<ByteBuf, SSEEvent> client =
                RxNetty.createHttpClient("localhost", 8080,
                                         new PipelineConfiguratorComposite<HttpResponse<SSEEvent>, HttpRequest<ByteBuf>>(new PipelineConfigurator() {

                                             @Override
                                             public void configureNewPipeline(ChannelPipeline pipeline) {
                                                 pipeline.addFirst(new LoggingHandler(LogLevel.ERROR));
                                             }
                                         }, PipelineConfigurators.<ByteBuf>sseClientConfigurator()));

        Observable<HttpResponse<SSEEvent>> response = client.submit(new HttpRequest.RequestBuilder<ByteBuf>(HttpMethod.GET, "/hello").build());
        response.subscribe(new Observer<HttpResponse<SSEEvent>>() {
            @Override
            public void onCompleted() {
                System.out.println("Response complete.");
            }

            @Override
            public void onError(Throwable e) {
                System.out.println("Error occured while sending/recieving Http request/response. Error: ");
                e.printStackTrace(System.out);
            }

            @Override
            public void onNext(HttpResponse<SSEEvent> response) {
                System.out.println("New response recieved.");
                System.out.println("========================");
                System.out.println(response.getHttpVersion().text() + ' ' + response.getStatus().code()
                                   + ' ' + response.getStatus().reasonPhrase());
                for (Map.Entry<String, String> header : response.getHeaders().entries()) {
                    System.out.println(header.getKey() + ": " + header.getValue());
                }

                response.getContent().subscribe(new Action1<SSEEvent>() {
                    @Override
                    public void call(SSEEvent event) {
                        System.out.print(event.getEventData());
                        System.out.println("========================");
                    }
                });
            }
        });
    }
}
