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
import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.metrics.Clock;
import io.reactivex.netty.metrics.MetricEventsSubject;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

import java.util.concurrent.TimeUnit;

/**
 * @author Nitesh Kant
 */
class RequestProcessingOperator<I, O> implements Observable.Operator<HttpClientResponse<O>,
        ObservableConnection<HttpClientResponse<O>, HttpClientRequest<I>>>{

    private final HttpClientRequest<I> request;
    private final MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject;
    private final long responseSubscriptionTimeoutMs;

    RequestProcessingOperator(HttpClientRequest<I> request, MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject,
                              long responseSubscriptionTimeoutMs) {
        this.request = request;
        this.eventsSubject = eventsSubject;
        this.responseSubscriptionTimeoutMs = responseSubscriptionTimeoutMs;
    }

    @Override
    public Subscriber<? super ObservableConnection<HttpClientResponse<O>, HttpClientRequest<I>>> call(
            final Subscriber<? super HttpClientResponse<O>> child) {
        final long startTimeMillis = Clock.newStartTimeMillis();
        eventsSubject.onEvent(HttpClientMetricsEvent.REQUEST_SUBMITTED);
        final CompositeSubscription cs = new CompositeSubscription();
        child.add(cs);// Unsubscribe when the child unsubscribes.

        Subscriber<ObservableConnection<HttpClientResponse<O>, HttpClientRequest<I>>> toReturn =
                new Subscriber<ObservableConnection<HttpClientResponse<O>, HttpClientRequest<I>>>() {

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

                        // Why don't we close the connection on unsubscribe?
                        // See issue: https://github.com/ReactiveX/RxNetty/issues/225
                        cs.add(connection.getInput()
                                         .doOnNext(new Action1<HttpClientResponse<O>>() {
                                             @Override
                                             public void call(final HttpClientResponse<O> response) {
                                                 response.updateNoContentSubscriptionTimeoutIfNotScheduled(
                                                         responseSubscriptionTimeoutMs,
                                                         TimeUnit.MILLISECONDS);
                                             }
                                         })
                                         .doOnError(new Action1<Throwable>() {
                                             @Override
                                             public void call(Throwable throwable) {
                                                 eventsSubject.onEvent(HttpClientMetricsEvent.RESPONSE_FAILED,
                                                                       Clock.onEndMillis(startTimeMillis), throwable);
                                             }
                                         })
                                         .subscribe(child)); //subscribe the child for response.
                        request.doOnWriteComplete(new Action0() {
                            @Override
                            public void call() {
                                connection.flush().subscribe(new Observer<Void>() {
                                    @Override
                                    public void onCompleted() {
                                        // Failure in writes gets reported during write, this is for the entire request write
                                        // over event.
                                        eventsSubject.onEvent(HttpClientMetricsEvent.REQUEST_WRITE_COMPLETE,
                                                              Clock.onEndMillis(startTimeMillis));
                                    }

                                    @Override
                                    public void onError(Throwable e) {
                                        eventsSubject.onEvent(HttpClientMetricsEvent.REQUEST_WRITE_FAILED,
                                                              Clock.onEndMillis(startTimeMillis), e);
                                        child.onError(e);
                                    }

                                    @Override
                                    public void onNext(Void aVoid) {
                                        // No op as nothing to do for a write onNext().
                                    }
                                });
                            }
                        });
                        connection.write(request);
                    }
                };
        return toReturn;
    }
}
