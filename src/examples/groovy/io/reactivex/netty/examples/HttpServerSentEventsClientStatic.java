/**
 * Copyright 2013 Netflix, Inc.
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
package io.reactivex.netty.examples;

import rx.Observable;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.http.HttpNettyPipelineConfigurator;
import io.reactivex.netty.http.sse.codec.SSEEvent;
import io.reactivex.netty.http.ObservableHttpResponse;
import rx.util.functions.Action1;
import rx.util.functions.Func1;

public class HttpServerSentEventsClientStatic {

    public static void main(String[] args) {

        RxNetty.createHttpRequest("http://ec2-107-22-122-75.compute-1.amazonaws.com:7001/turbine.stream?cluster=api-prod-c0us.ca", HttpNettyPipelineConfigurator.SSE_HANDLER)
                .flatMap(new Func1<ObservableHttpResponse<SSEEvent>, Observable<SSEEvent>>() {

                    @Override
                    public Observable<SSEEvent> call(ObservableHttpResponse<SSEEvent> response) {
                        
                        
                        System.out.println("Received connection: " + response.response().getStatus());
                        return response.content();
                    }

                })
                .take(10)
                .toBlockingObservable().forEach(new Action1<SSEEvent>() {

                    @Override
                    public void call(SSEEvent message) {
                        System.out.println("Message => " + message.getEventData().trim());
                    }

                });
    }
    
    
    public static SSEEvent executeRequest(String url) {
        return createRequest().toBlockingObservable().last();
    }

    public static Observable<SSEEvent> createRequest() {
        return RxNetty.createHttpRequest("http://ec2-107-22-122-75.compute-1.amazonaws.com:7001/turbine.stream?cluster=api-prod-c0us.ca", HttpNettyPipelineConfigurator.SSE_HANDLER)
        .flatMap(new Func1<ObservableHttpResponse<SSEEvent>, Observable<SSEEvent>>() {

            @Override
            public Observable<SSEEvent> call(ObservableHttpResponse<SSEEvent> response) {
                
                
                System.out.println("Received connection: " + response.response().getStatus());
                return response.content();
            }

        });
    }
    
    
}
