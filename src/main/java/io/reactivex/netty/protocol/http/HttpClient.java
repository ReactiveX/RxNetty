/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.reactivex.netty.protocol.http;

import io.netty.bootstrap.Bootstrap;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.reactivex.netty.ObservableConnection;
import io.reactivex.netty.client.RxClient;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import rx.Observable;
import rx.subjects.PublishSubject;
import rx.util.functions.Action1;
import rx.util.functions.Func1;

public class HttpClient<I extends HttpRequest, O> extends RxClient<I, O> {

    public HttpClient(ServerInfo serverInfo, Bootstrap clientBootstrap,
                      PipelineConfigurator<O, I> pipelineConfigurator) {
        super(serverInfo, clientBootstrap, pipelineConfigurator);
    }

    public Observable<ObservableHttpResponse<O>> connectAndObserve(I request) {
        Observable<ObservableConnection<O, I>> connectionObservable = connect();
        return observe(request, connectionObservable);
    }

    public Observable<ObservableHttpResponse<O>> connectAndObserve(I request, RequestConfig config) {
        Observable<ObservableConnection<O, I>> connectionObservable = connect();
        return observe(request, connectionObservable, config);
    }

    public Observable<ObservableHttpResponse<O>> observe(final I request,
                                                         Observable<ObservableConnection<O, I>> connectionObservable) {
        return observe(request, connectionObservable, RequestConfig.DEFAULT_CONFIG);
    }

    public Observable<ObservableHttpResponse<O>> observe(final I request,
                                                         Observable<ObservableConnection<O, I>> connectionObservable,
                                                         RequestConfig config) {
        enrichRequest(request, config);
        return connectionObservable.map(new Func1<ObservableConnection<O, I>, ObservableHttpResponse<O>>() {
            @Override
            public ObservableHttpResponse<O> call(ObservableConnection<O, I> connection) {
                final PublishSubject<HttpResponse> headerSubject = PublishSubject.create();
                final PublishSubject<O> contentSubject = PublishSubject.create();
                final ObservableHttpResponse<O> observableResponse =
                        new ObservableHttpResponse<O>(connection, headerSubject, contentSubject);
                connection.write(request).doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        // If the write fails, the response should get the error. Completion & onNext are managed by
                        // the response observable itself.
                        headerSubject.onError(throwable);
                        contentSubject.onError(throwable);
                    }
                });
                return observableResponse;
            }
        });
    }

    private void enrichRequest(I request, RequestConfig config) {
        if (config.getUserAgent() != null && request.headers().get(HttpHeaders.Names.USER_AGENT) == null) {
            request.headers().set(HttpHeaders.Names.USER_AGENT, config.getUserAgent());
        }
    }

    public static class RequestConfig {

        private static final RequestConfig DEFAULT_CONFIG = new RequestConfig();

        private String userAgent = "RxNetty Client";

        private RequestConfig() {
            // Only the builder can create this instance, so that we can change the constructor signature at will.
        }

        public String getUserAgent() {
            return userAgent;
        }

        public static class Builder {

            private final RequestConfig config;

            public Builder() {
                config = new RequestConfig();
            }

            public Builder userAgent(String userAgent) {
                config.userAgent = userAgent;
                return this;
            }

            public RequestConfig build() {
                return config;
            }
        }
    }
}