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
import io.reactivex.netty.channel.ConnectionImpl;
import io.reactivex.netty.channel.DetachedChannelPipeline;
import io.reactivex.netty.events.EventSource;
import io.reactivex.netty.protocol.tcp.client.ConnectionObservable.OnSubcribeFunc;
import io.reactivex.netty.protocol.tcp.client.events.TcpClientEventListener;
import io.reactivex.netty.protocol.tcp.client.events.TcpClientEventPublisher;
import io.reactivex.netty.protocol.tcp.client.internal.EventPublisherFactory;
import io.reactivex.netty.test.util.InboundRequestFeeder;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Func0;

import java.util.ArrayList;
import java.util.List;

public class EmbeddedConnectionProvider<W, R> extends ConnectionProvider<W, R> {

    private final Func0<EmbeddedChannelWithFeeder> channelFactory;
    private final List<EmbeddedChannelWithFeeder> createdChannels = new ArrayList<>();

    public EmbeddedConnectionProvider(final ConnectionFactory<W, R> connectionFactory,
                                      final EventPublisherFactory eventPublisherFactory) {
        super(connectionFactory);
        channelFactory = new Func0<EmbeddedChannelWithFeeder>() {
            @Override
            public EmbeddedChannelWithFeeder call() {
                InboundRequestFeeder feeder = new InboundRequestFeeder();
                EmbeddedChannel embeddedChannel = new EmbeddedChannel(feeder);
                final EventSource<TcpClientEventListener> source = eventPublisherFactory.call(embeddedChannel);
                ClientState<W, R> state = (ClientState<W, R>) connectionFactory;
                DetachedChannelPipeline detachedChannelPipeline = state.unsafeDetachedPipeline();
                detachedChannelPipeline.copyTo(new EmbeddedChannelPipelineDelegate(embeddedChannel));
                return new EmbeddedChannelWithFeeder(embeddedChannel, feeder, source);
            }
        };
    }

    @Override
    public ConnectionObservable<R, W> nextConnection() {
        final EmbeddedChannelWithFeeder channel = channelFactory.call();
        createdChannels.add(channel);
        final TcpClientEventPublisher tmpEp = new TcpClientEventPublisher();

        final Connection<R, W> c = ConnectionImpl.create(channel.getChannel(), tmpEp, tmpEp);
        return ConnectionObservable.createNew(new OnSubcribeFunc<R, W>() {

            @Override
            public Subscription subscribeForEvents(TcpClientEventListener eventListener) {
                return channel.getTcpEventSource().subscribe(eventListener);
            }

            @Override
            public void call(Subscriber<? super Connection<R, W>> subscriber) {
                subscriber.onNext(c);
                subscriber.onCompleted();
            }
        });
    }

    public List<EmbeddedChannelWithFeeder> getCreatedChannels() {
        return createdChannels;
    }
}
