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
package io.reactivex.netty.protocol.http.internal;

import io.netty.util.ReferenceCountUtil;
import rx.Subscriber;
import rx.functions.Action1;
import rx.observers.Subscribers;

public class HttpContentSubscriberEvent<T> {

    private final Subscriber<? super T> subscriber;

    public HttpContentSubscriberEvent(Subscriber<? super T> subscriber) {
        this.subscriber = subscriber;
    }

    public Subscriber<? super T> getSubscriber() {
        return subscriber;
    }

    public static <T> HttpContentSubscriberEvent<T> discardAllInput() {
        return new HttpContentSubscriberEvent<>(Subscribers.create(new Action1<T>() {
            @Override
            public void call(T msg) {
                ReferenceCountUtil.release(msg);
            }
        }));
    }
}
