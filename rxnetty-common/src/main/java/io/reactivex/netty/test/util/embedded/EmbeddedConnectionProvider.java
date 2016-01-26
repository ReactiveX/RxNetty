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
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.channel.ConnectionImpl;
import io.reactivex.netty.client.ConnectionProvider;
import io.reactivex.netty.client.ConnectionProviderFactory;
import io.reactivex.netty.client.HostConnector;
import rx.Observable;
import rx.functions.Func1;

public class EmbeddedConnectionProvider<W, R> implements ConnectionProvider<W, R> {

    private final EmbeddedChannelProvider channelProvider;

    public EmbeddedConnectionProvider() {
        this(new EmbeddedChannelProvider());
    }

    public EmbeddedConnectionProvider(EmbeddedChannelProvider channelProvider) {
        this.channelProvider = channelProvider;
    }

    @Override
    public Observable<Connection<R, W>> newConnectionRequest() {
        return channelProvider.newChannel(Observable.<Channel>empty())
                              .map(new Func1<Channel, Connection<R, W>>() {
                                  @Override
                                  public Connection<R, W> call(Channel channel) {
                                      return ConnectionImpl.fromChannel(channel);
                                  }
                              });
    }

    public EmbeddedChannelProvider getChannelProvider() {
        return channelProvider;
    }

    public ConnectionProviderFactory<W, R> asFactory() {
        return new ConnectionProviderFactory<W, R>() {
            @Override
            public ConnectionProvider<W, R> newProvider(Observable<HostConnector<W, R>> hosts) {
                return EmbeddedConnectionProvider.this;
            }
        };
    }
}
