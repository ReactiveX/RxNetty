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
package io.reactivex.netty.protocol.tcp.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.logging.LogLevel;
import io.netty.util.concurrent.EventExecutorGroup;
import io.reactivex.netty.HandlerNames;
import io.reactivex.netty.channel.ChannelSubscriberEvent;
import io.reactivex.netty.channel.Connection;
import io.reactivex.netty.channel.ConnectionImpl;
import io.reactivex.netty.channel.DetachedChannelPipeline;
import io.reactivex.netty.client.ChannelProvider;
import io.reactivex.netty.client.ChannelProviderFactory;
import io.reactivex.netty.client.ClientState;
import io.reactivex.netty.client.ConnectionProvider;
import io.reactivex.netty.client.ConnectionProviderFactory;
import io.reactivex.netty.client.ConnectionRequest;
import io.reactivex.netty.client.Host;
import io.reactivex.netty.client.HostConnector;
import io.reactivex.netty.client.internal.SingleHostConnectionProvider;
import io.reactivex.netty.events.EventSource;
import io.reactivex.netty.internal.InternalReadTimeoutHandler;
import io.reactivex.netty.protocol.tcp.client.events.TcpClientEventListener;
import io.reactivex.netty.protocol.tcp.client.events.TcpClientEventPublisher;
import io.reactivex.netty.protocol.tcp.client.internal.TcpChannelProviderFactory;
import io.reactivex.netty.ssl.SslCodec;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;

import javax.net.ssl.SSLEngine;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

public final class TcpClientImpl<W, R> extends TcpClient<W, R> {

    private final ClientState<W, R> state;
    private final TcpClientEventPublisher eventPublisher;
    private final InterceptingTcpClient<W, R> interceptingTcpClient;
    private ConnectionRequestImpl<W, R> requestSetLazily;

    private TcpClientImpl(ClientState<W, R> state, TcpClientEventPublisher eventPublisher,
                          InterceptingTcpClient<W, R> interceptingTcpClient) {
        this.state = state;
        this.eventPublisher = eventPublisher;
        this.interceptingTcpClient = interceptingTcpClient;
    }

    @Override
    public ConnectionRequest<W, R> createConnectionRequest() {
        return requestSetLazily;
    }

    @Override
    public <T> TcpClient<W, R> channelOption(ChannelOption<T> option, T value) {
        return copy(state.channelOption(option, value), eventPublisher);
    }

    @Override
    public TcpClient<W, R> readTimeOut(final int timeOut, final TimeUnit timeUnit) {
        return addChannelHandlerFirst(HandlerNames.ClientReadTimeoutHandler.getName(), new Func0<ChannelHandler>() {
            @Override
            public ChannelHandler call() {
                return new InternalReadTimeoutHandler(timeOut, timeUnit);
            }
        });
    }

    @Override
    public <WW, RR> TcpClient<WW, RR> addChannelHandlerFirst(String name, Func0<ChannelHandler> handlerFactory) {
        return copy(state.<WW, RR>addChannelHandlerFirst(name, handlerFactory), eventPublisher);
    }

    @Override
    public <WW, RR> TcpClient<WW, RR> addChannelHandlerFirst(EventExecutorGroup group, String name,
                                                             Func0<ChannelHandler> handlerFactory) {
        return copy(state.<WW, RR>addChannelHandlerFirst(group, name, handlerFactory), eventPublisher);
    }

    @Override
    public <WW, RR> TcpClient<WW, RR> addChannelHandlerLast(String name, Func0<ChannelHandler> handlerFactory) {
        return copy(state.<WW, RR>addChannelHandlerLast(name, handlerFactory), eventPublisher);
    }

    @Override
    public <WW, RR> TcpClient<WW, RR> addChannelHandlerLast(EventExecutorGroup group, String name,
                                                            Func0<ChannelHandler> handlerFactory) {
        return copy(state.<WW, RR>addChannelHandlerLast(group, name, handlerFactory), eventPublisher);
    }

    @Override
    public <WW, RR> TcpClient<WW, RR> addChannelHandlerBefore(String baseName, String name,
                                                              Func0<ChannelHandler> handlerFactory) {
        return copy(state.<WW, RR>addChannelHandlerBefore(baseName, name, handlerFactory), eventPublisher);
    }

    @Override
    public <WW, RR> TcpClient<WW, RR> addChannelHandlerBefore(EventExecutorGroup group, String baseName, String name,
                                                              Func0<ChannelHandler> handlerFactory) {
        return copy(state.<WW, RR>addChannelHandlerBefore(group, baseName, name, handlerFactory), eventPublisher);
    }

    @Override
    public <WW, RR> TcpClient<WW, RR> addChannelHandlerAfter(String baseName, String name,
                                                             Func0<ChannelHandler> handlerFactory) {
        return copy(state.<WW, RR>addChannelHandlerAfter(baseName, name, handlerFactory), eventPublisher);
    }

    @Override
    public <WW, RR> TcpClient<WW, RR> addChannelHandlerAfter(EventExecutorGroup group, String baseName, String name,
                                                             Func0<ChannelHandler> handlerFactory) {
        return copy(state.<WW, RR>addChannelHandlerAfter(group, baseName, name, handlerFactory), eventPublisher);
    }

    @Override
    public <WW, RR> TcpClient<WW, RR> pipelineConfigurator(Action1<ChannelPipeline> pipelineConfigurator) {
        return copy(state.<WW, RR>pipelineConfigurator(pipelineConfigurator), eventPublisher);
    }

    @Override
    public TcpClient<W, R> enableWireLogging(LogLevel wireLoggingLevel) {
        return copy(state.enableWireLogging(wireLoggingLevel), eventPublisher);
    }

    @Override
    public TcpClient<W, R> secure(Func1<ByteBufAllocator, SSLEngine> sslEngineFactory) {
        return copy(state.secure(sslEngineFactory), eventPublisher);
    }

    @Override
    public TcpClient<W, R> secure(SSLEngine sslEngine) {
        return copy(state.secure(sslEngine), eventPublisher);
    }

    @Override
    public TcpClient<W, R> secure(SslCodec sslCodec) {
        return copy(state.secure(sslCodec), eventPublisher);
    }

    @Override
    public TcpClient<W, R> unsafeSecure() {
        return copy(state.unsafeSecure(), eventPublisher);
    }

    @Override
    public TcpClient<W, R> channelProvider(ChannelProviderFactory providerFactory) {
        return copy(state.channelProviderFactory(providerFactory), eventPublisher);
    }

    @Override
    public Subscription subscribe(TcpClientEventListener listener) {
        return interceptingTcpClient.subscribe(listener);
    }

    @Override
    public TcpClientInterceptorChain<W, R> intercept() {
        return interceptingTcpClient.intercept();
    }

    /*Visible for testing*/ ClientState<W, R> getClientState() {
        return state;
    }

    public static <W, R> TcpClientImpl<W, R> create(SocketAddress socketAddress) {
        final Host host = new Host(socketAddress);
        return create(new ConnectionProviderFactory<W, R>() {
            @Override
            public ConnectionProvider<W, R> newProvider(Observable<HostConnector<W, R>> hosts) {
                return new SingleHostConnectionProvider<>(hosts);
            }
        }, Observable.just(host));
    }

    public static <W, R> TcpClientImpl<W, R> create(ConnectionProviderFactory<W, R> factory,
                                                    Observable<Host> hostStream) {
        ClientState<W, R> state = ClientState.create(factory, hostStream);
        final TcpClientEventPublisher eventPublisher = new TcpClientEventPublisher();
        return _create(state, eventPublisher);
    }

    private static <W, R> TcpClientImpl<W, R> copy(final ClientState<W, R> state,
                                                   TcpClientEventPublisher eventPublisher) {
        return _create(state, eventPublisher);
    }

    /*Visible for testing*/ static <W, R> TcpClientImpl<W, R> _create(ClientState<W, R> state,
                                                                      TcpClientEventPublisher eventPublisher) {
        DetachedChannelPipeline channelPipeline = state.unsafeDetachedPipeline();
        state = state.channelProviderFactory(new TcpChannelProviderFactory(channelPipeline,
                                                                           state.getChannelProviderFactory()));

        HostConnectorFactory<W, R> hostConnectorFactory = new HostConnectorFactory<>(state, eventPublisher);

        ConnectionProvider<W, R> cp = state.getFactory()
                                             .newProvider(state.getHostStream().map(hostConnectorFactory));

        InterceptingTcpClient<W, R> interceptingTcpClient = new InterceptingTcpClientImpl<>(cp, eventPublisher);
        TcpClientImpl<W, R> client = new TcpClientImpl<>(state, eventPublisher, interceptingTcpClient);
        client.requestSetLazily = new ConnectionRequestImpl<>(cp);
        return client;
    }

    private static class HostConnectorFactory<W, R> implements Func1<Host, HostConnector<W, R>> {

        private final ChannelProviderFactory channelProviderFactory;
        private final TcpClientEventPublisher clientEventPublisher;
        private ClientState<W, R> state;

        public HostConnectorFactory(ClientState<W, R> state, TcpClientEventPublisher clientEventPublisher) {
            this.state = state;
            channelProviderFactory = state.getChannelProviderFactory();
            this.clientEventPublisher = clientEventPublisher;
        }

        @Override
        public HostConnector<W, R> call(final Host host) {
            TcpClientEventPublisher hostEventPublisher = new TcpClientEventPublisher();
            @SuppressWarnings({"unchecked", "rawtypes"})
            EventSource eventSource = hostEventPublisher;
            hostEventPublisher.subscribe(clientEventPublisher);
            @SuppressWarnings("unchecked")
            ChannelProvider channelProvider = channelProviderFactory.newProvider(host, eventSource, hostEventPublisher,
                                                                                 hostEventPublisher);
            return new HostConnector<>(host, new TerminalConnectionProvider<>(host, channelProvider, state),
                                       hostEventPublisher, hostEventPublisher, hostEventPublisher);
        }
    }

    private static class TerminalConnectionProvider<W, R> implements ConnectionProvider<W, R> {

        private final Host host;
        private final Bootstrap bootstrap;
        private final ChannelProvider channelProvider;

        public TerminalConnectionProvider(Host host, ChannelProvider channelProvider, ClientState<W, R> state) {
            this.host = host;
            this.channelProvider = channelProvider;
            bootstrap = state.newBootstrap();
        }

        @Override
        public Observable<Connection<R, W>> newConnectionRequest() {
            return channelProvider.newChannel(Observable.create(new OnSubscribe<Channel>() {
                @Override
                public void call(final Subscriber<? super Channel> s) {
                    final ChannelFuture cf = bootstrap.connect(host.getHost());
                    s.add(Subscriptions.create(new Action0() {
                        @Override
                        public void call() {
                            if (null != cf && !cf.isDone()) {
                                cf.cancel(false);
                            }
                        }
                    }));
                    cf.addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(final ChannelFuture future) throws Exception {
                            if (!future.isSuccess()) {
                                s.onError(future.cause());
                            } else {
                                s.onNext(cf.channel());
                                s.onCompleted();
                            }
                        }
                    });
                }
            })).switchMap(new Func1<Channel, Observable<Channel>>() {
                @Override
                public Observable<Channel> call(final Channel channel) {

                    /*
                     * If channel is unregistered, all handlers are removed and hence the event will not flow through
                     * to the handler for the subscriber to be notified.
                      * So, here the channel is directly passed through the chain if the channel isn't registered.
                     */
                    if (channel.eventLoop().inEventLoop()) {
                        if (channel.isRegistered()) {
                            return Observable.create(new OnSubscribe<Channel>() {
                                @Override
                                public void call(Subscriber<? super Channel> subscriber) {
                                    channel.pipeline().fireUserEventTriggered(new ChannelSubscriberEvent<>(subscriber));
                                }
                            });
                        } else {
                            return Observable.just(channel);
                        }
                    } else {
                        return Observable.create(new OnSubscribe<Channel>() {
                            @Override
                            public void call(final Subscriber<? super Channel> subscriber) {
                                channel.eventLoop().execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (channel.isRegistered()) {
                                            channel.pipeline()
                                                   .fireUserEventTriggered(new ChannelSubscriberEvent<>(subscriber));
                                        } else {
                                            subscriber.onNext(channel);
                                            subscriber.onCompleted();
                                        }
                                    }
                                });
                            }
                        });
                    }
                }
            }).map(new Func1<Channel, Connection<R, W>>() {
                @Override
                public Connection<R, W> call(Channel channel) {
                    return ConnectionImpl.fromChannel(channel);
                }
            });
        }
    }
}
