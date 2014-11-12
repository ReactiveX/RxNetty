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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import io.reactivex.netty.channel.RxDefaultThreadFactory;
import io.reactivex.netty.metrics.MetricEventsListener;
import io.reactivex.netty.metrics.MetricEventsListenerFactory;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.server.ConnectionBasedServerBuilder;
import io.reactivex.netty.server.RxServer;
import io.reactivex.netty.server.ServerMetricsEvent;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * A convenience builder to create instances of {@link HttpServer}
 *
 * @author Nitesh Kant
 */
public class HttpServerBuilder<I, O>
        extends ConnectionBasedServerBuilder<HttpServerRequest<I>, HttpServerResponse<O>, HttpServerBuilder<I, O>> {

    private long contentSubscriptionTimeoutMs;

    public HttpServerBuilder(int port, RequestHandler<I, O> requestHandler, boolean send10ResponseFor10Request) {
        super(port, new HttpConnectionHandler<I, O>(requestHandler, send10ResponseFor10Request));
        pipelineConfigurator(PipelineConfigurators.<I, O>httpServerConfigurator());
    }

    public HttpServerBuilder(ServerBootstrap bootstrap, int port, RequestHandler<I, O> requestHandler) {
        super(port, new HttpConnectionHandler<I, O>(requestHandler), bootstrap);
        pipelineConfigurator(PipelineConfigurators.<I, O>httpServerConfigurator());
    }

    public HttpServerBuilder(int port, RequestHandler<I, O> requestHandler) {
        super(port, new HttpConnectionHandler<I, O>(requestHandler));
        pipelineConfigurator(PipelineConfigurators.<I, O>httpServerConfigurator());
    }

    public HttpServerBuilder(ServerBootstrap bootstrap, int port, RequestHandler<I, O> requestHandler,
                             boolean send10ResponseFor10Request) {
        super(port, new HttpConnectionHandler<I, O>(requestHandler, send10ResponseFor10Request), bootstrap);
        pipelineConfigurator(PipelineConfigurators.<I, O>httpServerConfigurator());
    }

    /**
     * If the passed executor is not {@code null} , the configured {@link RequestHandler} will be invoked in the passed
     * {@link EventExecutorGroup}
     *
     * @param eventExecutorGroup The {@link EventExecutorGroup} in which to invoke the configured
     *                           {@link RequestHandler}. Can be {@code null}, in which case, the
     *                           {@link RequestHandler} is invoked in the channel's eventloop.
     *
     * @return This builder.
     */
    @Override
    public HttpServerBuilder<I, O> withEventExecutorGroup(EventExecutorGroup eventExecutorGroup) {
        return super.withEventExecutorGroup(eventExecutorGroup);
    }

    /**
     * Same as calling {@link #withRequestProcessingThreads(int, ThreadFactory)} with {@link RxDefaultThreadFactory}
     *
     * @param threadCount Number of threads to use for request processing.
     *
     * @return This builder.
     */
    public HttpServerBuilder<I, O> withRequestProcessingThreads(int threadCount) {
        return super.withEventExecutorGroup(new DefaultEventExecutorGroup(threadCount, new RxDefaultThreadFactory("rx-request-processor")));
    }

    /**
     * Same as calling {@link #withEventExecutorGroup(EventExecutorGroup)} with {@link DefaultEventExecutorGroup} using
     * the passed {@code factory}
     *
     * @param threadCount Number of threads to use for request processing.
     * @param factory Thread factory to use for the {@link DefaultEventExecutorGroup}
     *
     * @return This builder.
     */
    public HttpServerBuilder<I, O> withRequestProcessingThreads(int threadCount, ThreadFactory factory) {
        return super.withEventExecutorGroup(new DefaultEventExecutorGroup(threadCount, factory));
    }

    /**
     * {@link HttpServerRequest} does not allow unlimited delayed content subscriptions.
     * This method specifies the timeout for the subscription of the content.
     *
     * @param subscriptionTimeout Timeout value.
     * @param timeunit Timeout time unit.
     *
     * @return This builder.
     */
    public HttpServerBuilder<I, O> withRequestContentSubscriptionTimeout(long subscriptionTimeout, TimeUnit timeunit) {
        contentSubscriptionTimeoutMs = TimeUnit.MILLISECONDS.convert(subscriptionTimeout, timeunit);
        return this;
    }

    @Override
    public HttpServer<I, O> build() {
        return (HttpServer<I, O>) super.build();
    }

    @Override
    protected HttpServer<I, O> createServer() {
        return new HttpServer<I, O>(serverBootstrap, port, pipelineConfigurator,
                                    (HttpConnectionHandler<I, O>) connectionHandler, eventExecutorGroup,
                                    contentSubscriptionTimeoutMs);
    }

    @Override
    protected MetricEventsListener<ServerMetricsEvent<?>>
    newMetricsListener(MetricEventsListenerFactory factory, RxServer<HttpServerRequest<I>, HttpServerResponse<O>> server) {
        return factory.forHttpServer((HttpServer<I, O>) server);
    }
}
