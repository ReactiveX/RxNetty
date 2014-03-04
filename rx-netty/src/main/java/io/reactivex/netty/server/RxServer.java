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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.reactivex.netty.channel.ConnectionHandler;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.pipeline.PipelineConfiguratorComposite;
import io.reactivex.netty.pipeline.RxRequiredConfigurator;

import java.util.concurrent.atomic.AtomicReference;

public class RxServer<I, O> {

    private ChannelFuture bindFuture;
    private ErrorHandler errorHandler;

    private enum ServerState {Created, Starting, Started, Shutdown}

    private final ServerBootstrap bootstrap;
    private final int port;
    private final AtomicReference<ServerState> serverStateRef;

    public RxServer(ServerBootstrap bootstrap, int port, final ConnectionHandler<I, O> connectionHandler) {
        this(bootstrap, port, null, connectionHandler);
    }

    public RxServer(ServerBootstrap bootstrap, int port, final PipelineConfigurator<I, O> pipelineConfigurator,
                    final ConnectionHandler<I, O> connectionHandler) {
        if (null == bootstrap) {
            throw new NullPointerException("Bootstrap can not be null.");
        }
        this.bootstrap = bootstrap;
        this.port = port;
        this.bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                RxRequiredConfigurator<I, O> requiredConfigurator = new RxRequiredConfigurator<I, O>(connectionHandler,
                                                                                                     errorHandler);
                PipelineConfigurator<I, O> configurator;
                if (null == pipelineConfigurator) {
                    configurator = requiredConfigurator;
                } else {
                    configurator = new PipelineConfiguratorComposite<I, O>(pipelineConfigurator, requiredConfigurator);
                }
                configurator.configureNewPipeline(ch.pipeline());
            }
        });

        serverStateRef = new AtomicReference<ServerState>(ServerState.Created);
    }

    public void startAndWait() {
        start();
        try {
            waitTillShutdown();
        } catch (InterruptedException e) {
            Thread.interrupted();
        }
    }

    public RxServer<I, O> start() {
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
        
        return this;
    }

    /**
     * A catch all error handler which gets invoked if any error happens during connection handling by the configured
     * {@link ConnectionHandler}.
     *
     * @param errorHandler Error handler to invoke when {@link ConnectionHandler} threw an error.
     *
     * @return This server instance.
     */
    public RxServer<I, O> withErrorHandler(ErrorHandler errorHandler) {
        if (serverStateRef.get() == ServerState.Started) {
            throw new IllegalStateException("Error handler can not be set after starting the server.");
        }
        this.errorHandler = errorHandler;
        return this;
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
}
