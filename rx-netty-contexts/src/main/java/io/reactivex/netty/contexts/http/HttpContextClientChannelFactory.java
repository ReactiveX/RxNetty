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

package io.reactivex.netty.contexts.http;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.client.ClientChannelFactoryImpl;
import io.reactivex.netty.client.ClientConnectionFactory;
import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.client.RxClient;
import io.reactivex.netty.contexts.ContextsContainer;
import io.reactivex.netty.contexts.RequestCorrelator;
import io.reactivex.netty.metrics.MetricEventsSubject;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import rx.Subscriber;

import static io.reactivex.netty.contexts.AbstractClientContextHandler.NewContextEvent;

/**
 * @author Nitesh Kant
 */
public class HttpContextClientChannelFactory<I, O> extends
        ClientChannelFactoryImpl<HttpClientResponse<O>, HttpClientRequest<I>> {

    private final RequestCorrelator correlator;

    public HttpContextClientChannelFactory(Bootstrap clientBootstrap, RequestCorrelator correlator,
                                           MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject) {
        super(clientBootstrap, eventsSubject);
        this.correlator = correlator;
    }

    @Override
    public ChannelFuture connect(
            Subscriber<? super ObservableConnection<HttpClientResponse<O>, HttpClientRequest<I>>> subscriber,
            RxClient.ServerInfo serverInfo,
            ClientConnectionFactory<HttpClientResponse<O>, HttpClientRequest<I>,
                    ? extends ObservableConnection<HttpClientResponse<O>, HttpClientRequest<I>>> connectionFactory) {
        final ContextCapturingSubscriber capturingSubscriber = new ContextCapturingSubscriber(subscriber);
        return super.connect(capturingSubscriber, serverInfo, connectionFactory);
    }

    @Override
    public void onNewConnection(ObservableConnection<HttpClientResponse<O>, HttpClientRequest<I>> newConnection,
                                Subscriber<? super ObservableConnection<HttpClientResponse<O>, HttpClientRequest<I>>> subscriber) {
        /*
         * This will either be called after a call to connect() or directly (from pool).
         * If it is the former then the subscriber should already be a capturing sub. In case of latter, we should be
         * called from the thread that has the relevant state & hence we capture the context now.
         */
        final Subscriber<? super ObservableConnection<HttpClientResponse<O>, HttpClientRequest<I>>> subToUse;
        if (ContextCapturingSubscriber.class == subscriber.getClass()) {
            subToUse = subscriber;
        } else {
            subToUse = new ContextCapturingSubscriber(subscriber);
        }

        super.onNewConnection(newConnection, subToUse);
    }

    private class ContextCapturingSubscriber extends Subscriber<ObservableConnection<HttpClientResponse<O>, HttpClientRequest<I>>> {

        private final Subscriber<? super ObservableConnection<HttpClientResponse<O>, HttpClientRequest<I>>> original;
        private final String requestId;
        private final ContextsContainer container;

        private ContextCapturingSubscriber(Subscriber<? super ObservableConnection<HttpClientResponse<O>, HttpClientRequest<I>>> original) {
            super(original);
            this.original = original;
            requestId = correlator.getRequestIdForClientRequest();
            container = correlator.getContextForClientRequest(requestId);
        }

        @Override
        public void onCompleted() {
            original.onCompleted();
        }

        @Override
        public void onError(Throwable e) {
            original.onError(e);
        }

        @Override
        public void onNext(ObservableConnection<HttpClientResponse<O>, HttpClientRequest<I>> connection) {
            if (null != requestId && null != container) {
                connection.getChannel().pipeline()
                          .fireUserEventTriggered(new NewContextEvent(requestId, container));
            }
            original.onNext(connection);
        }
    }
}
