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

package io.reactivex.netty.test.util;

import io.netty.channel.Channel;
import io.reactivex.netty.client.events.ClientEventListener;
import io.reactivex.netty.client.internal.EventPublisherFactory;
import io.reactivex.netty.events.EventSource;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

public class MockEventPublisherFactory implements EventPublisherFactory<ClientEventListener> {

    private ClientEventListener listener = new ClientEventListener();

    @Override
    public EventPublisherFactory<ClientEventListener> copy() {
        return new MockEventPublisherFactory();
    }

    @Override
    public ClientEventListener getGlobalClientEventPublisher() {
        return listener;
    }

    @Override
    public Subscription subscribe(ClientEventListener listener) {
        return Subscriptions.empty();
    }

    @Override
    public EventSource<ClientEventListener> call(Channel channel) {
        return new EventSource<ClientEventListener>() {
            @Override
            public Subscription subscribe(ClientEventListener listener) {
                return Subscriptions.empty();
            }
        };
    }
}
