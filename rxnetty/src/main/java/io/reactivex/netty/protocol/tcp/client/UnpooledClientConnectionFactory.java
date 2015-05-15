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

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.protocol.tcp.client.ClientConnectionToChannelBridge.ClientConnectionSubscriberEvent;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Func1;

/**
 * An implementation of {@link ClientConnectionFactory} that creates a new connection for every call to
 * {@link #connect()} and closes the physical connection when {@link ObservableConnection#close()} is
 * invoked.
 *
 * @param <W> Type of object that is written to the client using this factory.
 * @param <R> Type of object that is read from the the client using this factory.
 *
 * @author Nitesh Kant
 */
public final class UnpooledClientConnectionFactory<W, R> extends ClientConnectionFactory<W, R> {

    protected UnpooledClientConnectionFactory(ClientState<W, R> clientState) {
        super(clientState);
    }

    @Override
    public Observable<? extends Connection<R, W>> connect() {
        return Observable.create(new OnSubscribe<Connection<R, W>>() {
            @Override
            public void call(final Subscriber<? super Connection<R, W>> subscriber) {
                final ChannelFuture connectFuture = doConnect();
                connectFuture.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        future.channel()
                              .pipeline()
                              .fireUserEventTriggered(new ClientConnectionSubscriberEvent<R, W>(connectFuture,
                                                                                                subscriber));
                    }
                });
            }
        }).take(1);
    }

    @Override
    public void shutdown() {
        // No op.
    }

    public static <W, R> Func1<ClientState<W, R>, ClientConnectionFactory<W, R>> create() {
        return new Func1<ClientState<W, R>, ClientConnectionFactory<W, R>>() {
            @Override
            public ClientConnectionFactory<W, R> call(ClientState<W, R> clientState) {
                return new UnpooledClientConnectionFactory<>(clientState);
            }
        };
    }

    @Override
    protected <WW, RR> ClientConnectionFactory<WW, RR> doCopy(ClientState<WW, RR> newState) {
        return new UnpooledClientConnectionFactory<>(newState);
    }
}
