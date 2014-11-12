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

import io.netty.bootstrap.AbstractBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.EventExecutorGroup;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.ConnectionHandler;
import io.reactivex.netty.metrics.MetricEventsListener;
import io.reactivex.netty.metrics.MetricEventsListenerFactory;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.pipeline.ssl.SSLEngineFactory;

/**
 * @author Nitesh Kant
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractServerBuilder<I, O, T extends AbstractBootstrap<T, C>, C extends Channel,
        B extends AbstractServerBuilder, S extends AbstractServer<I, O, T, C, S>> {

    protected final T serverBootstrap;
    protected PipelineConfigurator<I, O> pipelineConfigurator;
    protected Class<? extends C> serverChannelClass;
    protected final ConnectionHandler<I, O> connectionHandler;
    protected final int port;
    protected LogLevel wireLogginLevel;
    protected MetricEventsListenerFactory eventListenersFactory;
    protected EventExecutorGroup eventExecutorGroup;
    protected SSLEngineFactory sslEngineFactory;

    protected AbstractServerBuilder(int port, T bootstrap, ConnectionHandler<I, O> connectionHandler) {
        if (null == connectionHandler) {
            throw new IllegalArgumentException("Connection handler can not be null");
        }
        if (null == bootstrap) {
            throw new IllegalArgumentException("Server bootstrap can not be null");
        }
        this.port = port;
        serverBootstrap = bootstrap;
        this.connectionHandler = connectionHandler;
        defaultChannelOptions();
    }

    public B eventLoop(EventLoopGroup singleGroup) {
        serverBootstrap.group(singleGroup);
        return returnBuilder();
    }

    public B channel(Class<? extends C> serverChannelClass) {
        this.serverChannelClass = serverChannelClass;
        return returnBuilder();
    }

    public <P> B channelOption(ChannelOption<P> option, P value) {
        serverBootstrap.option(option, value);
        return returnBuilder();
    }

    public B pipelineConfigurator(PipelineConfigurator<I, O> pipelineConfigurator) {
        this.pipelineConfigurator = pipelineConfigurator;
        return returnBuilder();
    }

    public B appendPipelineConfigurator(PipelineConfigurator<I, O> additionalConfigurator) {
        return pipelineConfigurator(PipelineConfigurators.composeConfigurators(pipelineConfigurator,
                                                                               additionalConfigurator));
    }

    public B withSslEngineFactory(SSLEngineFactory sslEngineFactory) {
        this.sslEngineFactory = sslEngineFactory;
        return returnBuilder();
    }

    /**
     * Enables wire level logs (all events received by netty) to be logged at the passed {@code wireLogginLevel}. <br/>
     *
     * Since, in most of the production systems, the logging level is set to {@link LogLevel#WARN} or
     * {@link LogLevel#ERROR}, if this wire level logging is required for all requests (not at all recommended as this
     * logging is very verbose), the passed level must be {@link LogLevel#WARN} or {@link LogLevel#ERROR} respectively. <br/>
     *
     * It is recommended to set this level to {@link LogLevel#DEBUG} and then dynamically enabled disable this log level
     * whenever required. <br/>
     *
     * @param wireLogginLevel Log level at which the wire level logs will be logged.
     *
     * @return This builder.
     *
     * @see LoggingHandler
     */
    public B enableWireLogging(LogLevel wireLogginLevel) {
        this.wireLogginLevel = wireLogginLevel;
        return returnBuilder();
    }

    public B defaultChannelOptions() {
        channelOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        return returnBuilder();
    }

    public B withMetricEventsListenerFactory(MetricEventsListenerFactory eventListenersFactory) {
        this.eventListenersFactory = eventListenersFactory;
        return returnBuilder();
    }

    /**
     * If the passed executor is not {@code null} , the configured {@link ConnectionHandler} will be invoked in
     * the passed {@link EventExecutorGroup}
     *
     * @param eventExecutorGroup The {@link EventExecutorGroup} in which to invoke the configured
     *                           {@link ConnectionHandler}. Can be {@code null}, in which case, the
     *                           {@link ConnectionHandler} is invoked in the channel's eventloop.
     *
     * @return This builder.
     */
    public B withEventExecutorGroup(EventExecutorGroup eventExecutorGroup) {
        this.eventExecutorGroup = eventExecutorGroup;
        return returnBuilder();
    }

    public PipelineConfigurator<I, O> getPipelineConfigurator() {
        return pipelineConfigurator;
    }

    public S build() {
        if (null == serverChannelClass) {
            serverChannelClass = defaultServerChannelClass();
            EventLoopGroup acceptorGroup = serverBootstrap.group();
            if (null == acceptorGroup) {
                configureDefaultEventloopGroup();
            }
        }

        if (null == serverBootstrap.group()) {
            if (defaultServerChannelClass() == serverChannelClass) {
                configureDefaultEventloopGroup();
            } else {
                // Fail fast for defaults we do not support.
                throw new IllegalStateException("Specified a channel class but not the event loop group.");
            }
        }

        serverBootstrap.channel(serverChannelClass);
        if (null != wireLogginLevel) {
            pipelineConfigurator = PipelineConfigurators.appendLoggingConfigurator(pipelineConfigurator,
                                                                                   wireLogginLevel);
        }
        if(null != sslEngineFactory) {
            appendPipelineConfigurator(PipelineConfigurators.<I, O>sslConfigurator(sslEngineFactory));
        }
        S server = createServer();
        if (null != eventListenersFactory) {
            MetricEventsListener<? extends ServerMetricsEvent<?>> listener = newMetricsListener(eventListenersFactory,
                                                                                             server);
            server.subscribe(listener);
        }
        return server;
    }

    protected void configureDefaultEventloopGroup() {
        serverBootstrap.group(RxNetty.getRxEventLoopProvider().globalServerEventLoop());
    }

    protected abstract Class<? extends C> defaultServerChannelClass();

    protected abstract S createServer();

    protected abstract MetricEventsListener<? extends ServerMetricsEvent<? extends Enum>>
    newMetricsListener(MetricEventsListenerFactory factory, S server);

    @SuppressWarnings("unchecked")
    protected B returnBuilder() {
        return (B) this;
    }
}
