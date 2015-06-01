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

import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.events.EventSource;
import io.reactivex.netty.events.ListenersHolder;
import io.reactivex.netty.protocol.tcp.client.events.TcpClientEventListener;
import io.reactivex.netty.protocol.tcp.client.events.TcpClientEventPublisher;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.annotations.Beta;
import rx.functions.Action1;
import rx.subscriptions.Subscriptions;

@Beta
public final class ConnectionObservable<R, W> extends Observable<Connection<R, W>> {

    private final OnSubcribeFunc<R, W> f;

    private ConnectionObservable(final OnSubcribeFunc<R, W> f) {
        super(new OnSubscribe<Connection<R, W>>() {
            @Override
            public void call(Subscriber<? super Connection<R, W>> subscriber) {
                f.call(subscriber);
            }
        });
        this.f = f;
    }

    public Subscription subscribeForEvents(TcpClientEventListener eventListener) {
        return f.subscribeForEvents(eventListener);
    }

    public static <R, W> ConnectionObservable<R, W> forError(final Throwable error) {
        return new ConnectionObservable<R, W>(new OnSubcribeFunc<R, W>() {
            @Override
            public Subscription subscribeForEvents(TcpClientEventListener eventListener) {
                return Subscriptions.empty();
            }

            @Override
            public void call(Subscriber<? super Connection<R, W>> subscriber) {
                subscriber.onError(error);
            }
        });
    }

    public static <R, W> ConnectionObservable<R, W> createNew(final OnSubcribeFunc<R, W> onSubscribe) {
        return new ConnectionObservable<R, W>(onSubscribe);
    }

    public interface OnSubcribeFunc<R, W> extends Action1<Subscriber<? super Connection<R, W>>> {

        Subscription subscribeForEvents(TcpClientEventListener eventListener);

    }

    public static abstract class AbstractOnSubscribeFunc<R, W> implements OnSubcribeFunc<R, W> {

        private final ListenersHolder<TcpClientEventListener> listeners = new ListenersHolder<>();

        @Override
        public final void call(Subscriber<? super Connection<R, W>> subscriber) {
            doSubscribe(subscriber, new Action1<ConnectionObservable<R, W>>() {
                @Override
                public void call(ConnectionObservable<R, W> connectionObservable) {
                    listeners.subscribeAllTo(connectionObservable);
                }
            });
        }

        @Override
        public final Subscription subscribeForEvents(TcpClientEventListener eventListener) {
            return listeners.subscribe(eventListener);
        }

        protected abstract void doSubscribe(Subscriber<? super Connection<R, W>> sub,
                                            Action1<ConnectionObservable<R, W>> subscribeAllListenersAction);
    }
}
