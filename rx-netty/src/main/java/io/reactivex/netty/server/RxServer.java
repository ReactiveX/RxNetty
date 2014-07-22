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
import io.netty.channel.ServerChannel;
import io.netty.util.concurrent.EventExecutorGroup;
import io.reactivex.netty.channel.ConnectionHandler;
import io.reactivex.netty.pipeline.PipelineConfigurator;

public class RxServer<I, O> extends AbstractServer<I, O, ServerBootstrap, ServerChannel, RxServer<I, O>> {

    protected final PipelineConfigurator<I, O> pipelineConfigurator;

    public RxServer(ServerBootstrap bootstrap, int port, final ConnectionHandler<I, O> connectionHandler) {
        this(bootstrap, port, null, connectionHandler);
    }

    public RxServer(ServerBootstrap bootstrap, int port, final PipelineConfigurator<I, O> pipelineConfigurator,
                    final ConnectionHandler<I, O> connectionHandler) {
        this(bootstrap, port, pipelineConfigurator, connectionHandler, null);
    }

    public RxServer(ServerBootstrap bootstrap, int port, final ConnectionHandler<I, O> connectionHandler,
                    EventExecutorGroup connHandlingExecutor) {
        this(bootstrap, port, null, connectionHandler, connHandlingExecutor);
    }

    public RxServer(ServerBootstrap bootstrap, int port, final PipelineConfigurator<I, O> pipelineConfigurator,
                    final ConnectionHandler<I, O> connectionHandler, EventExecutorGroup connHandlingExecutor) {
        super(bootstrap, port);
        this.pipelineConfigurator = pipelineConfigurator;
        bootstrap.childHandler(newChannelInitializer(pipelineConfigurator, connectionHandler, connHandlingExecutor));
    }
}
