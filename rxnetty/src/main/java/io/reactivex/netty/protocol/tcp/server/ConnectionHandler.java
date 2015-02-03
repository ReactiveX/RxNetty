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
package io.reactivex.netty.protocol.tcp.server;

import io.reactivex.netty.channel.Connection;
import rx.Observable;

/**
 * A connection handler invoked for every new connection is established by {@link TcpServer}
 *
 * @param <R> The type of the object that is read from a new connection.
 * @param <W> The type of objects that are written to a new connection.
 *
 * @author Nitesh Kant
 */
public interface ConnectionHandler<R, W> {

    /**
     * Invoked whenever a new connection is established.
     *
     * @param newConnection Newly established connection.
     *
     * @return An {@link Observable}, unsubscribe from which should cancel the handling.
     */
    Observable<Void> handle(Connection<R, W> newConnection);
}
