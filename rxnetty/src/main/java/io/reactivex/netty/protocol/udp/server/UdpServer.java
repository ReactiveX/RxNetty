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

package io.reactivex.netty.protocol.udp.server;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.util.concurrent.EventExecutorGroup;
import io.reactivex.netty.channel.ConnectionHandler;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.server.AbstractServer;

/**
 * A UDP/IP server.
 *
 * @author Nitesh Kant
 */
public class UdpServer<I, O> extends AbstractServer<I, O, Bootstrap, Channel, UdpServer<I, O>> {

    public UdpServer(Bootstrap bootstrap, int port, final ConnectionHandler<I, O> connectionHandler) {
        this(bootstrap, port, connectionHandler, null);
    }

    public UdpServer(Bootstrap bootstrap, int port, final ConnectionHandler<I, O> connectionHandler,
                     EventExecutorGroup connHandlingExecutor) {
        this(bootstrap, port, null, connectionHandler, connHandlingExecutor);
    }

    public UdpServer(Bootstrap bootstrap, int port, final PipelineConfigurator<I, O> pipelineConfigurator,
                    final ConnectionHandler<I, O> connectionHandler) {
        this(bootstrap, port, pipelineConfigurator, connectionHandler, null);
    }

    public UdpServer(Bootstrap bootstrap, int port, final PipelineConfigurator<I, O> pipelineConfigurator,
                    final ConnectionHandler<I, O> connectionHandler, EventExecutorGroup connHandlingExecutor) {
        super(bootstrap, port);
        bootstrap.handler(newChannelInitializer(pipelineConfigurator, connectionHandler, connHandlingExecutor));
    }
}
