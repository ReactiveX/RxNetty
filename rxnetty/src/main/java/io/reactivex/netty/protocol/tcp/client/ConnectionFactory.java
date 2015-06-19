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
 */
package io.reactivex.netty.protocol.tcp.client;

import rx.annotations.Beta;

import java.net.SocketAddress;

/**
 * A factory to create new connections. This is used by {@link ConnectionProvider} to delegate actual connection
 * creation work.
 *
 * @param <W> Type of object that is written to the connections created by this factory.
 * @param <R> Type of object that is read from the connections created by this factory.
 */
@Beta
public abstract class ConnectionFactory<W, R> {

    /**
     * Creates a new connection, every time the returned {@code Observable} is subscribed.
     *
     * @param hostAddress The socket address to which a client connection is to be established.
     *
     * @return A {@link ConnectionObservable}, each subscription to which creates a new connection.
     */
    public abstract ConnectionObservable<R, W> newConnection(SocketAddress hostAddress);

}
