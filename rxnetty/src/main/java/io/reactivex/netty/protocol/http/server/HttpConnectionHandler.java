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

package io.reactivex.netty.protocol.http.server;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpVersion;
import io.reactivex.netty.channel.ConnectionHandler;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.metrics.Clock;
import io.reactivex.netty.metrics.MetricEventsSubject;
import rx.Observable;
import rx.Subscriber;

/**
* @author Nitesh Kant
*/
class HttpConnectionHandler<I, O> implements ConnectionHandler<HttpServerRequest<I>, HttpServerResponse<O>> {

    private ErrorResponseGenerator<O> responseGenerator = new DefaultErrorResponseGenerator<O>();

    private final RequestHandler<I, O> requestHandler;
    private final boolean send10ResponseFor10Request;
    @SuppressWarnings("rawtypes")private MetricEventsSubject eventsSubject;

    public HttpConnectionHandler(RequestHandler<I, O> requestHandler) {
        this(requestHandler, false);
    }

    public HttpConnectionHandler(RequestHandler<I, O> requestHandler, boolean send10ResponseFor10Request) {
        this.requestHandler = requestHandler;
        this.send10ResponseFor10Request = send10ResponseFor10Request;
    }

    void setResponseGenerator(ErrorResponseGenerator<O> responseGenerator) {
        this.responseGenerator = responseGenerator;
    }

    void useMetricEventsSubject(MetricEventsSubject<?> eventsSubject) {
        this.eventsSubject = eventsSubject;
    }

    @Override
    public Observable<Void> handle(final ObservableConnection<HttpServerRequest<I>, HttpServerResponse<O>> newConnection) {

        return newConnection.getInput().lift(new Observable.Operator<Void, HttpServerRequest<I>>() {
            @Override
            public Subscriber<? super HttpServerRequest<I>> call(final Subscriber<? super Void> child) {
                return new Subscriber<HttpServerRequest<I>>() {
                    @Override
                    public void onCompleted() {
                        child.onCompleted();
                    }

                    @Override
                    public void onError(Throwable e) {
                        child.onError(e);
                    }

                    @SuppressWarnings("unchecked")
                    @Override
                    public void onNext(HttpServerRequest<I> newRequest) {
                        final long startTimeMillis = Clock.newStartTimeMillis();
                        eventsSubject.onEvent(HttpServerMetricsEvent.NEW_REQUEST_RECEIVED);

                        final HttpServerResponse<O> response = new HttpServerResponse<O>(newConnection.getChannel(),
                        /*
                         * Server should send the highest version it is compatible with.
                         * http://tools.ietf.org/html/rfc2145#section-2.3
                         *
                         * unless overriden explicitly.
                         */
                        send10ResponseFor10Request ? newRequest.getHttpVersion() : HttpVersion.HTTP_1_1, eventsSubject);
                        if (newRequest.getHeaders().isKeepAlive()) {
                            if (!newRequest.getHttpVersion().isKeepAliveDefault()) {
                                // Avoid sending keep-alive header if keep alive is default. Issue: https://github.com/Netflix/RxNetty/issues/167
                                // This optimizes data transferred on the wire.

                                // Add keep alive header as per:
                                // - http://www.w3.org/Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html#Connection
                                response.getHeaders().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
                            }
                        } else {
                            response.getHeaders().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
                        }
                        Observable<Void> requestHandlingResult;

                        try {
                            eventsSubject.onEvent(HttpServerMetricsEvent.REQUEST_HANDLING_START,
                                                  Clock.onEndMillis(startTimeMillis));
                            requestHandlingResult = requestHandler.handle(newRequest, response);
                            if (null == requestHandlingResult) {
                                requestHandlingResult = Observable.empty();
                            }
                        } catch (Throwable throwable) {
                            requestHandlingResult = Observable.error(throwable);
                        }

                        requestHandlingResult.subscribe(new Subscriber<Void>() {
                            @Override
                            public void onCompleted() {
                                eventsSubject.onEvent(HttpServerMetricsEvent.REQUEST_HANDLING_SUCCESS,
                                                      Clock.onEndMillis(startTimeMillis));
                                response.close(false); // Since close() can be called only once, the flush is separated from close.
                                // Close will write the last HTTP content and hence the flush must be done post close.
                                if (!response.isFlushOnlyOnReadComplete()) {
                                    response.flush();
                                }
                            }

                            @Override
                            public void onError(Throwable throwable) {
                                eventsSubject.onEvent(HttpServerMetricsEvent.REQUEST_HANDLING_FAILED,
                                                      Clock.onEndMillis(startTimeMillis), throwable);
                                if (!response.isHeaderWritten()) {
                                    responseGenerator.updateResponse(response, throwable);
                                }
                                response.close(false); // Since close() can be called only once, the flush is separated from close.
                                // Close will write the last HTTP content and hence the flush must be done post close.
                                response.flush(); // Response should be flushed for errors: https://github.com/ReactiveX/RxNetty/issues/226
                            }

                            @Override
                            public void onNext(Void aVoid) {
                                // Not significant.
                            }
                        });
                    }
                };
            }
        });
    }
}
