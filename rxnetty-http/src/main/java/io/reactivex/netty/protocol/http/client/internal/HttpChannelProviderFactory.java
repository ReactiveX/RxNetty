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

package io.reactivex.netty.protocol.http.client.internal;

import io.reactivex.netty.client.ChannelProvider;
import io.reactivex.netty.client.ChannelProviderFactory;
import io.reactivex.netty.client.Host;
import io.reactivex.netty.client.events.ClientEventListener;
import io.reactivex.netty.events.EventPublisher;
import io.reactivex.netty.events.EventSource;
import io.reactivex.netty.protocol.http.client.events.HttpClientEventPublisher;

public class HttpChannelProviderFactory implements ChannelProviderFactory {

    private final HttpClientEventPublisher clientEventPublisher;

    public HttpChannelProviderFactory(HttpClientEventPublisher clientEventPublisher) {
        this.clientEventPublisher = clientEventPublisher;
    }

    @Override
    public ChannelProvider newProvider(Host host, EventSource<? super ClientEventListener> eventSource,
                                       EventPublisher publisher, ClientEventListener clientPublisher) {
        final HttpClientEventPublisher hostPublisher = new HttpClientEventPublisher();
        hostPublisher.subscribe(clientEventPublisher);
        eventSource.subscribe(hostPublisher);
        return new HttpChannelProvider(hostPublisher);
    }
}
