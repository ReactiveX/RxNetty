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

package io.reactivex.netty.test.util.embedded;

import io.netty.channel.Channel;
import io.reactivex.netty.client.ChannelProvider;
import io.reactivex.netty.client.ChannelProviderFactory;
import io.reactivex.netty.client.Host;
import io.reactivex.netty.client.events.ClientEventListener;
import io.reactivex.netty.events.EventPublisher;
import io.reactivex.netty.events.EventSource;
import io.reactivex.netty.test.util.InboundRequestFeeder;
import rx.Observable;
import rx.Observable.Operator;
import rx.Subscriber;
import rx.functions.Func0;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class EmbeddedChannelProvider implements ChannelProvider {

    private final List<EmbeddedChannelWithFeeder> createdChannels = new CopyOnWriteArrayList<>();
    private final boolean failConnect;

    public EmbeddedChannelProvider() {
        this(false);
    }

    public EmbeddedChannelProvider(boolean failConnect) {
        this.failConnect = failConnect;
    }

    @Override
    public Observable<Channel> newChannel(Observable<Channel> input) {
        if (failConnect) {
            return Observable.error(new IOException("Deliberate connection failure"));
        }

        return Observable.defer(new Func0<Observable<Channel>>() {
            @Override
            public Observable<Channel> call() {
                InboundRequestFeeder feeder = new InboundRequestFeeder();
                EmbeddedChannelTmp embeddedChannel = new EmbeddedChannelTmp(feeder);
                EmbeddedChannelWithFeeder ecwf = new EmbeddedChannelWithFeeder(embeddedChannel, feeder);
                createdChannels.add(ecwf);
                return Observable.<Channel>just(embeddedChannel);
            }
        }).lift(new Operator<Channel, Channel>() {
            @Override
            public Subscriber<? super Channel> call(final Subscriber<? super Channel> subscriber) {
                return new Subscriber<Channel>(subscriber) {
                    @Override
                    public void onCompleted() {
                        subscriber.onCompleted();
                    }

                    @Override
                    public void onError(Throwable e) {
                        subscriber.onError(e);
                    }

                    @Override
                    public void onNext(Channel channel) {
                        subscriber.onNext(channel);
                        channel.pipeline().fireChannelActive();
                    }
                };
            }
        });
    }

    public List<EmbeddedChannelWithFeeder> getCreatedChannels() {
        return createdChannels;
    }

    public ChannelProviderFactory asFactory() {
        return new ChannelProviderFactory() {
            @Override
            public ChannelProvider newProvider(Host host, EventSource<? super ClientEventListener> eventSource,
                                               EventPublisher publisher, ClientEventListener clientPublisher) {
                return EmbeddedChannelProvider.this;
            }
        };
    }
}
