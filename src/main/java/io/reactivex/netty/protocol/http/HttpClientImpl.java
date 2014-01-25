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
import io.reactivex.netty.client.RxClientImpl;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subjects.PublishSubject;
import rx.util.functions.Action1;

public class HttpClientImpl<I extends HttpRequest, O> extends RxClientImpl<I, O> implements HttpClient<I,O> {

    private final RequestConfig globalRequestConfig;

    public HttpClientImpl(ServerInfo serverInfo, Bootstrap clientBootstrap,
                          PipelineConfigurator<O, I> pipelineConfigurator, RequestConfig globalRequestConfig) {
        super(serverInfo, clientBootstrap, pipelineConfigurator);
        this.globalRequestConfig = globalRequestConfig;
    }

    @Override
    public Observable<ObservableHttpResponse<O>> submit(I request) {
        Observable<ObservableConnection<O, I>> connectionObservable = connect();
        return submit(request, connectionObservable);
    }

    @Override
    public Observable<ObservableHttpResponse<O>> submit(I request, RequestConfig config) {
        Observable<ObservableConnection<O, I>> connectionObservable = connect();
        return submit(request, connectionObservable, config);
    }

    protected Observable<ObservableHttpResponse<O>> submit(final I request,
                                                           Observable<ObservableConnection<O, I>> connectionObservable) {
        return submit(request, connectionObservable, null == globalRequestConfig
                                                     ? RequestConfig.DEFAULT_CONFIG : globalRequestConfig);
    }

    protected Observable<ObservableHttpResponse<O>> submit(final I request,
                                                           final Observable<ObservableConnection<O, I>> connectionObservable,
                                                           RequestConfig config) {
        enrichRequest(request, config);

        return Observable.create(new Observable.OnSubscribeFunc<ObservableHttpResponse<O>>() {
            @Override
            public Subscription onSubscribe(final Observer<? super ObservableHttpResponse<O>> observer) {
                    connectionObservable.subscribe(new Action1<ObservableConnection<O, I>>() {
                        @Override
                        public void call(ObservableConnection<O, I> connection) {
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
                            observer.onNext(observableResponse);
                        }
                    }, new Action1<Throwable>() {
                           @Override
                           public void call(Throwable throwable) {
                               observer.onError(throwable);
                           }
                       }
                );

                return new Subscription() {
                    @Override
                    public void unsubscribe() {
                    }
                };
            }
        });
    }

    private void enrichRequest(I request, RequestConfig config) {
        if (config.getUserAgent() != null && request.headers().get(HttpHeaders.Names.USER_AGENT) == null) {
            request.headers().set(HttpHeaders.Names.USER_AGENT, config.getUserAgent());
        }
    }
}