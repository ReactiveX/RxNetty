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
package io.reactivex.netty.protocol.tcp.client;

import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.client.ConnectionProvider;
import io.reactivex.netty.client.ConnectionRequest;
import rx.Subscriber;

final class ConnectionRequestImpl<W, R> extends ConnectionRequest<W, R> {

    ConnectionRequestImpl(final ConnectionProvider<W, R> cp) {
        super(new OnSubscribe<Connection<R, W>>() {
            @Override
            public void call(final Subscriber<? super Connection<R, W>> subscriber) {
                cp.newConnectionRequest().unsafeSubscribe(subscriber);
            }
        });
    }
}
