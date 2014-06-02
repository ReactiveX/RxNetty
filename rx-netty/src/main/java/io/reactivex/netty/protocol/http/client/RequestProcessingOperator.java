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
package io.reactivex.netty.protocol.http.client;

import io.reactivex.netty.channel.ObservableConnection;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.subscriptions.SubscriptionList;
import rx.subscriptions.Subscriptions;

/**
 * @author Nitesh Kant
 */
class RequestProcessingOperator<I, O> implements Observable.Operator<HttpClientResponse<O>,
        ObservableConnection<HttpClientResponse<O>, HttpClientRequest<I>>>{

    private final HttpClientRequest<I> request;

    RequestProcessingOperator(HttpClientRequest<I> request) {
        this.request = request;
    }

    @Override
    public Subscriber<? super ObservableConnection<HttpClientResponse<O>, HttpClientRequest<I>>> call(
            final Subscriber<? super HttpClientResponse<O>> child) {

        final SubscriptionList cs = new SubscriptionList();
        child.add(cs);// Unsubscribe when the child unsubscribes.

        Subscriber<ObservableConnection<HttpClientResponse<O>, HttpClientRequest<I>>> toReturn =
                new Subscriber<ObservableConnection<HttpClientResponse<O>, HttpClientRequest<I>>>(cs) {

                    @Override
                    public void onCompleted() {
                        // Ignore onComplete of connection.
                    }

                    @Override
                    public void onError(Throwable e) {
                        child.onError(e);
                    }

                    @Override
                    public void onNext(final ObservableConnection<HttpClientResponse<O>, HttpClientRequest<I>> connection) {

                        add(Subscriptions.create(new Action0() {
                            @Override
                            public void call() {
                                connection.close();
                            }
                        }));

                        add(connection.getInput()
                                         .doOnNext(new Action1<HttpClientResponse<O>>() {
                                             @Override
                                             public void call(final HttpClientResponse<O> response) {
                                                 add(response.getContent().subscribe(new Observer<O>() {
                                                     @Override
                                                     public void onCompleted() {
                                                         child.onCompleted();
                                                     }

                                                     @Override
                                                     public void onError(Throwable e) {
                                                         // Nothing to do as error on content also comes to the error in input and the child is
                                                         // already subscribed to input.
                                                     }

                                                     @Override
                                                     public void onNext(O o) {
                                                         // Swallow as the eventual subscriber will subscribe to it if required.
                                                     }
                                                 }));
                                             }
                                         }).subscribe(child)); //subscribe the child for response.

                        connection.writeAndFlush(request).doOnError(new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                child.onError(throwable);
                            }
                        });
                    }
                };
        return toReturn;
    }
}
