/*
 * Copyright 2015 Netflix, Inc.
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
 *
 */

package io.reactivex.netty.threads;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;

/**
 * A provider for netty's {@link EventLoopGroup} to be used for RxNetty's clients and servers when they are not
 * provided explicitly.
 */
public abstract class RxEventLoopProvider {

    /**
     * The {@link EventLoopGroup} to be used by all client instances if it is not explicitly provided in the client.
     *
     * @return The {@link EventLoopGroup} to be used for all clients.
     */
    public abstract EventLoopGroup globalClientEventLoop();

    /**
     * The {@link EventLoopGroup} to be used by all server instances if it is not explicitly provided.
     *
     * @return The {@link EventLoopGroup} to be used for all servers.
     */
    public abstract EventLoopGroup globalServerEventLoop();

    /**
     * The {@link EventLoopGroup} to be used by all server instances as a parent eventloop group
     * (First argument to this method: {@link ServerBootstrap#group(EventLoopGroup, EventLoopGroup)}),
     * if it is not explicitly provided.
     *
     * @return The {@link EventLoopGroup} to be used for all servers.
     */
    public abstract EventLoopGroup globalServerParentEventLoop();

    /**
     * The {@link EventLoopGroup} to be used by all client instances if it is not explicitly provided.
     *
     * @param nativeTransport {@code true} If the eventloop for native transport is to be returned (if configured)
     *
     * @return The {@link EventLoopGroup} to be used for all client. If {@code nativeTransport} was {@code true} then
     * return the {@link EventLoopGroup} for native transport.
     */
    public abstract EventLoopGroup globalClientEventLoop(boolean nativeTransport);

    /**
     * The {@link EventLoopGroup} to be used by all server instances if it is not explicitly provided.
     *
     * @param nativeTransport {@code true} If the eventloop for native transport is to be returned (if configured)
     *
     * @return The {@link EventLoopGroup} to be used for all servers. If {@code nativeTransport} was {@code true} then
     * return the {@link EventLoopGroup} for native transport.
     */
    public abstract EventLoopGroup globalServerEventLoop(boolean nativeTransport);

    /**
     * The {@link EventLoopGroup} to be used by all server instances as a parent eventloop group
     * (First argument to this method: {@link ServerBootstrap#group(EventLoopGroup, EventLoopGroup)}),
     * if it is not explicitly provided.
     *
     * @param nativeTransport {@code true} If the eventloop for native transport is to be returned (if configured)
     *
     * @return The {@link EventLoopGroup} to be used for all servers. If {@code nativeTransport} was {@code true} then
     * return the {@link EventLoopGroup} for native transport.
     */
    public abstract EventLoopGroup globalServerParentEventLoop(boolean nativeTransport);
}
