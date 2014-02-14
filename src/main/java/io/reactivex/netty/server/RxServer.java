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
import io.reactivex.netty.ConnectionHandler;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.pipeline.PipelineConfiguratorComposite;
import io.reactivex.netty.pipeline.RxRequiredConfigurator;

import java.util.concurrent.atomic.AtomicReference;

public class RxServer<I, O> {

    private ChannelFuture bindFuture;

    private enum ServerState {Created, Starting, Started, Shutdown}

    private final ServerBootstrap bootstrap;
    private final int port;
    private final AtomicReference<ServerState> serverStateRef;

    public RxServer(ServerBootstrap bootstrap, int port, final PipelineConfigurator<I, O> pipelineConfigurator,
                    final ConnectionHandler<I, O> connectionHandler) {
        this.bootstrap = bootstrap;
        this.port = port;

        this.bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                RxRequiredConfigurator<I, O> requiredConfigurator = new RxRequiredConfigurator<I, O>(connectionHandler);
                PipelineConfigurator<O, I> configurator = new PipelineConfiguratorComposite<O, I>(pipelineConfigurator,
                                                                                                  requiredConfigurator);
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

    public void start() {
        if (!serverStateRef.compareAndSet(ServerState.Created, ServerState.Starting)) {
            throw new IllegalStateException("Server already started");
        }

        bindFuture = bootstrap.bind(port);

        serverStateRef.set(ServerState.Started); // It will come here only if this was the thread that transitioned to Starting
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
                bindFuture.sync();
                bindFuture.channel().closeFuture().await();
                break;
            case Shutdown:
                // Nothing to do as it is already shutdown.
                break;
        }
    }
}
