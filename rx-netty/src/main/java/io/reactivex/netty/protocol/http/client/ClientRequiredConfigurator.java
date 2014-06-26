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

import io.netty.channel.ChannelPipeline;
import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.metrics.MetricEventsSubject;
import io.reactivex.netty.pipeline.PipelineConfigurator;

/**
 *
 * @param <I> The type of the content of request.
 * @param <O> The type of the content of response.
 *
 * @author Nitesh Kant
 */
class ClientRequiredConfigurator<I, O> implements PipelineConfigurator<HttpClientResponse<O>, HttpClientRequest<I>> {

    private final MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject;

    public ClientRequiredConfigurator(MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject) {
        this.eventsSubject = eventsSubject;
    }

    @Override
    public void configureNewPipeline(ChannelPipeline pipeline) {
        ClientRequestResponseConverter converter = pipeline.get(ClientRequestResponseConverter.class);
        if (null == converter) {
            pipeline.addLast(HttpClientPipelineConfigurator.REQUEST_RESPONSE_CONVERTER_HANDLER_NAME,
                             new ClientRequestResponseConverter(eventsSubject));
        }
    }
}
