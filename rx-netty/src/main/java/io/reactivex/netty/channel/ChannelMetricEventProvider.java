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

import io.reactivex.netty.metrics.MetricsEvent;

/**
 * An {@link ObservableConnection} and the underlying netty channel is the same irrespective of whether it is used by
 * the client or the server. However, the metric events are specific to clients and servers, so, this class makes it
 * possible to use the same ObservableConnection both with client and server.
 *
 * <h2>Event types</h2>
 * Since our event model does not have a common type, this class also does not check for type safety but assumes that
 * the provider is providing the correct event types (matching with the attached publisher)
 *
 * @author Nitesh Kant
 */
public abstract class ChannelMetricEventProvider {

    public abstract MetricsEvent<?> getChannelCloseStartEvent();

    public abstract MetricsEvent<?> getChannelCloseSuccessEvent();

    public abstract MetricsEvent<?> getChannelCloseFailedEvent();

    public abstract MetricsEvent<?> getWriteStartEvent();

    public abstract MetricsEvent<?> getWriteSuccessEvent();

    public abstract MetricsEvent<?> getWriteFailedEvent();

    public abstract MetricsEvent<?> getFlushStartEvent();

    public abstract MetricsEvent<?> getFlushSuccessEvent();

    public abstract MetricsEvent<?> getFlushFailedEvent();

    public abstract MetricsEvent<?> getBytesReadEvent();
}
