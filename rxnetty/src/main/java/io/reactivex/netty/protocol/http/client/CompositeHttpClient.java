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
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.client.ClientChannelFactory;
import io.reactivex.netty.client.ClientConnectionFactory;
import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.client.ConnectionPoolBuilder;
import io.reactivex.netty.metrics.MetricEventsListener;
import io.reactivex.netty.metrics.MetricEventsSubject;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import rx.Observable;
import rx.Subscription;

import java.util.concurrent.ConcurrentHashMap;

/**
 * An implementation of {@link HttpClient} that can execute requests over multiple hosts.
 * Internally this implementation uses one {@link HttpClientImpl} per unique
 * {@link io.reactivex.netty.client.RxClient.ServerInfo}.
 * The only way to create this client is via the {@link CompositeHttpClientBuilder}
 *
 * @author Nitesh Kant
 */
public class CompositeHttpClient<I, O> extends HttpClientImpl<I, O> {

    private final ConcurrentHashMap<ServerInfo, HttpClient<I, O>> httpClients;

    /**
     * This will be the unmodified configurator that can be used for all clients.
     */
    private final PipelineConfigurator<HttpClientResponse<O>, HttpClientRequest<I>> pipelineConfigurator;
    private final ConnectionPoolBuilder<HttpClientResponse<O>, HttpClientRequest<I>> poolBuilder;

    public CompositeHttpClient(String name, ServerInfo defaultServer, Bootstrap clientBootstrap,
                               PipelineConfigurator<HttpClientResponse<O>, HttpClientRequest<I>> pipelineConfigurator,
                               ClientConfig clientConfig,
                               ClientChannelFactory<HttpClientResponse<O>, HttpClientRequest<I>> channelFactory,
                               ClientConnectionFactory<HttpClientResponse<O>, HttpClientRequest<I>,
                                       ? extends ObservableConnection<HttpClientResponse<O>, HttpClientRequest<I>>> connectionFactory,
                               MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject) {
        super(name, defaultServer, clientBootstrap, pipelineConfigurator, clientConfig, channelFactory, connectionFactory,
              eventsSubject);
        httpClients = new ConcurrentHashMap<ServerInfo, HttpClient<I, O>>();
        this.pipelineConfigurator = pipelineConfigurator;
        poolBuilder = null;
        httpClients.put(defaultServer, this); // So that submit() with default serverInfo also goes to the same client as no serverinfo.
    }

    CompositeHttpClient(String name, ServerInfo defaultServer, Bootstrap clientBootstrap,
                        PipelineConfigurator<HttpClientResponse<O>, HttpClientRequest<I>> pipelineConfigurator,
                        ClientConfig clientConfig,
                        ConnectionPoolBuilder<HttpClientResponse<O>, HttpClientRequest<I>> poolBuilder,
                        MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject) {
        super(name, defaultServer, clientBootstrap, pipelineConfigurator, clientConfig, poolBuilder, eventsSubject);
        httpClients = new ConcurrentHashMap<ServerInfo, HttpClient<I, O>>();
        this.pipelineConfigurator = pipelineConfigurator;
        this.poolBuilder = poolBuilder;
        httpClients.put(defaultServer, this); // So that submit() with default serverInfo also goes to the same client as no serverinfo.
    }

    public Observable<HttpClientResponse<O>> submit(ServerInfo serverInfo, HttpClientRequest<I> request) {
        HttpClient<I, O> client = getClient(serverInfo);
        return client.submit(request);
    }

    public Observable<HttpClientResponse<O>> submit(ServerInfo serverInfo, HttpClientRequest<I> request,
                                                    HttpClientConfig config) {
        HttpClient<I, O> client = getClient(serverInfo);
        return client.submit(request, config);
    }

    private HttpClient<I, O> getClient(ServerInfo serverInfo) {
        HttpClient<I, O> client = httpClients.get(serverInfo);
        if (null == client) {
            client = newClient(serverInfo);
            HttpClient<I, O> existing = httpClients.putIfAbsent(serverInfo, client);
            if (null != existing) {
                client.shutdown();
                client = existing;
            }
        }
        return client;
    }

    @Override
    public void shutdown() {
        super.shutdown();
        for (HttpClient<I, O> client : httpClients.values()) {
            // Constructor adds 'this' as the default client; special-case it to avoid stack overflow.
            if (client != this) {
                client.shutdown();
            }
        }
    }

    public Subscription subscribe(ServerInfo server, MetricEventsListener<? extends ClientMetricsEvent<?>> listener) {
        HttpClient<I, O> client = httpClients.get(server);
        if (null == client) {
            throw new IllegalArgumentException("Invalid server: " + server.getHost() + ':' + server.getPort());
        }
        return client.subscribe(listener);
    }

    public ServerInfo getDefaultServer() {
        return serverInfo;
    }

    private HttpClientImpl<I, O> newClient(ServerInfo serverInfo) {
        if (null != poolBuilder) {
            return new HttpClientImpl<I, O>(name, serverInfo, clientBootstrap.clone(), pipelineConfigurator, clientConfig,
                                            clonePoolBuilder(serverInfo, poolBuilder), eventsSubject);
        } else {
            return new HttpClientImpl<I, O>(name, serverInfo, clientBootstrap.clone(), pipelineConfigurator, clientConfig,
                                            channelFactory, connectionFactory, eventsSubject);
        }
    }

    private ConnectionPoolBuilder<HttpClientResponse<O>, HttpClientRequest<I>> clonePoolBuilder(ServerInfo serverInfo,
                                                                                                ConnectionPoolBuilder<HttpClientResponse<O>, HttpClientRequest<I>> poolBuilder) {
        ConnectionPoolBuilder<HttpClientResponse<O>, HttpClientRequest<I>> toReturn = poolBuilder.copy(serverInfo);
        toReturn.withConnectionPoolLimitStrategy(
                ((CompositeHttpClientBuilder.CloneablePoolLimitDeterminationStrategy) poolBuilder
                        .getLimitDeterminationStrategy()).copy());
        return toReturn;
    }
}
