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
package io.reactivex.netty.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.AbstractChannel;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.ChannelSubscriberEvent;
import io.reactivex.netty.channel.DetachedChannelPipeline;
import io.reactivex.netty.channel.WriteTransformer;
import io.reactivex.netty.client.events.ClientEventListener;
import io.reactivex.netty.events.EventPublisher;
import io.reactivex.netty.events.EventSource;
import io.reactivex.netty.ssl.DefaultSslCodec;
import io.reactivex.netty.ssl.SslCodec;
import io.reactivex.netty.util.LoggingHandlerFactory;
import rx.Observable;
import rx.exceptions.Exceptions;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;

import javax.net.ssl.SSLEngine;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import static io.reactivex.netty.HandlerNames.*;


/**
 * A collection of state that a client holds. This supports the copy-on-write semantics of clients.
 *
 * @param <W> The type of objects written to the client owning this state.
 * @param <R> The type of objects read from the client owning this state.
 */
public class ClientState<W, R> {

    private final Observable<Host> hostStream;
    private final ConnectionProviderFactory<W, R> factory;
    private final DetachedChannelPipeline detachedPipeline;
    private final Map<ChannelOption<?>, Object> options;
    private final boolean isSecure;
    private final EventLoopGroup eventLoopGroup;
    private final Class<? extends Channel> channelClass;
    private final ChannelProviderFactory channelProviderFactory;

    protected ClientState(Observable<Host> hostStream, ConnectionProviderFactory<W, R> factory,
                          DetachedChannelPipeline detachedPipeline, EventLoopGroup eventLoopGroup,
                          Class<? extends Channel> channelClass) {
        this.eventLoopGroup = eventLoopGroup;
        this.channelClass = channelClass;
        options = new LinkedHashMap<>(); /// Same as netty bootstrap, order matters.
        this.hostStream = hostStream;
        this.factory = factory;
        this.detachedPipeline = detachedPipeline;
        isSecure = false;
        channelProviderFactory = new ChannelProviderFactory() {
            @Override
            public ChannelProvider newProvider(Host host, EventSource<? super ClientEventListener> eventSource,
                                               EventPublisher publisher, ClientEventListener clientPublisher) {
                return new ChannelProvider() {
                    @Override
                    public Observable<Channel> newChannel(Observable<Channel> input) {
                        return input;
                    }
                };
            }
        };
    }

    protected ClientState(ClientState<W, R> toCopy, ChannelOption<?> option, Object value) {
        options = new LinkedHashMap<>(toCopy.options); // Since, we are adding an option, copy it.
        options.put(option, value);
        detachedPipeline = toCopy.detachedPipeline;
        hostStream = toCopy.hostStream;
        factory = toCopy.factory;
        eventLoopGroup = toCopy.eventLoopGroup;
        channelClass = toCopy.channelClass;
        isSecure = toCopy.isSecure;
        channelProviderFactory = toCopy.channelProviderFactory;
    }

    protected ClientState(ClientState<?, ?> toCopy, DetachedChannelPipeline newPipeline, boolean secure) {
        final ClientState<W, R> toCopyCast = toCopy.cast();
        options = toCopy.options;
        hostStream = toCopy.hostStream;
        factory = toCopyCast.factory;
        eventLoopGroup = toCopy.eventLoopGroup;
        channelClass = toCopy.channelClass;
        detachedPipeline = newPipeline;
        isSecure = secure;
        channelProviderFactory = toCopyCast.channelProviderFactory;
    }

    protected ClientState(ClientState<?, ?> toCopy, ChannelProviderFactory newFactory) {
        final ClientState<W, R> toCopyCast = toCopy.cast();
        options = toCopy.options;
        hostStream = toCopy.hostStream;
        factory = toCopyCast.factory;
        eventLoopGroup = toCopy.eventLoopGroup;
        channelClass = toCopy.channelClass;
        detachedPipeline = toCopy.detachedPipeline;
        channelProviderFactory = newFactory;
        isSecure = toCopy.isSecure;
    }

    protected ClientState(ClientState<?, ?> toCopy, SslCodec sslCodec) {
        this(toCopy, toCopy.detachedPipeline.copy(new TailHandlerFactory(true)).configure(sslCodec), true);
    }

    public <T> ClientState<W, R> channelOption(ChannelOption<T> option, T value) {
        return new ClientState<>(this, option, value);
    }

    public <WW, RR> ClientState<WW, RR> addChannelHandlerFirst(String name, Func0<ChannelHandler> handlerFactory) {
        ClientState<WW, RR> copy = copy();
        copy.detachedPipeline.addFirst(name, handlerFactory);
        return copy;
    }

    public <WW, RR> ClientState<WW, RR> addChannelHandlerFirst(EventExecutorGroup group, String name,
                                                               Func0<ChannelHandler> handlerFactory) {
        ClientState<WW, RR> copy = copy();
        copy.detachedPipeline.addFirst(group, name, handlerFactory);
        return copy;
    }

    public <WW, RR> ClientState<WW, RR> addChannelHandlerLast(String name, Func0<ChannelHandler> handlerFactory) {
        ClientState<WW, RR> copy = copy();
        copy.detachedPipeline.addLast(name, handlerFactory);
        return copy;
    }

    public <WW, RR> ClientState<WW, RR> addChannelHandlerLast(EventExecutorGroup group, String name,
                                                              Func0<ChannelHandler> handlerFactory) {
        ClientState<WW, RR> copy = copy();
        copy.detachedPipeline.addLast(group, name, handlerFactory);
        return copy;
    }

    public <WW, RR> ClientState<WW, RR> addChannelHandlerBefore(String baseName, String name,
                                                                Func0<ChannelHandler> handlerFactory) {
        ClientState<WW, RR> copy = copy();
        copy.detachedPipeline.addBefore(baseName, name, handlerFactory);
        return copy;
    }

    public <WW, RR> ClientState<WW, RR> addChannelHandlerBefore(EventExecutorGroup group, String baseName,
                                                                String name, Func0<ChannelHandler> handlerFactory) {
        ClientState<WW, RR> copy = copy();
        copy.detachedPipeline.addBefore(group, baseName, name, handlerFactory);
        return copy;
    }

    public <WW, RR> ClientState<WW, RR> addChannelHandlerAfter(String baseName, String name,
                                                               Func0<ChannelHandler> handlerFactory) {
        ClientState<WW, RR> copy = copy();
        copy.detachedPipeline.addAfter(baseName, name, handlerFactory);
        return copy;
    }

    public <WW, RR> ClientState<WW, RR> addChannelHandlerAfter(EventExecutorGroup group, String baseName,
                                                               String name, Func0<ChannelHandler> handlerFactory) {
        ClientState<WW, RR> copy = copy();
        copy.detachedPipeline.addAfter(group, baseName, name, handlerFactory);
        return copy;
    }

    public <WW, RR> ClientState<WW, RR> pipelineConfigurator(Action1<ChannelPipeline> pipelineConfigurator) {
        ClientState<WW, RR> copy = copy();
        copy.detachedPipeline.configure(pipelineConfigurator);
        return copy;
    }

    public ClientState<W, R> enableWireLogging(final LogLevel wireLoggingLevel) {
        return enableWireLogging(LoggingHandler.class.getName(), wireLoggingLevel);
    }

    public ClientState<W, R> enableWireLogging(String name, final LogLevel wireLoggingLevel) {
        return addChannelHandlerFirst(WireLogging.getName(),
                                      LoggingHandlerFactory.getFactory(name, wireLoggingLevel));
    }

    public static <WW, RR> ClientState<WW, RR> create(ConnectionProviderFactory<WW, RR> factory,
                                                      Observable<Host> hostStream) {
        return create(newChannelPipeline(new TailHandlerFactory(false)), factory, hostStream);
    }

    public static <WW, RR> ClientState<WW, RR> create(DetachedChannelPipeline detachedPipeline,
                                                      ConnectionProviderFactory<WW, RR> factory,
                                                      Observable<Host> hostStream) {
        return create(detachedPipeline, factory, hostStream, defaultEventloopGroup(), defaultSocketChannelClass());
    }

    public static <WW, RR> ClientState<WW, RR> create(DetachedChannelPipeline detachedPipeline,
                                                      ConnectionProviderFactory<WW, RR> factory,
                                                      Observable<Host> hostStream,
                                                      EventLoopGroup eventLoopGroup,
                                                      Class<? extends Channel> channelClass) {
        return new ClientState<>(hostStream, factory, detachedPipeline, eventLoopGroup, channelClass);
    }

    private static DetachedChannelPipeline newChannelPipeline(TailHandlerFactory thf) {
        return new DetachedChannelPipeline(thf)
                .addLast(WriteTransformer.getName(), new Func0<ChannelHandler>() {
                    @Override
                    public ChannelHandler call() {
                        return new WriteTransformer();
                    }
                });
    }

    public Bootstrap newBootstrap() {
        final Bootstrap nettyBootstrap = new Bootstrap().group(eventLoopGroup)
                                                        .channel(channelClass)
                                                        .option(ChannelOption.AUTO_READ, false);// by default do not read content unless asked.

        for (Entry<ChannelOption<?>, Object> optionEntry : options.entrySet()) {
            // Type is just for safety for user of ClientState, internally in Bootstrap, types are thrown on the floor.
            @SuppressWarnings("unchecked")
            ChannelOption<Object> key = (ChannelOption<Object>) optionEntry.getKey();
            nettyBootstrap.option(key, optionEntry.getValue());
        }

        nettyBootstrap.handler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                ch.pipeline().addLast(ClientChannelActiveBufferingHandler.getName(),
                                      new ChannelActivityBufferingHandler());
            }
        });
        return nettyBootstrap;
    }

    public DetachedChannelPipeline unsafeDetachedPipeline() {
        return detachedPipeline;
    }

    public Map<ChannelOption<?>, Object> unsafeChannelOptions() {
        return options;
    }

    public ClientState<W, R> channelProviderFactory(ChannelProviderFactory factory) {
        return new ClientState<>(this, factory);
    }

    public ClientState<W, R> secure(Func1<ByteBufAllocator, SSLEngine> sslEngineFactory) {
        return secure(new DefaultSslCodec(sslEngineFactory));
    }

    public ClientState<W, R> secure(SSLEngine sslEngine) {
        return secure(new DefaultSslCodec(sslEngine));
    }

    public ClientState<W, R> secure(SslCodec sslCodec) {
        return new ClientState<>(this, sslCodec);
    }

    public ClientState<W, R> unsafeSecure() {
        return secure(new DefaultSslCodec(new Func1<ByteBufAllocator, SSLEngine>() {
            @Override
            public SSLEngine call(ByteBufAllocator allocator) {
                try {
                    return SslContextBuilder.forClient()
                                            .trustManager(InsecureTrustManagerFactory.INSTANCE)
                                            .build()
                                            .newEngine(allocator);
                } catch (Exception e) {
                    throw Exceptions.propagate(e);
                }
            }
        }));
    }

    private <WW, RR> ClientState<WW, RR> copy() {
        TailHandlerFactory newTail = new TailHandlerFactory(isSecure);
        return new ClientState<>(this, detachedPipeline.copy(newTail), isSecure);
    }

    public ConnectionProviderFactory<W, R> getFactory() {
        return factory;
    }

    public Observable<Host> getHostStream() {
        return hostStream;
    }

    public ChannelProviderFactory getChannelProviderFactory() {
        return channelProviderFactory;
    }

    @SuppressWarnings("unchecked")
    private <WW, RR> ClientState<WW, RR> cast() {
        return (ClientState<WW, RR>) this;
    }

    protected static class TailHandlerFactory implements Action1<ChannelPipeline> {

        private final boolean isSecure;

        public TailHandlerFactory(boolean isSecure) {
            this.isSecure = isSecure;
        }

        @Override
        public void call(ChannelPipeline pipeline) {
            ClientConnectionToChannelBridge.addToPipeline(pipeline, isSecure);
        }
    }

    protected static EventLoopGroup defaultEventloopGroup() {
        return RxNetty.getRxEventLoopProvider().globalClientEventLoop(true);
    }

    protected static Class<? extends AbstractChannel> defaultSocketChannelClass() {
        return RxNetty.isUsingNativeTransport() ? EpollSocketChannel.class : NioSocketChannel.class;
    }

    /**
     * Clients construct the pipeline, outside of the {@link ChannelInitializer} through {@link ChannelProvider}.
     * Thus channel registration and activation events may be lost due to a race condition when the channel is active
     * before the pipeline is configured.
     * This handler buffers, the channel events till the time, a subscriber appears for channel establishment.
     */
    private static class ChannelActivityBufferingHandler extends ChannelInboundHandlerAdapter {

        private enum State {
            Initialized,
            Registered,
            Active,
            Inactive,
            ChannelSubscribed
        }

        private State state = State.Initialized;

        /**
         * Unregistered state will hide the active/inactive state, hence this is a different flag.
         */
        private boolean unregistered;

        @Override
        public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
            if (State.ChannelSubscribed == state) {
                super.channelRegistered(ctx);
            } else {
                state = State.Registered;
            }
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
            if (State.ChannelSubscribed == state) {
                super.channelUnregistered(ctx);
            } else {
                unregistered = true;
            }
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            if (State.ChannelSubscribed == state) {
                super.channelActive(ctx);
            } else {
                state = State.Active;
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            if (State.ChannelSubscribed == state) {
                super.channelInactive(ctx);
            } else {
                state = State.Inactive;
            }
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof ChannelSubscriberEvent) {
                final State existingState = state;
                state = State.ChannelSubscribed;
                super.userEventTriggered(ctx, evt);
                final ChannelPipeline pipeline = ctx.channel().pipeline();
                switch (existingState) {
                case Initialized:
                    break;
                case Registered:
                    pipeline.fireChannelRegistered();
                    break;
                case Active:
                    pipeline.fireChannelRegistered();
                    pipeline.fireChannelActive();
                    break;
                case Inactive:
                    pipeline.fireChannelRegistered();
                    pipeline.fireChannelActive();
                    pipeline.fireChannelInactive();
                    break;
                case ChannelSubscribed:
                    // Duplicate event, ignore.
                    break;
                }

                if (unregistered) {
                    pipeline.fireChannelUnregistered();
                }
            } else {
                super.userEventTriggered(ctx, evt);
            }
        }
    }
}
