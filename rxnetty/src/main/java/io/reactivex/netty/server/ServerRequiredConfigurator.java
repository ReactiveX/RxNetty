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

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.util.concurrent.EventExecutorGroup;
import io.reactivex.netty.channel.ConnectionHandler;
import io.reactivex.netty.channel.ObservableConnectionFactory;
import io.reactivex.netty.metrics.MetricEventsSubject;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.pipeline.RxRequiredConfigurator;
import rx.Observable;

/**
 * An implementation of {@link PipelineConfigurator} which is ALWAYS added at the end of the pipeline. This
 * pipeline configurator bridges between netty's pipeline processing and Rx {@link Observable}
 *
 * @param <I> Input type for the pipeline. This is the type one writes to this pipeline.
 * @param <O> Output type of the emitted observable.  This is the type one reads from this pipeline.
 *
 * @author Nitesh Kant
 */
public class ServerRequiredConfigurator<I, O> extends RxRequiredConfigurator<I,O> {

    private final ConnectionHandler<I, O> connectionHandler;
    private final ObservableConnectionFactory<I, O> connectionFactory;
    private final ErrorHandler errorHandler;
    private final MetricEventsSubject<ServerMetricsEvent<?>> eventsSubject;

    public ServerRequiredConfigurator(final ConnectionHandler<I, O> connectionHandler,
                                      ObservableConnectionFactory<I, O> connectionFactory, ErrorHandler errorHandler,
                                      MetricEventsSubject<ServerMetricsEvent<?>> eventsSubject) {
        this(connectionHandler, connectionFactory, errorHandler, eventsSubject, null);
    }

    public ServerRequiredConfigurator(final ConnectionHandler<I, O> connectionHandler,
                                      ObservableConnectionFactory<I, O> connectionFactory, ErrorHandler errorHandler,
                                      MetricEventsSubject<ServerMetricsEvent<?>> eventsSubject,
                                      EventExecutorGroup connHandlingExecutor) {
        super(eventsSubject, ServerChannelMetricEventProvider.INSTANCE, connHandlingExecutor);
        this.connectionHandler = connectionHandler;
        this.connectionFactory = connectionFactory;
        this.errorHandler = errorHandler;
        this.eventsSubject = eventsSubject;
    }

    @Override
    protected ChannelHandler newConnectionLifecycleHandler(ChannelPipeline pipeline) {
        return new ConnectionLifecycleHandler<I, O>(connectionHandler, connectionFactory, errorHandler, eventsSubject);
    }
}
