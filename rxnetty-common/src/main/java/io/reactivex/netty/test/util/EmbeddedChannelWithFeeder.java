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

import io.netty.channel.embedded.EmbeddedChannel;
import io.reactivex.netty.client.events.ClientEventListener;
import io.reactivex.netty.events.EventSource;

public class EmbeddedChannelWithFeeder {

    private final EmbeddedChannel channel;
    private final InboundRequestFeeder feeder;
    private final EventSource<? extends ClientEventListener> tcpEventSource;

    public EmbeddedChannelWithFeeder(EmbeddedChannel channel, InboundRequestFeeder feeder) {
        this(channel, feeder, new MockEventPublisherFactory());
    }

    public EmbeddedChannelWithFeeder(EmbeddedChannel channel, InboundRequestFeeder feeder,
                                     EventSource<? extends ClientEventListener> tcpEventSource) {
        this.channel = channel;
        this.feeder = feeder;
        this.tcpEventSource = tcpEventSource;
    }

    public EmbeddedChannel getChannel() {
        return channel;
    }

    public InboundRequestFeeder getFeeder() {
        return feeder;
    }

    public EventSource<? extends ClientEventListener> getTcpEventSource() {
        return tcpEventSource;
    }
}
