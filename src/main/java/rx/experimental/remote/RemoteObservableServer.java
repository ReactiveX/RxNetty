/**
 * Copyright 2013 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.experimental.remote;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.operators.SafeObservableSubscription;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;
import rx.util.functions.Func1;
import rx.util.functions.Function;

public class RemoteObservableServer<T> {

    private final RemoteServerOnSubscribeFunc<T> onSubscribe;

    /**
     * Function interface for work to be performed when an {@link Observable} is subscribed to via {@link Observable#subscribe(Observer)}
     * 
     * @param <T>
     */
    public static interface RemoteServerOnSubscribeFunc<T> extends Function {

        public Subscription onSubscribe(Observer<? super T> observer);

    }

    protected RemoteObservableServer(RemoteServerOnSubscribeFunc<T> onSubscribe) {
        this.onSubscribe = onSubscribe;
    }

    public static <T> RemoteObservableServer<T> create(RemoteServerOnSubscribeFunc<T> onSubscribe) {
        return new RemoteObservableServer<T>(onSubscribe);
    }

    public Subscription subscribe(Observer<? super T> observer) {
        return onSubscribe.onSubscribe(observer);
    }

    public <R> Observable<R> onConnect(Func1<T, Observable<R>> func) {
        return Observable.create(new ConnectedRemoteObservableServer<T, R>(this, func));
    }

    public static class ConnectedRemoteObservableServer<T, R> implements OnSubscribeFunc<R> {

        private final RemoteObservableServer<T> remoteObservableServer;
        private final Func1<T, Observable<R>> func;

        private ConnectedRemoteObservableServer(RemoteObservableServer<T> remoteObservableServer, Func1<T, Observable<R>> func) {
            this.remoteObservableServer = remoteObservableServer;
            this.func = func;
        }

        @Override
        public Subscription onSubscribe(final Observer<? super R> observer) {
            final CompositeSubscription serverSubscription = new CompositeSubscription();

            serverSubscription.add(remoteObservableServer.subscribe(new Observer<T>() {

                @Override
                public void onCompleted() {
                    observer.onCompleted();
                }

                @Override
                public void onError(Throwable e) {
                    observer.onError(e);
                }

                @Override
                public void onNext(T connection) {
                    try {
                        Observable<R> onConnectResponse = func.call(connection);

                        final SafeObservableSubscription connectionSubscription = new SafeObservableSubscription();
                        serverSubscription.add(connectionSubscription);
                        connectionSubscription.wrap(onConnectResponse.subscribe(new Observer<R>() {

                            @Override
                            public void onCompleted() {
                                serverSubscription.remove(connectionSubscription);
                                /*
                                 * we don't call 'observer.onCompleted()' here because
                                 * this is completion of a single connection not the server
                                 * we don't want to shutdown the server
                                 */
                            }

                            @Override
                            public void onError(Throwable e) {
                                /*
                                 * If we get an error in the function processing connections we shut down the server
                                 * This seems harsh but the connection handler function needs to handle errors correctly
                                 * otherwise it ends up here and we have no choice but to propagate "to the top".
                                 */
                                observer.onError(e);
                            }

                            @Override
                            public void onNext(R r) {
                                // pass along the new connection for processing
                                observer.onNext(r);
                            }

                        }));
                    } catch (Throwable e) {
                        observer.onError(e);
                    }
                }
            }));

            return Subscriptions.create(new Action0() {

                @Override
                public void call() {
                    System.out.println("received unsubscribe to SERVER - onConnect - shut down all connections");
                    serverSubscription.unsubscribe();
                }

            });

        }

    }
}
