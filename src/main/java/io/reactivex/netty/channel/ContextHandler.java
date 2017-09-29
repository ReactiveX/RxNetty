/*
 * Copyright (c) 2011-2017 Pivotal Software Inc, All Rights Reserved.
 * Modifications Copyright (c) 2017 RxNetty Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.reactivex.netty.channel;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map.Entry;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.pool.ChannelPool;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.reactivex.Maybe;
import io.reactivex.MaybeEmitter;
import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Cancellable;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.netty.NettyConnector;
import io.reactivex.netty.options.ClientOptions;
import io.reactivex.netty.options.ServerOptions;
import io.reactivex.plugins.RxJavaPlugins;
import org.reactivestreams.Publisher;
import io.reactivex.netty.NettyContext;
import io.reactivex.netty.NettyPipeline;
import io.reactivex.netty.options.NettyOptions;

/**
 * A one time-set channel pipeline callback to emit {@link NettyContext} state for clean
 * disposing. A {@link ContextHandler} is bound to a user-facing {@link MaybeEmitter}
 *
 * @param <CHANNEL> the channel type
 *
 * @author Stephane Maldini
 */
public abstract class ContextHandler<CHANNEL extends Channel>
		extends ChannelInitializer<CHANNEL> implements Cancellable, Consumer<Channel> {

	/**
	 * Create a new client context
	 *
	 * @param sink
	 * @param options
	 * @param loggingHandler
	 * @param secure
	 * @param channelOpFactory
	 * @param <CHANNEL>
	 *
	 * @return a new {@link ContextHandler} for clients
	 */
	public static <CHANNEL extends Channel> ContextHandler<CHANNEL> newClientContext(
			MaybeEmitter<NettyContext> sink,
			ClientOptions options,
			LoggingHandler loggingHandler,
			boolean secure,
			SocketAddress providedAddress,
			ChannelOperations.OnNew<CHANNEL> channelOpFactory) {
		return newClientContext(sink,
				options,
				loggingHandler,
				secure,
				providedAddress,
				null,
				channelOpFactory);
	}

	/**
	 * Create a new client context with optional pool support
	 *
	 * @param sink
	 * @param options
	 * @param loggingHandler
	 * @param secure
	 * @param providedAddress
	 * @param channelOpFactory
	 * @param pool
	 * @param <CHANNEL>
	 *
	 * @return a new {@link ContextHandler} for clients
	 */
	public static <CHANNEL extends Channel> ContextHandler<CHANNEL> newClientContext(
			MaybeEmitter<NettyContext> sink,
			ClientOptions options,
			LoggingHandler loggingHandler,
			boolean secure,
			SocketAddress providedAddress,
			ChannelPool pool, ChannelOperations.OnNew<CHANNEL> channelOpFactory) {
		if (pool != null) {
			return new PooledClientContextHandler<>(channelOpFactory,
					options,
					sink,
					loggingHandler,
					secure,
					providedAddress,
					pool);
		}
		return new ClientContextHandler<>(channelOpFactory,
				options,
				sink,
				loggingHandler,
				secure,
				providedAddress);
	}

	/**
	 * Create a new server context
	 *
	 * @param sink
	 * @param options
	 * @param loggingHandler
	 * @param channelOpFactory
	 *
	 * @return a new {@link ContextHandler} for servers
	 */
	public static ContextHandler<Channel> newServerContext(MaybeEmitter<NettyContext> sink,
			ServerOptions options,
			LoggingHandler loggingHandler,
			ChannelOperations.OnNew<Channel> channelOpFactory) {
		return new ServerContextHandler(channelOpFactory, options, sink, loggingHandler, options.getAddress());
	}

	final MaybeEmitter<NettyContext>           sink;
	final NettyOptions<?, ?>               options;
	final LoggingHandler                   loggingHandler;
	final SocketAddress                    providedAddress;
	final ChannelOperations.OnNew<CHANNEL> channelOpFactory;

	BiConsumer<ChannelPipeline, ContextHandler<Channel>> pipelineConfigurator;
	boolean                                              fired;
	boolean                                              autoCreateOperations;

	/**
	 * @param channelOpFactory
	 * @param options
	 * @param sink
	 * @param loggingHandler
	 * @param providedAddress the {@link InetSocketAddress} targeted by the operation
	 * associated with that handler (useable eg. for SNI), or null if unavailable.
	 */
	@SuppressWarnings("unchecked")
	protected ContextHandler(ChannelOperations.OnNew<CHANNEL> channelOpFactory,
			NettyOptions<?, ?> options,
			MaybeEmitter<NettyContext> sink,
			LoggingHandler loggingHandler,
			SocketAddress providedAddress) {
		this.channelOpFactory =
				ObjectHelper.requireNonNull(channelOpFactory, "channelOpFactory");
		this.options = options;
		this.sink = sink;
		this.loggingHandler = loggingHandler;
		this.autoCreateOperations = true;
		this.providedAddress = providedAddress;

	}

	/**
	 * Setup protocol specific handlers such as HTTP codec. Should be called before
	 * binding the context to any channel or bootstrap.
	 *
	 * @param pipelineConfigurator a configurator for extra codecs in the {@link
	 * ChannelPipeline}
	 *
	 * @return this context
	 */
	public final ContextHandler<CHANNEL> onPipeline(BiConsumer<ChannelPipeline, ContextHandler<Channel>> pipelineConfigurator) {
		this.pipelineConfigurator =
				ObjectHelper.requireNonNull(pipelineConfigurator, "pipelineConfigurator");
		return this;
	}

	/**
	 * Allow the {@link ChannelOperations} to be created automatically on pipeline setup
	 *
	 * @param autoCreateOperations should auto create {@link ChannelOperations}
	 *
	 * @return this context
	 */
	public final ContextHandler<CHANNEL> autoCreateOperations(boolean autoCreateOperations) {
		this.autoCreateOperations = autoCreateOperations;
		return this;
	}

	/**
	 * Return a new {@link ChannelOperations} or null if one of the two
	 * following conditions are not met:
	 * <ul>
	 * <li>{@link #autoCreateOperations(boolean)} is true
	 * </li>
	 * <li>The passed message is not null</li>
	 * </ul>
	 *
	 * @param channel the current {@link Channel}
	 * @param msg an optional message inbound, meaning the channel has already been
	 * started before
	 *
	 * @return a new {@link ChannelOperations}
	 */
	@SuppressWarnings("unchecked")
	public final ChannelOperations<?, ?> createOperations(Channel channel, Object msg) {

		if (autoCreateOperations || msg != null) {
			ChannelOperations<?, ?> op =
					channelOpFactory.create((CHANNEL) channel, this, msg);

			if (op != null) {
				ChannelOperations old = ChannelOperations.tryGetAndSet(channel, op);

				if (old != null) {
					return null;
				}

				if (this.options.afterNettyContextInit() != null) {
					try {
						this.options.afterNettyContextInit().accept(op.context());
					}
					catch (Throwable t) {
						RxJavaPlugins.onError(t);
					}
				}

				channel.pipeline()
				       .get(ChannelOperationsHandler.class).lastContext = this;

				channel.eventLoop().execute(op::onHandlerStart);
			}
			return op;
		}

		return null;
	}

	/**
	 * Trigger {@link MaybeEmitter#onSuccess(Object)} that will signal
	 * {@link NettyConnector#newHandler(BiFunction)} returned
	 * {@link Maybe} subscriber.
	 *
	 * @param context optional context to succeed the associated {@link MaybeEmitter}
	 */
	public abstract void fireContextActive(NettyContext context);

	/**
	 * Trigger {@link MaybeEmitter#onError(Throwable)} that will signal
	 * {@link NettyConnector#newHandler(BiFunction)} returned
	 * {@link Maybe} subscriber.
	 *
	 * @param t error to fail the associated {@link MaybeEmitter}
	 */
	public void fireContextError(Throwable t) {
		if (!fired) {
			fired = true;
			sink.onError(t);
		}
	}

	/**
	 * One-time only future setter
	 *
	 * @param future the connect/bind future to associate with and cancel on dispose
	 */
	public abstract void setFuture(Future<?> future);

	/**
	 * @param channel
	 */
	protected void doStarted(Channel channel) {
		//ignore
	}

	@Override
	protected void initChannel(CHANNEL ch) throws Exception {
		accept(ch);
	}

	/**
	 * Initialize pipeline
	 *
	 * @param ch channel to initialize
	 */
	protected abstract void doPipeline(Channel ch);

	@Override
	@SuppressWarnings("unchecked")
	public void accept(Channel channel) throws Exception {
		doPipeline(channel);
		if (options.onChannelInit() != null) {
			if (options.onChannelInit()
			           .test(channel)) {
				doDropped(channel);
				return;
			}
		}

		try {
			if (pipelineConfigurator != null) {
				pipelineConfigurator.accept(channel.pipeline(),
						(ContextHandler<Channel>) this);
			}
			channel.pipeline()
			       .addLast(NettyPipeline.ReactiveBridge,
					       new ChannelOperationsHandler(this));
		}
		finally {
			if (null != options.afterChannelInit()) {
				options.afterChannelInit()
				       .accept(channel);
			}
		}
	}

	/**
	 * @param channel
	 */
	protected void doDropped(Channel channel) {
		//ignore
	}

	/**
	 * Cleanly terminate a channel according to the current context handler type.
	 * Server might keep alive and recycle connections, pooled client will release and
	 * classic client will close.
	 *
	 * @param channel the channel to unregister
	 */
	protected void terminateChannel(Channel channel) {
		cancel();
	}

	@Override
	public abstract void cancel();

	protected Entry<String, Integer> getSNI() {
		return null; //will ignore SNI
	}

	/**
	 * Return a Publisher to signal onComplete on {@link Channel} close or release.
	 *
	 * @param channel the channel to monitor
	 *
	 * @return a Publisher to signal onComplete on {@link Channel} close or release.
	 */
	protected abstract Publisher<Void> onCloseOrRelease(Channel channel);

	static void addSslAndLogHandlers(NettyOptions<?, ?> options,
			ContextHandler<?> sink,
			LoggingHandler loggingHandler,
			boolean secure,
			Entry<String, Integer> sniInfo,
			ChannelPipeline pipeline) {
		SslHandler sslHandler = secure
				? options.getSslHandler(pipeline.channel().alloc(), sniInfo)
				: null;

		if (sslHandler != null) {
			pipeline.addFirst(NettyPipeline.SslHandler, sslHandler);
			pipeline.addAfter(NettyPipeline.SslHandler,
					NettyPipeline.SslReader,
					new SslReadHandler(sink));
		}
	}
}
