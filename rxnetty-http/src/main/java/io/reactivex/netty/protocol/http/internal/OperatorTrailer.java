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
 *
 */
package io.reactivex.netty.protocol.http.internal;

import io.reactivex.netty.protocol.http.TrailingHeaders;
import rx.Observable;
import rx.Observable.Operator;
import rx.Subscriber;
import rx.exceptions.Exceptions;
import rx.exceptions.OnErrorThrowable;
import rx.functions.Func0;
import rx.functions.Func2;

@SuppressWarnings({"rawtypes", "unchecked"})
public class OperatorTrailer<T extends TrailingHeaders> implements Operator {

    private final Func0<T> trailerFactory;
    private final Func2 trailerMutator;

    public OperatorTrailer(Func0<T> trailerFactory, Func2 trailerMutator) {
        this.trailerFactory = trailerFactory;
        this.trailerMutator = trailerMutator;
    }

    @Override
    public Object call(Object child) {
        final Subscriber subscriber = (Subscriber) child;
        return new Subscriber(subscriber) {

            private T trailer = trailerFactory.call();

            @SuppressWarnings("unchecked")
            @Override
            public void onCompleted() {
                subscriber.onNext(trailer);
                subscriber.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                subscriber.onError(e);
            }

            @SuppressWarnings("unchecked")
            @Override
            public void onNext(Object i) {
                try {
                    trailer = (T) trailerMutator.call(trailer, i);
                    subscriber.onNext(i);
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    onError(OnErrorThrowable.addValueAsLastCause(e, i));
                }
            }
        };
    }

    @SuppressWarnings("unchecked")
    public static <T extends TrailingHeaders> Observable liftFrom(Observable source,
                                                                   Func0<T> trailerFactory, Func2 trailerMutator) {
        return source.lift(new OperatorTrailer<>(trailerFactory, trailerMutator));
    }
}
