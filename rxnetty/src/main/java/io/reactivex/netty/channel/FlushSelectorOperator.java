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
package io.reactivex.netty.channel;

import io.netty.channel.Channel;
import rx.Observable.Operator;
import rx.Subscriber;
import rx.functions.Func1;

public class FlushSelectorOperator<T> implements Operator<T, T> {

    private final Func1<T, Boolean> flushSelector;
    private final Channel channel;

    public FlushSelectorOperator(Func1<T, Boolean> flushSelector, Channel channel) {
        this.flushSelector = flushSelector;
        this.channel = channel;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> subscriber) {

        return new Subscriber<T>(subscriber) {
            @Override
            public void onCompleted() {
                subscriber.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                subscriber.onError(e);
            }

            @Override
            public void onNext(T next) {
                subscriber.onNext(next);
                /*Call the selector _after_ writing an element*/
                if (flushSelector.call(next)) {
                    channel.flush();
                }
            }
        };
    }
}
