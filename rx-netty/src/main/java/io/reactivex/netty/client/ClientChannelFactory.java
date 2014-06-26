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
package io.reactivex.netty.client;

import io.netty.channel.ChannelFuture;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.metrics.MetricEventsSubject;
import rx.Subscriber;

/**
 *
 * @param <I> The type of the object that is read from the channel created by this factory.
 * @param <O> The type of objects that are written to the channel created by this factory.
 *
 * @author Nitesh Kant
 */
public interface ClientChannelFactory<I, O> {

    ChannelFuture connect(Subscriber<? super ObservableConnection<I, O>> subscriber, RxClient.ServerInfo serverInfo,
                          ClientConnectionFactory<I, O, ? extends ObservableConnection<I, O>> connectionFactory);

    void onNewConnection(ObservableConnection<I, O> newConnection,
                         Subscriber<? super ObservableConnection<I, O>> subscriber);

    void useMetricEventsSubject(MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject);
}
