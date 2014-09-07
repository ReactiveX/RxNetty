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

import rx.Observable;
import rx.Subscriber;

/**
 * An operator to be used for a source of {@link HttpClientResponse} containing aggregated responses i.e. which does not
 * have multiple HTTP chunks. This operator simplifies the handling of such a responses by flattening the content
 * {@link Observable} into a single element producing a {@link ResponseHolder} object.
 * See <a href="https://github.com/Netflix/RxNetty/issues/187">related issue</a> for details.
 *
 * @author Nitesh Kant
 */
public class FlatResponseOperator<T>
        implements Observable.Operator<ResponseHolder<T>, HttpClientResponse<T>> {

    public static <T> FlatResponseOperator<T> flatResponse() {
        return new FlatResponseOperator<T>();
    }

    @Override
    public Subscriber<? super HttpClientResponse<T>> call(final Subscriber<? super ResponseHolder<T>> child) {
        return new Subscriber<HttpClientResponse<T>>() {
            @Override
            public void onCompleted() {
                // Content complete propagates to the child subscriber.
            }

            @Override
            public void onError(Throwable e) {
                child.onError(e);
            }

            @Override
            public void onNext(final HttpClientResponse<T> response) {
                response.getContent()
                        .take(1)
                        .lift(new Observable.Operator<ResponseHolder<T>, T>() {
                            @Override
                            public Subscriber<? super T> call(final Subscriber<? super ResponseHolder<T>> child) {
                                return new Subscriber<T>() {

                                    private boolean hasContent;

                                    @Override
                                    public void onCompleted() {
                                        if (!hasContent) {
                                            child.onNext(new ResponseHolder<T>(response));
                                        }
                                        child.onCompleted();
                                    }

                                    @Override
                                    public void onError(Throwable e) {
                                        child.onError(e);
                                    }

                                    @Override
                                    public void onNext(T next) {
                                        hasContent = true;
                                        child.onNext(new ResponseHolder<T>(response, next));
                                    }
                                };
                            }
                        })
                        .subscribe(child);
            }
        };
    }
}
