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
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.util.concurrent.EventExecutorGroup;
import io.reactivex.netty.channel.ConnectionHandler;
import io.reactivex.netty.channel.UnpooledConnectionFactory;
import io.reactivex.netty.metrics.MetricEventsListener;
import io.reactivex.netty.metrics.MetricEventsPublisher;
import io.reactivex.netty.metrics.MetricEventsSubject;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.pipeline.PipelineConfiguratorComposite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Subscription;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Nitesh Kant
 */
@SuppressWarnings("rawtypes")
public class AbstractServer<I, O, B extends AbstractBootstrap<B, C>, C extends Channel, S extends AbstractServer>
        implements MetricEventsPublisher<ServerMetricsEvent<?>> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractServer.class);

    protected enum ServerState {Created, Starting, Started, Shutdown}

    protected final UnpooledConnectionFactory<I,O> connectionFactory;
    protected final B bootstrap;
    protected final int port;
    protected final AtomicReference<ServerState> serverStateRef;
    protected final MetricEventsSubject<ServerMetricsEvent<?>> eventsSubject;
    protected ErrorHandler errorHandler;
    private ChannelFuture bindFuture;

    public AbstractServer(B bootstrap, int port) {
        if (null == bootstrap) {
            throw new NullPointerException("Bootstrap can not be null.");
        }
        serverStateRef = new AtomicReference<ServerState>(ServerState.Created);
        this.bootstrap = bootstrap;
        this.port = port;
        eventsSubject = new MetricEventsSubject<ServerMetricsEvent<?>>();
        connectionFactory = UnpooledConnectionFactory.from(eventsSubject, ServerChannelMetricEventProvider.INSTANCE);
    }

    public void startAndWait() {
        start();
        try {
            waitTillShutdown();
        } catch (InterruptedException e) {
            Thread.interrupted();
        }
    }

    public S start() {
        if (!serverStateRef.compareAndSet(ServerState.Created, ServerState.Starting)) {
            throw new IllegalStateException("Server already started");
        }

        try {
            bindFuture = bootstrap.bind(port).sync();
            if (!bindFuture.isSuccess()) {
                throw new RuntimeException(bindFuture.cause());
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        serverStateRef.set(ServerState.Started); // It will come here only if this was the thread that transitioned to Starting

        logger.info("Rx server started at port: " + getServerPort());

        return returnServer();
    }

    /**
     * A catch all error handler which gets invoked if any error happens during connection handling by the configured
     * {@link ConnectionHandler}.
     *
     * @param errorHandler Error handler to invoke when {@link ConnectionHandler} threw an error.
     *
     * @return This server instance.
     */
    public S withErrorHandler(ErrorHandler errorHandler) {
        if (serverStateRef.get() == ServerState.Started) {
            throw new IllegalStateException("Error handler can not be set after starting the server.");
        }
        this.errorHandler = errorHandler;
        return returnServer();
    }

    public void shutdown() throws InterruptedException {
        if (!serverStateRef.compareAndSet(ServerState.Started, ServerState.Shutdown)) {
            throw new IllegalStateException("The server is already shutdown.");
        } else {
            bindFuture.channel().close().sync();
        }
    }

    @SuppressWarnings("fallthrough")
    public void waitTillShutdown() throws InterruptedException {
        ServerState serverState = serverStateRef.get();
        switch (serverState) {
            case Created:
            case Starting:
                throw new IllegalStateException("Server not started yet.");
            case Started:
                bindFuture.channel().closeFuture().await();
                break;
            case Shutdown:
                // Nothing to do as it is already shutdown.
                break;
        }
    }

    @SuppressWarnings("fallthrough")
    public void waitTillShutdown(long duration, TimeUnit timeUnit) throws InterruptedException {
        ServerState serverState = serverStateRef.get();
        switch (serverState) {
            case Created:
            case Starting:
                throw new IllegalStateException("Server not started yet.");
            case Started:
                bindFuture.channel().closeFuture().await(duration, timeUnit);
                break;
            case Shutdown:
                // Nothing to do as it is already shutdown.
                break;
        }
    }

    public int getServerPort() {
        if (null != bindFuture && bindFuture.isDone()) {
            SocketAddress localAddress = bindFuture.channel().localAddress();
            if (localAddress instanceof InetSocketAddress) {
                return ((InetSocketAddress) localAddress).getPort();
            }
        }

        return port;
    }

    public MetricEventsSubject<ServerMetricsEvent<?>> getEventsSubject() {
        return eventsSubject;
    }

    @Override
    public Subscription subscribe(MetricEventsListener<? extends ServerMetricsEvent<?>> listener) {
        return eventsSubject.subscribe(listener);
    }

    @SuppressWarnings("unchecked")
    protected S returnServer() {
        return (S) this;
    }

    protected ChannelInitializer<Channel> newChannelInitializer(final PipelineConfigurator<I, O> pipelineConfigurator,
                                                                final ConnectionHandler<I, O> connectionHandler,
                                                                final EventExecutorGroup connHandlingExecutor) {
        return new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                ServerRequiredConfigurator<I, O> requiredConfigurator =
                        new ServerRequiredConfigurator<I, O>(connectionHandler, connectionFactory, errorHandler,
                                                             eventsSubject, connHandlingExecutor);
                PipelineConfigurator<I, O> configurator;
                if (null == pipelineConfigurator) {
                    configurator = requiredConfigurator;
                } else {
                    configurator = new PipelineConfiguratorComposite<I, O>(pipelineConfigurator, requiredConfigurator);
                }
                configurator.configureNewPipeline(ch.pipeline());
            }
        };
    }
}
