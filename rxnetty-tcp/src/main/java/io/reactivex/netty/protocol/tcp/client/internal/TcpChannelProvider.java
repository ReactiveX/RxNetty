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

package io.reactivex.netty.protocol.tcp.client.internal;

import io.netty.channel.Channel;
import io.reactivex.netty.channel.DetachedChannelPipeline;
import io.reactivex.netty.client.ChannelProvider;
import io.reactivex.netty.client.events.ClientEventListener;
import io.reactivex.netty.events.EventAttributeKeys;
import io.reactivex.netty.events.EventPublisher;
import rx.Observable;
import rx.functions.Func1;

public class TcpChannelProvider implements ChannelProvider {

    private final DetachedChannelPipeline channelPipeline;
    private final ChannelProvider delegate;
    private final EventPublisher publisher;
    private final ClientEventListener hostEventPublisher;

    public TcpChannelProvider(DetachedChannelPipeline channelPipeline, ChannelProvider delegate,
                              EventPublisher publisher, ClientEventListener hostEventPublisher) {
        this.channelPipeline = channelPipeline;
        this.delegate = delegate;
        this.publisher = publisher;
        this.hostEventPublisher = hostEventPublisher;
    }

    @Override
    public Observable<Channel> newChannel(Observable<Channel> input) {
        return delegate.newChannel(input)
                       .map(new Func1<Channel, Channel>() {
                           @Override
                           public Channel call(Channel channel) {
                               channel.attr(EventAttributeKeys.EVENT_PUBLISHER).set(publisher);
                               channel.attr(EventAttributeKeys.CLIENT_EVENT_LISTENER).set(hostEventPublisher);
                               channel.attr(EventAttributeKeys.CONNECTION_EVENT_LISTENER).set(hostEventPublisher);
                               channelPipeline.addToChannel(channel);
                               return channel;
                           }
                       });
    }
}
