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

package io.reactivex.netty.channel;

import io.netty.channel.EventLoopGroup;
import io.reactivex.netty.client.ClientBuilder;
import io.reactivex.netty.client.RxClient;
import io.reactivex.netty.server.RxServer;
import io.reactivex.netty.server.ServerBuilder;

/**
 * A provider for netty's {@link EventLoopGroup} to be used for RxNetty's clients and servers when they are not
 * provided explicitly.
 *
 * @author Nitesh Kant
 */
public abstract class RxEventLoopProvider {

    /**
     * The {@link EventLoopGroup} to be used by all {@link RxClient} instances if it is not explicitly provided using
     * {@link ClientBuilder#eventloop(EventLoopGroup)}.
     *
     * @return The {@link EventLoopGroup} to be used for all clients.
     */
    public abstract EventLoopGroup globalClientEventLoop();

    /**
     * The {@link EventLoopGroup} to be used by all {@link RxServer} instances if it is not explicitly provided using
     * {@link ServerBuilder#eventLoop(EventLoopGroup)} or {@link ServerBuilder#eventLoops(EventLoopGroup, EventLoopGroup)} .
     *
     * @return The {@link EventLoopGroup} to be used for all servers.
     */
    public abstract EventLoopGroup globalServerEventLoop();

    /**
     * The {@link EventLoopGroup} to be used by all {@link RxServer} instances as a parent eventloop group
     * (First argument to this method: {@link io.netty.bootstrap.ServerBootstrap#group(EventLoopGroup, EventLoopGroup)}),
     * if it is not explicitly provided using {@link ServerBuilder#eventLoop(EventLoopGroup)} or
     * {@link ServerBuilder#eventLoops(EventLoopGroup, EventLoopGroup)}.
     *
     * @return The {@link EventLoopGroup} to be used for all servers.
     */
    public abstract EventLoopGroup globalServerParentEventLoop();

    /**
     * The {@link EventLoopGroup} to be used by all {@link RxClient} instances if it is not explicitly provided using
     * {@link ClientBuilder#eventloop(EventLoopGroup)}.
     *
     * @param nativeTransport {@code true} If the eventloop for native transport is to be returned (if configured)
     *
     * @return The {@link EventLoopGroup} to be used for all client. If {@code nativeTransport} was {@code true} then
     * return the {@link EventLoopGroup} for native transport.
     */
    public abstract EventLoopGroup globalClientEventLoop(boolean nativeTransport);

    /**
     * The {@link EventLoopGroup} to be used by all {@link RxServer} instances if it is not explicitly provided using
     * {@link ServerBuilder#eventLoop(EventLoopGroup)} or {@link ServerBuilder#eventLoops(EventLoopGroup, EventLoopGroup)} .
     *
     * @param nativeTransport {@code true} If the eventloop for native transport is to be returned (if configured)
     *
     * @return The {@link EventLoopGroup} to be used for all servers. If {@code nativeTransport} was {@code true} then
     * return the {@link EventLoopGroup} for native transport.     *
     */
    public abstract EventLoopGroup globalServerEventLoop(boolean nativeTransport);

    /**
     * The {@link EventLoopGroup} to be used by all {@link RxServer} instances as a parent eventloop group
     * (First argument to this method: {@link io.netty.bootstrap.ServerBootstrap#group(EventLoopGroup, EventLoopGroup)}),
     * if it is not explicitly provided using {@link ServerBuilder#eventLoop(EventLoopGroup)} or
     * {@link ServerBuilder#eventLoops(EventLoopGroup, EventLoopGroup)}.
     *
     * @param nativeTransport {@code true} If the eventloop for native transport is to be returned (if configured)
     *
     * @return The {@link EventLoopGroup} to be used for all servers. If {@code nativeTransport} was {@code true} then
     * return the {@link EventLoopGroup} for native transport.
     */
    public abstract EventLoopGroup globalServerParentEventLoop(boolean nativeTransport);
}
