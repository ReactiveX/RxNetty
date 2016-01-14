/*
 * Copyright 2016 Netflix, Inc.
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
package io.reactivex.netty.client;

import io.reactivex.netty.channel.Connection;
import rx.Observable;

/**
 * A connection request that is used to create connections for different protocols.
 *
 * <h2>Mutations</h2>
 *
 * All mutations to this request that creates a brand new instance.
 *
 * <h2> Inititating connections</h2>
 *
 * A new connection is initiated every time {@link ConnectionRequest#subscribe()} is called and is the only way of
 * creating connections.
 *
 * @param <W> The type of the objects that are written to the connection created by this request.
 * @param <R> The type of objects that are read from the connection created by this request.
 */
public abstract class ConnectionRequest<W, R> extends Observable<Connection<R, W>> {

    protected ConnectionRequest(OnSubscribe<Connection<R, W>> f) {
        super(f);
    }
}
