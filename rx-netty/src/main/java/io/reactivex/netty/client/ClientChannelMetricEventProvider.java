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

import io.reactivex.netty.channel.ChannelMetricEventProvider;
import io.reactivex.netty.metrics.MetricsEvent;

/**
 * @author Nitesh Kant
 */
public class ClientChannelMetricEventProvider extends ChannelMetricEventProvider {

    public static final ClientChannelMetricEventProvider INSTANCE = new ClientChannelMetricEventProvider();

    @Override
    public MetricsEvent<?> getChannelCloseStartEvent() {
        return ClientMetricsEvent.CONNECTION_CLOSE_START;
    }

    @Override
    public MetricsEvent<?> getChannelCloseSuccessEvent() {
        return ClientMetricsEvent.CONNECTION_CLOSE_SUCCESS;
    }

    @Override
    public MetricsEvent<?> getChannelCloseFailedEvent() {
        return ClientMetricsEvent.CONNECTION_CLOSE_FAILED;
    }

    @Override
    public MetricsEvent<?> getWriteStartEvent() {
        return ClientMetricsEvent.WRITE_START;
    }

    @Override
    public MetricsEvent<?> getWriteSuccessEvent() {
        return ClientMetricsEvent.WRITE_SUCCESS;
    }

    @Override
    public MetricsEvent<?> getWriteFailedEvent() {
        return ClientMetricsEvent.WRITE_FAILED;
    }

    @Override
    public MetricsEvent<?> getFlushStartEvent() {
        return ClientMetricsEvent.FLUSH_START;
    }

    @Override
    public MetricsEvent<?> getFlushSuccessEvent() {
        return ClientMetricsEvent.FLUSH_SUCCESS;
    }

    @Override
    public MetricsEvent<?> getFlushFailedEvent() {
        return ClientMetricsEvent.FLUSH_FAILED;
    }

    @Override
    public MetricsEvent<?> getBytesReadEvent() {
        return ClientMetricsEvent.BYTES_READ;
    }
}
