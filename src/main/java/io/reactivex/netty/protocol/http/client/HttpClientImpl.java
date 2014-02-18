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
package io.reactivex.netty.protocol.http.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.handler.codec.http.HttpHeaders;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.client.RxClientImpl;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.pipeline.PipelineConfiguratorComposite;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;
import rx.util.functions.Action1;

public class HttpClientImpl<I, O> extends RxClientImpl<HttpRequest<I>, HttpResponse<O>> implements HttpClient<I, O> {

    public HttpClientImpl(ServerInfo serverInfo, Bootstrap clientBootstrap,
                          PipelineConfigurator<HttpResponse<O>, HttpRequest<I>> pipelineConfigurator, ClientConfig clientConfig) {
        super(serverInfo, clientBootstrap, pipelineConfigurator, clientConfig);
    }

    @Override
    public Observable<HttpResponse<O>> submit(HttpRequest<I> request) {
        Observable<ObservableConnection<HttpResponse<O>, HttpRequest<I>>> connectionObservable = connect();
        return submit(request, connectionObservable);
    }

    @Override
    public Observable<HttpResponse<O>> submit(HttpRequest<I> request, ClientConfig config) {
        Observable<ObservableConnection<HttpResponse<O>, HttpRequest<I>>> connectionObservable = connect();
        return submit(request, connectionObservable, config);
    }

    protected Observable<HttpResponse<O>> submit(final HttpRequest<I> request,
                                              Observable<ObservableConnection<HttpResponse<O>, HttpRequest<I>>> connectionObservable) {
        return submit(request, connectionObservable, null == clientConfig
                                                     ? HttpClientConfig.DEFAULT_CONFIG : clientConfig);
    }

    protected Observable<HttpResponse<O>> submit(final HttpRequest<I> request,
                                                 final Observable<ObservableConnection<HttpResponse<O>, HttpRequest<I>>> connectionObservable,
                                                 final ClientConfig config) {
        enrichRequest(request, config);

        return Observable.create(new Observable.OnSubscribe<HttpResponse<O>>() {
            @Override
            public void call(final Subscriber<? super HttpResponse<O>> subscriber) {
                final Subscription connectSubscription =
                        connectionObservable.subscribe(new ConnectObserver<I, O>(request, subscriber));

                subscriber.add(Subscriptions.create(new Action0() {
                    @Override
                    public void call() {
                        //TODO: Cancel write & if the response is not over, disconnect the channel.
                        connectSubscription.unsubscribe();
                    }
                }));
            }
        });
    }

    @Override
    protected PipelineConfigurator<HttpRequest<I>, HttpResponse<O>> getPipelineConfiguratorForAChannel(ClientConnectionHandler clientConnectionHandler,
                                                                                                       PipelineConfigurator<HttpResponse<O>, HttpRequest<I>> pipelineConfigurator) {
        PipelineConfigurator<HttpResponse<O>, HttpRequest<I>> configurator =
                new PipelineConfiguratorComposite<HttpResponse<O>, HttpRequest<I>>(pipelineConfigurator,
                                                                                   new ClientRequiredConfigurator<I, O>());
        return super.getPipelineConfiguratorForAChannel(clientConnectionHandler, configurator);
    }

    private void enrichRequest(HttpRequest<I> request, ClientConfig config) {
        if (config instanceof HttpClientConfig) {
            HttpClientConfig httpClientConfig = (HttpClientConfig) config;
            if (httpClientConfig.getUserAgent() != null && request.getHeaders().get(HttpHeaders.Names.USER_AGENT) == null) {
                request.getHeaders().set(HttpHeaders.Names.USER_AGENT, httpClientConfig.getUserAgent());
            }
        }
    }

    private class ConnectObserver<I, O> extends CompositeObserver<ObservableConnection<HttpResponse<O>, HttpRequest<I>>> {

        private final HttpRequest<I> request;
        private final Observer<? super HttpResponse<O>> requestProcessingObserver;

        public ConnectObserver(HttpRequest<I> request, Observer<? super HttpResponse<O>> requestProcessingObserver) {
            super(requestProcessingObserver);
            this.request = request;
            this.requestProcessingObserver = requestProcessingObserver;
        }

        @Override
        public void onNext(ObservableConnection<HttpResponse<O>, HttpRequest<I>> connection) {
            ClientRequestResponseConverter converter =
                    connection.getChannelHandlerContext().pipeline().get(ClientRequestResponseConverter.class);
            if (null != converter) {
                converter.setRequestProcessingObserver(requestProcessingObserver);
            }

            connection.getInput().subscribe(requestProcessingObserver);
            connection.writeAndFlush(request).doOnError(new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    // If the write fails, the response should get the error. Completion & onNext are managed by
                    // the response observable itself.
                    requestProcessingObserver.onError(throwable);
                }
            });
        }
    }
}