/*
 * Copyright 2016 Netflix, Inc.
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
package io.reactivex.netty.test.util;

import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.client.ConnectionRequest;
import rx.Observable;
import rx.Subscriber;

public class TcpConnectionRequestMock<W, R> extends ConnectionRequest<W, R> {

    public TcpConnectionRequestMock(final Observable<Connection<R, W>> connections) {
        super(new OnSubscribe<Connection<R, W>>() {
            @Override
            public void call(Subscriber<? super Connection<R, W>> subscriber) {
                connections.unsafeSubscribe(subscriber);
            }
        });
    }
}
