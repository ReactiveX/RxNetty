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

import io.reactivex.netty.channel.DetachedChannelPipeline;
import io.reactivex.netty.client.ChannelProvider;
import io.reactivex.netty.client.ChannelProviderFactory;
import io.reactivex.netty.client.Host;
import io.reactivex.netty.client.events.ClientEventListener;
import io.reactivex.netty.events.EventPublisher;
import io.reactivex.netty.events.EventSource;

public class TcpChannelProviderFactory implements ChannelProviderFactory {

    private final DetachedChannelPipeline channelPipeline;
    private final ChannelProviderFactory delegate;

    public TcpChannelProviderFactory(DetachedChannelPipeline channelPipeline, ChannelProviderFactory delegate) {
        this.channelPipeline = channelPipeline;
        this.delegate = delegate instanceof TcpChannelProviderFactory ? ((TcpChannelProviderFactory) delegate).delegate
                                                                      : delegate;
    }

    @Override
    public ChannelProvider newProvider(Host host, EventSource<? super ClientEventListener> hostEventSource,
                                       EventPublisher publisher, ClientEventListener hostEventPublisher) {
        ChannelProvider delegate = this.delegate.newProvider(host, hostEventSource, publisher, hostEventPublisher);
        return new TcpChannelProvider(channelPipeline, delegate, publisher, hostEventPublisher);
    }
}
