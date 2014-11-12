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
import io.netty.handler.codec.http.HttpMethod;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.client.ClientChannelFactory;
import io.reactivex.netty.client.ClientConnectionFactory;
import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.client.ConnectionPool;
import io.reactivex.netty.client.ConnectionPoolBuilder;
import io.reactivex.netty.client.RxClientImpl;
import io.reactivex.netty.metrics.Clock;
import io.reactivex.netty.metrics.MetricEventsSubject;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.pipeline.PipelineConfiguratorComposite;
import rx.Observable;
import rx.functions.Action0;

public class HttpClientImpl<I, O> extends RxClientImpl<HttpClientRequest<I>, HttpClientResponse<O>> implements HttpClient<I, O> {

    private final String hostHeaderValue;

    public HttpClientImpl(String name, ServerInfo serverInfo, Bootstrap clientBootstrap,
                          PipelineConfigurator<HttpClientResponse<O>, HttpClientRequest<I>> pipelineConfigurator,
                          ClientConfig clientConfig,
                          ClientChannelFactory<HttpClientResponse<O>, HttpClientRequest<I>> channelFactory,
                          ClientConnectionFactory<HttpClientResponse<O>, HttpClientRequest<I>,
                                  ? extends ObservableConnection<HttpClientResponse<O>, HttpClientRequest<I>>> connectionFactory,
                          MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject) {
        super(name, serverInfo, clientBootstrap, pipelineConfigurator, clientConfig, channelFactory, connectionFactory,
              eventsSubject);
        hostHeaderValue = prepareHostHeaderValue();
    }

    public HttpClientImpl(String name, ServerInfo serverInfo, Bootstrap clientBootstrap,
                          PipelineConfigurator<HttpClientResponse<O>, HttpClientRequest<I>> pipelineConfigurator,
                          ClientConfig clientConfig,
                          ConnectionPoolBuilder<HttpClientResponse<O>, HttpClientRequest<I>> poolBuilder,
                          MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject) {
        super(name, serverInfo, clientBootstrap, pipelineConfigurator, clientConfig, poolBuilder, eventsSubject);
        hostHeaderValue = prepareHostHeaderValue();
    }

    @Override
    public Observable<HttpClientResponse<O>> submit(HttpClientRequest<I> request) {
        return submit(request, connect());
    }

    @Override
    public Observable<HttpClientResponse<O>> submit(HttpClientRequest<I> request, ClientConfig config) {
        return submit(request, connect(), config);
    }

    protected Observable<HttpClientResponse<O>> submit(final HttpClientRequest<I> request,
                                                       Observable<ObservableConnection<HttpClientResponse<O>, HttpClientRequest<I>>> connectionObservable) {
        return submit(request, connectionObservable, null == clientConfig
                                                     ? HttpClientConfig.Builder.newDefaultConfig() : clientConfig);
    }

    protected Observable<HttpClientResponse<O>> submit(final HttpClientRequest<I> request,
                                                       final Observable<ObservableConnection<HttpClientResponse<O>, HttpClientRequest<I>>> connectionObservable,
                                                       final ClientConfig config) {
        final long startTimeMillis = Clock.newStartTimeMillis();
        HttpClientConfig httpClientConfig;
        if (config instanceof HttpClientConfig) {
            httpClientConfig = (HttpClientConfig) config;
        } else {
            httpClientConfig = new HttpClientConfig(config);
        }
        boolean followRedirect = shouldFollowRedirectForRequest(httpClientConfig, request);

        enrichRequest(request, httpClientConfig);
        Observable<HttpClientResponse<O>> toReturn =
                connectionObservable.lift(new RequestProcessingOperator<I, O>(request, eventsSubject,
                                                                              httpClientConfig.getResponseSubscriptionTimeoutMs()));

        if (followRedirect) {
            toReturn = toReturn.lift(new RedirectOperator<I, O>(request, this, httpClientConfig));
        }
        return toReturn.take(1).finallyDo(new Action0() {
            @Override
            public void call() {
                eventsSubject.onEvent(HttpClientMetricsEvent.REQUEST_PROCESSING_COMPLETE,
                                      Clock.onEndMillis(startTimeMillis));
            }
        });
    }

    @Override
    protected PipelineConfigurator<HttpClientResponse<O>, HttpClientRequest<I>> adaptPipelineConfigurator(
            PipelineConfigurator<HttpClientResponse<O>, HttpClientRequest<I>> pipelineConfigurator,
            ClientConfig clientConfig, MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject) {
        PipelineConfigurator<HttpClientResponse<O>, HttpClientRequest<I>> configurator =
                new PipelineConfiguratorComposite<HttpClientResponse<O>, HttpClientRequest<I>>(pipelineConfigurator,
                                                     new ClientRequiredConfigurator<I, O>(eventsSubject));
        return super.adaptPipelineConfigurator(configurator, clientConfig, eventsSubject);
    }

    protected boolean shouldFollowRedirectForRequest(HttpClientConfig config, HttpClientRequest<I> request) {
        HttpClientConfig.RedirectsHandling redirectsHandling = config.getFollowRedirect();

        switch (redirectsHandling) {
            case Enable:
                return true;
            case Disable:
                return false;
            case Undefined:
                return request.getMethod() == HttpMethod.HEAD || request.getMethod() == HttpMethod.GET;
            default:
                return false;
        }
    }

    /*visible for testing*/ ConnectionPool<HttpClientResponse<O>, HttpClientRequest<I>> getConnectionPool() {
        return pool;
    }

    private void enrichRequest(HttpClientRequest<I> request, ClientConfig config) {

        request.setDynamicUriParts(serverInfo.getHost(), serverInfo.getPort(), false /*Set when we handle https*/);

        if(!request.getHeaders().contains(HttpHeaders.Names.HOST)) {
            request.getHeaders().add(HttpHeaders.Names.HOST, hostHeaderValue);
        }

        if (config instanceof HttpClientConfig) {
            HttpClientConfig httpClientConfig = (HttpClientConfig) config;
            if (httpClientConfig.getUserAgent() != null && request.getHeaders().get(HttpHeaders.Names.USER_AGENT) == null) {
                request.getHeaders().set(HttpHeaders.Names.USER_AGENT, httpClientConfig.getUserAgent());
            }
        }
    }

    private String prepareHostHeaderValue() {
        if (serverInfo.getPort() == 80 || serverInfo.getPort() == 443) {
            return serverInfo.getHost();
        }
        // Add port to the host header if the port is not standard port. Issue: https://github.com/ReactiveX/RxNetty/issues/258
        return serverInfo.getHost() + ':' + serverInfo.getPort();
    }
}