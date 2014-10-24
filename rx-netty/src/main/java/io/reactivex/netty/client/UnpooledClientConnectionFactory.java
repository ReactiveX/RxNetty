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

import io.netty.channel.Channel;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.channel.UnpooledConnectionFactory;
import io.reactivex.netty.metrics.MetricEventsSubject;

/**
 * @author Nitesh Kant
 */
public class UnpooledClientConnectionFactory<I, O> implements ClientConnectionFactory<I, O, ObservableConnection<I, O>> {

    private final UnpooledConnectionFactory<I, O> delegate;

    public UnpooledClientConnectionFactory() {
        delegate = new UnpooledConnectionFactory<I, O>(ClientChannelMetricEventProvider.INSTANCE);
    }

    @Override
    public ObservableConnection<I, O> newConnection(Channel channel) {
        return delegate.newConnection(channel);
    }

    @Override
    public void useMetricEventsSubject(MetricEventsSubject<?> eventsSubject) {
        delegate.useMetricEventsSubject(eventsSubject);
    }
}
