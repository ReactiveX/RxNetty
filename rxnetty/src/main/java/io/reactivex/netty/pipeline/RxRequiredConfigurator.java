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

package io.reactivex.netty.pipeline;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.util.concurrent.EventExecutorGroup;
import io.reactivex.netty.channel.ChannelMetricEventProvider;
import io.reactivex.netty.metrics.BytesInspector;
import io.reactivex.netty.metrics.MetricEventsSubject;
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
public abstract class RxRequiredConfigurator<I, O> implements PipelineConfigurator<I, O> {

    public static final String CONN_LIFECYCLE_HANDLER_NAME = "conn_lifecycle_handler";
    public static final String BYTES_INSPECTOR_HANDLER_NAME = "bytes_inspector";
    public static final String NETTY_OBSERVABLE_ADAPTER_NAME = "netty_observable_adapter";

    private final BytesInspector bytesInspector;
    private final EventExecutorGroup handlersExecutorGroup;

    protected RxRequiredConfigurator(@SuppressWarnings("rawtypes") MetricEventsSubject eventsSubject,
                                     ChannelMetricEventProvider metricEventProvider) {
        this(eventsSubject, metricEventProvider, null);
    }

    /**
     *
     * @param eventsSubject Metrics event subject.
     * @param metricEventProvider Metrics event provider.
     * @param handlersExecutorGroup The {@link EventExecutorGroup} to be used for all the handlers added by this
     *                              configurator. This can be {@code null} , in which case the eventloop for the
     *                              underlying channel is used.
     */
    protected RxRequiredConfigurator(@SuppressWarnings("rawtypes") MetricEventsSubject eventsSubject,
                                     ChannelMetricEventProvider metricEventProvider,
                                     EventExecutorGroup handlersExecutorGroup) {
        bytesInspector = new BytesInspector(eventsSubject, metricEventProvider);
        this.handlersExecutorGroup = handlersExecutorGroup;
    }

    @Override
    public void configureNewPipeline(ChannelPipeline pipeline) {

        /**
         * This method is called for each new connection & the following two channel handlers are not shareable, so
         * we need to create a new instance every time.
         */
        ChannelHandler lifecycleHandler = newConnectionLifecycleHandler(pipeline);
        ObservableAdapter observableAdapter = new ObservableAdapter();

        pipeline.addFirst(BYTES_INSPECTOR_HANDLER_NAME, bytesInspector);
        pipeline.addLast(getConnectionLifecycleHandlerExecutor(), CONN_LIFECYCLE_HANDLER_NAME, lifecycleHandler);
        pipeline.addLast(getObservableAdapterExecutor(), NETTY_OBSERVABLE_ADAPTER_NAME, observableAdapter);
    }

    protected EventExecutorGroup getConnectionLifecycleHandlerExecutor() {
        return handlersExecutorGroup;
    }

    protected EventExecutorGroup getObservableAdapterExecutor() {
        return handlersExecutorGroup;
    }

    protected abstract ChannelHandler newConnectionLifecycleHandler(ChannelPipeline pipeline);
}
