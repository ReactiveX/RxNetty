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
package io.reactivex.netty.protocol.tcp.client;

import io.netty.channel.embedded.EmbeddedChannel;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.channel.DetachedChannelPipeline;
import io.reactivex.netty.events.EventSource;
import io.reactivex.netty.events.ListenersHolder;
import io.reactivex.netty.protocol.tcp.ConnectionSubscriberEvent;
import io.reactivex.netty.protocol.tcp.client.ConnectionObservable.OnSubcribeFunc;
import io.reactivex.netty.protocol.tcp.client.events.TcpClientEventListener;
import io.reactivex.netty.protocol.tcp.client.internal.EventPublisherFactory;
import io.reactivex.netty.protocol.tcp.client.internal.TcpEventPublisherFactory;
import io.reactivex.netty.test.util.InboundRequestFeeder;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Func0;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

public class EmbeddedConnectionFactory<W, R> extends ConnectionFactory<W, R> {

    private final boolean failConnect;
    private final Func0<EmbeddedChannelWithFeeder> channelFactory;
    private final List<EmbeddedChannelWithFeeder> createdChannels = new ArrayList<>();

    public EmbeddedConnectionFactory(boolean failConnect) {
        this(failConnect, new TcpEventPublisherFactory());
    }

    public EmbeddedConnectionFactory(boolean failConnect, final EventPublisherFactory eventPublisherFactory) {
        this.failConnect = failConnect;
        channelFactory = new Func0<EmbeddedChannelWithFeeder>() {
            @Override
            public EmbeddedChannelWithFeeder call() {
                InboundRequestFeeder feeder = new InboundRequestFeeder();
                EmbeddedChannel channel = new EmbeddedChannel(feeder);
                EventSource<TcpClientEventListener> tcpEventSource = eventPublisherFactory.call(channel);
                ClientConnectionToChannelBridge.addToPipeline(channel.pipeline(), false);
                return new EmbeddedChannelWithFeeder(channel, feeder, tcpEventSource);
            }
        };
    }

    public EmbeddedConnectionFactory(ConnectionFactory<W, R> connectionFactory,
                                     final EventPublisherFactory eventPublisherFactory) {
        final ClientState<W, R> implCast = (ClientState<W, R>) connectionFactory;
        failConnect = false;
        channelFactory = new Func0<EmbeddedChannelWithFeeder>() {
            @Override
            public EmbeddedChannelWithFeeder call() {
                InboundRequestFeeder feeder = new InboundRequestFeeder();
                EmbeddedChannel channel = new EmbeddedChannel(feeder);
                EventSource<TcpClientEventListener> source = eventPublisherFactory.call(channel);
                DetachedChannelPipeline detachedChannelPipeline = implCast.unsafeDetachedPipeline();
                detachedChannelPipeline.copyTo(new EmbeddedChannelPipelineDelegate(channel));
                return new EmbeddedChannelWithFeeder(channel, feeder, source);
            }
        };
    }

    @Override
    public ConnectionObservable<R, W> newConnection(SocketAddress hostAddress) {
        if (failConnect) {
            return ConnectionObservable.forError(new IOException());
        }

        return ConnectionObservable.createNew(new OnSubcribeFunc<R, W>() {

            private final ListenersHolder<TcpClientEventListener> listeners = new ListenersHolder<>();

            @Override
            public void call(Subscriber<? super Connection<R, W>> subscriber) {
                EmbeddedChannelWithFeeder channelWithFeeder = channelFactory.call();
                createdChannels.add(channelWithFeeder);

                if (null != channelWithFeeder.getTcpEventSource()) {
                    listeners.subscribeAllTo(channelWithFeeder.getTcpEventSource());
                }

                EmbeddedChannel channel = channelWithFeeder.getChannel();
                channel.connect(new InetSocketAddress("127.0.0.1", 0));

                channel.pipeline().fireUserEventTriggered(new ConnectionSubscriberEvent<>(subscriber));

                if (channel.isOpen()) {
                    channel.pipeline().fireChannelActive();
                } else {
                    subscriber.onError(new IllegalStateException("Embedded channel is not open, post creation."));
                }
            }

            @Override
            public Subscription subscribeForEvents(TcpClientEventListener eventListener) {
                return listeners.subscribe(eventListener);
            }
        });
    }

    public List<EmbeddedChannelWithFeeder> getCreatedChannels() {
        return createdChannels;
    }
}
