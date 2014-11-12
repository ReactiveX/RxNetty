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

package io.reactivex.netty.protocol.http.server;

import io.netty.channel.ChannelPipeline;
import io.netty.util.concurrent.EventExecutorGroup;
import io.reactivex.netty.metrics.MetricEventsSubject;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.server.ServerMetricsEvent;

/**
 *
 * @param <I> The type of the content of request.
 * @param <O> The type of the content of response.
 *
 * @author Nitesh Kant
 */
class ServerRequiredConfigurator<I, O> implements PipelineConfigurator<HttpServerRequest<I>, HttpServerResponse<O>> {

    public static final String REQUEST_RESPONSE_CONVERTER_HANDLER_NAME = "request-response-converter";
    private final EventExecutorGroup handlersExecutorGroup;
    private final long requestContentSubscriptionTimeoutMs;
    private MetricEventsSubject<ServerMetricsEvent<?>> eventsSubject;

    ServerRequiredConfigurator(EventExecutorGroup handlersExecutorGroup, long requestContentSubscriptionTimeoutMs) {
        this.handlersExecutorGroup = handlersExecutorGroup;
        this.requestContentSubscriptionTimeoutMs = requestContentSubscriptionTimeoutMs;
    }

    void useMetricEventsSubject(MetricEventsSubject<ServerMetricsEvent<?>> eventsSubject) {
        this.eventsSubject = eventsSubject;
    }

    @Override
    public void configureNewPipeline(ChannelPipeline pipeline) {
        pipeline.addLast(getRequestResponseConverterExecutor(), REQUEST_RESPONSE_CONVERTER_HANDLER_NAME,
                         new ServerRequestResponseConverter(eventsSubject, requestContentSubscriptionTimeoutMs));
    }

    protected EventExecutorGroup getRequestResponseConverterExecutor() {
        return handlersExecutorGroup;
    }
}
