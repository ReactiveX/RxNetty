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
package io.reactivex.netty.protocol.http.client.internal;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import io.reactivex.netty.events.EventSource;
import io.reactivex.netty.protocol.http.client.events.HttpClientEventPublisher;
import io.reactivex.netty.protocol.http.client.events.HttpClientEventsListener;
import io.reactivex.netty.protocol.tcp.client.events.TcpClientEventListener;
import io.reactivex.netty.protocol.tcp.client.internal.EventPublisherFactory;
import io.reactivex.netty.protocol.tcp.client.internal.TcpEventPublisherFactory;
import rx.Subscription;

public class HttpEventPublisherFactory implements EventPublisherFactory {

    public static final AttributeKey<HttpClientEventsListener> HTTP_CLIENT_EVENT_LISTENER =
            AttributeKey.valueOf("rxnetty_http_client_event_listener");
    private final HttpClientEventPublisher globalClientPublisher;

    public HttpEventPublisherFactory() {
        this(new HttpClientEventPublisher());
    }

    private HttpEventPublisherFactory(HttpClientEventPublisher globalClientPublisher) {
        this.globalClientPublisher = globalClientPublisher;
    }

    public HttpClientEventPublisher getGlobalClientPublisher() {
        return globalClientPublisher;
    }

    @Override
    public EventSource<TcpClientEventListener> call(Channel channel) {
        final HttpClientEventPublisher eventPublisher = new HttpClientEventPublisher();
        channel.attr(TcpEventPublisherFactory.EVENT_PUBLISHER).set(eventPublisher);
        channel.attr(TcpEventPublisherFactory.TCP_CLIENT_EVENT_LISTENER).set(eventPublisher);
        channel.attr(TcpEventPublisherFactory.CONNECTION_EVENT_LISTENER).set(eventPublisher);
        channel.attr(HTTP_CLIENT_EVENT_LISTENER).set(eventPublisher);
        return eventPublisher.asTcpEventSource();
    }

    @Override
    public Subscription subscribe(TcpClientEventListener listener) {
        if (listener instanceof HttpClientEventsListener) {
            return globalClientPublisher.subscribe((HttpClientEventsListener) listener);
        } else {
            return globalClientPublisher.asTcpEventSource().subscribe(listener);
        }
    }

    @Override
    public HttpEventPublisherFactory copy() {
        return new HttpEventPublisherFactory(globalClientPublisher.copy());
    }

    @Override
    public TcpClientEventListener getGlobalClientEventPublisher() {
        return globalClientPublisher;
    }
}
