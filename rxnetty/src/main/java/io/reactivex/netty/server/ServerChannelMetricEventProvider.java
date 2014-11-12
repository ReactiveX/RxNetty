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
package io.reactivex.netty.server;

import io.reactivex.netty.channel.ChannelMetricEventProvider;
import io.reactivex.netty.metrics.MetricsEvent;

/**
 * @author Nitesh Kant
 */
public class ServerChannelMetricEventProvider extends ChannelMetricEventProvider {

    public static final ServerChannelMetricEventProvider INSTANCE = new ServerChannelMetricEventProvider();

    @Override
    public MetricsEvent<?> getChannelCloseStartEvent() {
        return ServerMetricsEvent.CONNECTION_CLOSE_START;
    }

    @Override
    public MetricsEvent<?> getChannelCloseSuccessEvent() {
        return ServerMetricsEvent.CONNECTION_CLOSE_SUCCESS;
    }

    @Override
    public MetricsEvent<?> getChannelCloseFailedEvent() {
        return ServerMetricsEvent.CONNECTION_CLOSE_FAILED;
    }

    @Override
    public MetricsEvent<?> getWriteStartEvent() {
        return ServerMetricsEvent.WRITE_START;
    }

    @Override
    public MetricsEvent<?> getWriteSuccessEvent() {
        return ServerMetricsEvent.WRITE_SUCCESS;
    }

    @Override
    public MetricsEvent<?> getWriteFailedEvent() {
        return ServerMetricsEvent.WRITE_FAILED;
    }

    @Override
    public MetricsEvent<?> getFlushStartEvent() {
        return ServerMetricsEvent.FLUSH_START;
    }

    @Override
    public MetricsEvent<?> getFlushSuccessEvent() {
        return ServerMetricsEvent.FLUSH_SUCCESS;
    }

    @Override
    public MetricsEvent<?> getFlushFailedEvent() {
        return ServerMetricsEvent.FLUSH_FAILED;
    }

    @Override
    public MetricsEvent<?> getBytesReadEvent() {
        return ServerMetricsEvent.BYTES_READ;
    }
}
