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

import io.reactivex.netty.protocol.tcp.client.ClientState;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * An implementation of {@link IdleConnectionsHolder} with a FIFO strategy.
 *
 * @param <W> Type of object that is written to the client using this holder.
 * @param <R> Type of object that is read from the the client using this holder.
 *
 * @author Nitesh Kant
 */
public class FIFOIdleConnectionsHolder<W, R> extends IdleConnectionsHolder<W, R> {

    private final ConcurrentLinkedQueue<PooledConnection<R, W>> idleConnections;
    private final Observable<PooledConnection<R, W>> pollObservable;
    private final Observable<PooledConnection<R, W>> peekObservable;

    public FIFOIdleConnectionsHolder() {
        idleConnections = new ConcurrentLinkedQueue<>();

        pollObservable = Observable.create(new OnSubscribe<PooledConnection<R, W>>() {
            @Override
            public void call(Subscriber<? super PooledConnection<R, W>> subscriber) {
                PooledConnection<R, W> idleConnection;
                while (!subscriber.isUnsubscribed() && (idleConnection = idleConnections.poll()) != null) {
                    if (!idleConnection.isUsable()) {
                        discard(idleConnection);
                    } else {
                        subscriber.onNext(idleConnection);
                        break;
                    }
                }
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onCompleted();
                }
            }
        });

        peekObservable = Observable.from(idleConnections);
    }

    @Override
    public Observable<PooledConnection<R, W>> poll() {
        return pollObservable;
    }

    @Override
    public Observable<PooledConnection<R, W>> peek() {
        return peekObservable;
    }

    @Override
    public void add(PooledConnection<R, W> toAdd) {
        idleConnections.add(toAdd);
    }

    @Override
    public boolean remove(PooledConnection<R, W> toRemove) {
        return idleConnections.remove(toRemove);
    }

    @Override
    protected <WW, RR> IdleConnectionsHolder<WW, RR> doCopy(ClientState<WW, RR> newState) {
        return new FIFOIdleConnectionsHolder<>();
    }
}
