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

import io.reactivex.netty.metrics.MetricEventsListener;
import io.reactivex.netty.metrics.MetricEventsSubject;
import io.reactivex.netty.metrics.MetricsEvent;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

/**
 * An implementation of {@link ChannelMetricEventProvider} that does not do anything. It is imperative that this
 * provider is always used with {@link NoOpMetricEventsSubject} to make sure that none
 * of the events generated ever reach the listeners. Failure to do so will result in runtime type mismatched for the
 * listeners.
 *
 * @author Nitesh Kant
 */
public class NoOpChannelMetricEventProvider extends ChannelMetricEventProvider {

    public static final NoOpChannelMetricEventProvider INSTANCE = new NoOpChannelMetricEventProvider();

    public static final MetricsEvent<?> NO_OP_EVENT = new NoOpMetricsEvent();

    @Override
    public MetricsEvent<?> getChannelCloseStartEvent() {
        return NO_OP_EVENT;
    }

    @Override
    public MetricsEvent<?> getChannelCloseSuccessEvent() {
        return NO_OP_EVENT;
    }

    @Override
    public MetricsEvent<?> getChannelCloseFailedEvent() {
        return NO_OP_EVENT;
    }

    @Override
    public MetricsEvent<?> getWriteStartEvent() {
        return NO_OP_EVENT;
    }

    @Override
    public MetricsEvent<?> getWriteSuccessEvent() {
        return NO_OP_EVENT;
    }

    @Override
    public MetricsEvent<?> getWriteFailedEvent() {
        return NO_OP_EVENT;
    }

    @Override
    public MetricsEvent<?> getFlushStartEvent() {
        return NO_OP_EVENT;
    }

    @Override
    public MetricsEvent<?> getFlushSuccessEvent() {
        return NO_OP_EVENT;
    }

    @Override
    public MetricsEvent<?> getFlushFailedEvent() {
        return NO_OP_EVENT;
    }

    @Override
    public MetricsEvent<?> getBytesReadEvent() {
        return NO_OP_EVENT;
    }

    private static class NoOpMetricsEvent implements MetricsEvent<Enum<NoOpMetricsEvent.NoOp>> {

        public enum NoOp {None}

        @Override
        public NoOp getType() {
            return NoOp.None;
        }

        @Override
        public boolean isTimed() {
            return false;
        }

        @Override
        public boolean isError() {
            return false;
        }
    }

    @SuppressWarnings("rawtypes")
    public final static class NoOpMetricEventsSubject extends MetricEventsSubject {

        public static final NoOpMetricEventsSubject INSTANCE = new NoOpMetricEventsSubject();

        @Override
        public Subscription subscribe(MetricEventsListener listener) {
            return Subscriptions.empty(); // Don't add the listener to the underlying subject and hence avoid any callbacks.
        }
    }
}
