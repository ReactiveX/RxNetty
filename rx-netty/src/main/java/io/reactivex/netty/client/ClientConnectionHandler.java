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
package io.reactivex.netty.client;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.reactivex.netty.channel.ConnectionHandler;
import io.reactivex.netty.channel.ObservableConnection;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;

/**
 * An implementation of {@link ConnectionHandler} that provides notifications to an {@link Observer} of
 * {@link ObservableConnection} pertaining to connection establishment.
 *
 * @param <I> The type of the object that is read from a new connection handled by this handler.
 * @param <O> The type of objects that are written to a new connection handled by this handler.
 *
 * @author Nitesh Kant
 */
public class ClientConnectionHandler<I, O> implements ConnectionHandler<I, O>, ChannelFutureListener {

    private final Observer<? super ObservableConnection<I, O>> connectionObserver;

    public ClientConnectionHandler(Observer<? super ObservableConnection<I, O>> connectionObserver) {
        this.connectionObserver = connectionObserver;
    }

    @Override
    public Observable<Void> handle(final ObservableConnection<I, O> newConnection) {
        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> voidSub) {
                connectionObserver.onNext(newConnection);
                connectionObserver.onCompleted(); // The observer is no longer looking for any more connections.
            }
        });
    }

    @Override
    public void operationComplete(ChannelFuture future) throws Exception {
        if (!future.isSuccess()) {
            connectionObserver.onError(future.cause());
        } // onComplete() needs to be send after onNext(), calling it here will cause a race-condition between next & complete.
    }
}
