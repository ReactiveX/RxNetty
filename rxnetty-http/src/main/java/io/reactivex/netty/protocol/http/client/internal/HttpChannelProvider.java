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

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import io.reactivex.netty.client.ChannelProvider;
import io.reactivex.netty.protocol.http.client.events.HttpClientEventPublisher;
import io.reactivex.netty.protocol.http.client.events.HttpClientEventsListener;
import rx.Observable;
import rx.functions.Func1;

public class HttpChannelProvider implements ChannelProvider {

    public static final AttributeKey<HttpClientEventsListener> HTTP_CLIENT_EVENT_LISTENER =
            AttributeKey.valueOf("rxnetty_http_client_event_listener");

    private HttpClientEventPublisher hostEventPublisher;

    public HttpChannelProvider(HttpClientEventPublisher hostEventPublisher) {
        this.hostEventPublisher = hostEventPublisher;
    }

    @Override
    public Observable<Channel> newChannel(Observable<Channel> input) {
        return input.map(new Func1<Channel, Channel>() {
                           @Override
                           public Channel call(Channel channel) {
                               channel.attr(HTTP_CLIENT_EVENT_LISTENER).set(hostEventPublisher);
                               return channel;
                           }
                       });
    }
}
