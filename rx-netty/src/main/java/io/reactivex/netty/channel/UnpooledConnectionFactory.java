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
package io.reactivex.netty.channel;

import io.netty.channel.Channel;
import io.reactivex.netty.metrics.MetricEventsSubject;

/**
 * A factory to create {@link ObservableConnection}s
 *
 * @author Nitesh Kant
 */
public class UnpooledConnectionFactory<I, O> implements ObservableConnectionFactory<I, O> {

    private final ChannelMetricEventProvider metricEventProvider;
    @SuppressWarnings("rawtypes") private MetricEventsSubject eventsSubject;

    public UnpooledConnectionFactory(ChannelMetricEventProvider metricEventProvider) {
        this.metricEventProvider = metricEventProvider;
    }

    @Override
    public ObservableConnection<I, O> newConnection(Channel channel) {
        return ObservableConnection.create(channel, eventsSubject, metricEventProvider);
    }

    @Override
    public void useMetricEventsSubject(MetricEventsSubject<?> eventsSubject) {
        this.eventsSubject = eventsSubject;
    }

    public static <I, O> UnpooledConnectionFactory<I, O> from(MetricEventsSubject<?> eventsSubject,
                                                              ChannelMetricEventProvider metricEventProvider) {
        UnpooledConnectionFactory<I, O> factory = new UnpooledConnectionFactory<I, O>(metricEventProvider);
        factory.useMetricEventsSubject(eventsSubject);
        return factory;
    }
}
