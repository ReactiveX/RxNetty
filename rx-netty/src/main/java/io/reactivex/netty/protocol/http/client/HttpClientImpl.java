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

import java.net.URI;

import io.netty.bootstrap.Bootstrap;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.client.RxClientImpl;
import io.reactivex.netty.client.pool.ChannelPool;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.pipeline.PipelineConfiguratorComposite;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
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
    
    private static boolean isFollowRedirect(ClientConfig config) {
        if (config instanceof HttpClientConfig) {
            return ((HttpClientConfig) config).isFollowRedirect();
        }
        return false;
    }

    protected Observable<HttpClientResponse<O>> submit(final HttpClientRequest<I> request,
                                                 final Observable<ObservableConnection<HttpClientResponse<O>, HttpClientRequest<I>>> connectionObservable,
                                                 final ClientConfig config) {
        boolean isFollowRedirect = isFollowRedirect(config);
        final HttpClientRequest<I> _request;
        if (isFollowRedirect && request.hasContentSource()){
            // need to make sure content source 
            // is repeatable when we resubmit the request to the redirected host
            _request = new RepeatableContentHttpRequest<I>(request);
        } else {
            _request = request;
        }
        enrichRequest(_request, config);

        // Here we do not map the connection Observable and return because the onComplete() of connectionObservable,
        // does not indicate onComplete of the request processing.
        final Observable<HttpClientResponse<O>> response = Observable.create(new Observable.OnSubscribe<HttpClientResponse<O>>() {
            @Override
            public void call(final Subscriber<? super HttpClientResponse<O>> subscriber) {
                final ConnectObserver<I, O> connectObserver = new ConnectObserver<I, O>(_request, subscriber);
                final Subscription connectSubscription = connectionObservable.subscribe(connectObserver);
                subscriber.add(Subscriptions.create(new Action0() {
                    @Override
                    public void call() {
                        connectSubscription.unsubscribe();
                        connectObserver.cancel(); // Also closes the connection.
                    }
                }));
            }
        });
        if (!isFollowRedirect) {
            return response;
        }
        // follow redirect logic
        return response.flatMap(new Func1<HttpClientResponse<O>, Observable<HttpClientResponse<O>>>() {
            @Override
            public Observable<HttpClientResponse<O>> call(
                    HttpClientResponse<O> responseToCheckRedirect) {
                int statusCode = responseToCheckRedirect.getStatus().code();
                switch (statusCode) {
                case 301:
                case 302:
                case 303:
                case 307:
                case 308:
                    String location = responseToCheckRedirect.getHeaders().get(HttpHeaders.Names.LOCATION);
                    if (location == null) {
                        return Observable.error(new Exception("Location header is not set in the redirect response"));
                    } 
                    URI uri;
                    try {
                        uri = new URI(location);
                    } catch (Exception e) {
                        return Observable.error(e);
                    }
                    String host = uri.getHost();
                    int port = uri.getPort();
                    if (port < 0) {
                        port = 80;
                    }
                    HttpClientImpl<I, O> redirectClient = new HttpClientImpl<I, O>(new ServerInfo(host, port), clientBootstrap, incompleteConfigurator, config);
                    HttpClientRequest<I> newRequest = copyRequest(_request, uri.getRawPath(), statusCode);
                    newRequest.getHeaders().set(HttpHeaders.Names.HOST, host);
                    return redirectClient.submit(newRequest, config);
                default:
                    break;
                }
                return Observable.from(responseToCheckRedirect);
            }
        });
    }

    private static <I> HttpClientRequest<I> copyRequest(HttpClientRequest<I> original, String newURI, int statusCode) {
        HttpRequest nettyRequest = original.getNettyRequest();
        nettyRequest.setUri(newURI);
        if (statusCode == 303) {
            // according to HTTP spec, 303 mandates the change of request type to GET
            nettyRequest.setMethod(HttpMethod.GET);
        }
        HttpClientRequest<I> newRequest = new HttpClientRequest<I>(nettyRequest);
        if (statusCode != 303) {
            // if status code is 303, we can just leave the content factory to be null
            newRequest.contentFactory = original.contentFactory;
            newRequest.rawContentFactory = original.rawContentFactory;
        }
        return newRequest;
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
        private ObservableConnection<HttpClientResponse<O>, HttpClientRequest<I>> connection; // Nullable

        public ConnectObserver(HttpClientRequest<I> request, Observer<? super HttpClientResponse<O>> requestProcessingObserver) {
            super(requestProcessingObserver);
            this.request = request;
            this.requestProcessingObserver = requestProcessingObserver;
        }

        @Override
        public void onCompleted() {
            // We do not want an onComplete() call to Request Processing Observer on onComplete of connection observable.
            // If we do not override this, super.onCompleted() will invoke onCompleted on requestProcessingObserver.
        }

        @Override
        public void onNext(ObservableConnection<HttpClientResponse<O>, HttpClientRequest<I>> newConnection) {
            connection = newConnection;
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

        Observable<Void> cancel() {
            if (null != connection) {
                return connection.close(); // Also cancels any pending writes.
            } else {
                return Observable.empty();
            }
        }
    }
}