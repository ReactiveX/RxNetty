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
import io.reactivex.netty.client.ChannelPool;
import io.reactivex.netty.client.RxClientImpl;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.pipeline.PipelineConfiguratorComposite;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.subscriptions.Subscriptions;

public class HttpClientImpl<I, O> extends RxClientImpl<HttpClientRequest<I>, HttpClientResponse<O>> implements HttpClient<I, O> {

    public HttpClientImpl(ServerInfo serverInfo, Bootstrap clientBootstrap,
            PipelineConfigurator<HttpClientResponse<O>, HttpClientRequest<I>> pipelineConfigurator, ClientConfig clientConfig, ChannelPool pool) {
        super(serverInfo, clientBootstrap, pipelineConfigurator, clientConfig, pool);
    }
    
    public HttpClientImpl(ServerInfo serverInfo, Bootstrap clientBootstrap,
                          PipelineConfigurator<HttpClientResponse<O>, HttpClientRequest<I>> pipelineConfigurator, ClientConfig clientConfig) {
        super(serverInfo, clientBootstrap, pipelineConfigurator, clientConfig);
    }

    @Override
    public Observable<HttpClientResponse<O>> submit(HttpClientRequest<I> request) {
        Observable<ObservableConnection<HttpClientResponse<O>, HttpClientRequest<I>>> connectionObservable = connect();
        return submit(request, connectionObservable);
    }

    @Override
    public Observable<HttpClientResponse<O>> submit(HttpClientRequest<I> request, ClientConfig config) {
        Observable<ObservableConnection<HttpClientResponse<O>, HttpClientRequest<I>>> connectionObservable = connect();
        return submit(request, connectionObservable, config);
    }

    protected Observable<HttpClientResponse<O>> submit(final HttpClientRequest<I> request,
                                              Observable<ObservableConnection<HttpClientResponse<O>, HttpClientRequest<I>>> connectionObservable) {
        return submit(request, connectionObservable, null == clientConfig
                                                     ? HttpClientConfig.DEFAULT_CONFIG : clientConfig);
    }

    protected Observable<HttpClientResponse<O>> submit(final HttpClientRequest<I> request,
                                                 final Observable<ObservableConnection<HttpClientResponse<O>, HttpClientRequest<I>>> connectionObservable,
                                                 final ClientConfig config) {
        enrichRequest(request, config);

        // Here we do not map the connection Observable and return because the onComplete() of connectionObservable,
        // does not indicate onComplete of the request processing.
        return Observable.create(new Observable.OnSubscribe<HttpClientResponse<O>>() {
            @Override
            public void call(final Subscriber<? super HttpClientResponse<O>> subscriber) {
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
    protected PipelineConfigurator<HttpClientRequest<I>, HttpClientResponse<O>> getPipelineConfiguratorForAChannel(ClientConnectionHandler clientConnectionHandler,
                                                                                                       PipelineConfigurator<HttpClientResponse<O>, HttpClientRequest<I>> pipelineConfigurator) {
        PipelineConfigurator<HttpClientResponse<O>, HttpClientRequest<I>> configurator =
                new PipelineConfiguratorComposite<HttpClientResponse<O>, HttpClientRequest<I>>(pipelineConfigurator,
                                                                                   new ClientRequiredConfigurator<I, O>());
        return super.getPipelineConfiguratorForAChannel(clientConnectionHandler, configurator);
    }

    private void enrichRequest(HttpClientRequest<I> request, ClientConfig config) {
        if(!request.getHeaders().contains(HttpHeaders.Names.HOST)) {
            request.getHeaders().add(HttpHeaders.Names.HOST, serverInfo.getHost());
        }

        if (config instanceof HttpClientConfig) {
            HttpClientConfig httpClientConfig = (HttpClientConfig) config;
            if (httpClientConfig.getUserAgent() != null && request.getHeaders().get(HttpHeaders.Names.USER_AGENT) == null) {
                request.getHeaders().set(HttpHeaders.Names.USER_AGENT, httpClientConfig.getUserAgent());
            }
        }
    }

    private class ConnectObserver<I, O> extends CompositeObserver<ObservableConnection<HttpClientResponse<O>, HttpClientRequest<I>>> {

        private final HttpClientRequest<I> request;
        private final Observer<? super HttpClientResponse<O>> requestProcessingObserver;

        public ConnectObserver(HttpClientRequest<I> request, Observer<? super HttpClientResponse<O>> requestProcessingObserver) {
            super(requestProcessingObserver);
            this.request = request;
            this.requestProcessingObserver = requestProcessingObserver;
        }

        @Override
        public void onCompleted() {
            // We do not want an onComplete() call to Request Processing Observer on onComplete of connection observable.
        }

        @Override
        public void onNext(ObservableConnection<HttpClientResponse<O>, HttpClientRequest<I>> connection) {
            System.err.println("Connection established");
            ClientRequestResponseConverter converter =
                    connection.getChannelHandlerContext().pipeline().get(ClientRequestResponseConverter.class);
            if (null != converter) {
                converter.setRequestProcessingObserver(requestProcessingObserver);
                converter.setObservableConnection(connection);
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