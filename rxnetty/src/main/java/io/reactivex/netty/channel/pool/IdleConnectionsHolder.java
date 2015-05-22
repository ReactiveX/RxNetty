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
package io.reactivex.netty.channel.pool;

import io.netty.channel.EventLoop;
import io.reactivex.netty.protocol.tcp.client.ClientState;
import rx.Observable;

/**
 * A holder of idle {@link PooledConnection} used by {@link PooledClientConnectionFactory}
 *
 * @param <W> Type of object that is written to the client using this holder.
 * @param <R> Type of object that is read from the the client using this holder.
 */
public abstract class IdleConnectionsHolder<W, R> {

    /**
     * Creates a stream of idle connections where every item sent on to the stream is removed from the underlying
     * idle connections pool.
     *
     * @return A stream of idle connections.
     */
    public abstract Observable<PooledConnection<R, W>> poll();

    /**
     * Creates a stream of idle connections where every item sent on to the stream is removed from the underlying
     * idle connections pool.
     * This method will only poll connections if the calling thread is an {@link EventLoop} known to this holder.
     * Otherwise, it should return an empty stream.
     *
     * @return A stream of idle connections.
     */
    public Observable<PooledConnection<R, W>> pollThisEventLoopConnections() {
        return poll(); /*Override if the holder is aware of eventloops*/
    }

    /**
     * Creates a stream of idle connections where every item sent on to the stream is NOT removed from the
     * underlying idle connections pool. If the connection is to be removed, {@link #remove(PooledConnection)} must
     * be called for that connection.
     *
     * @return A stream of idle connections.
     */
    public abstract Observable<PooledConnection<R, W>> peek();

    /**
     * Adds the passed connection to this holder.
     *
     * @param toAdd Connection to add.
     */
    public abstract void add(PooledConnection<R, W> toAdd);

    /**
     * Removes the passed connection from this holder.
     *
     * @param toRemove Connection to remove.
     */
    public abstract boolean remove(PooledConnection<R, W> toRemove);

    public final <WW, RR> IdleConnectionsHolder<WW, RR> copy(ClientState<WW, RR> newState) {
        return doCopy(newState);
    }

    protected abstract <WW, RR> IdleConnectionsHolder<WW, RR> doCopy(ClientState<WW, RR> newState);

    protected Observable<Void> discard(PooledConnection<R, W> toDiscard) {
        return toDiscard.discard();
    }
}
