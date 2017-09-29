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

package io.reactivex.netty.options;

import java.net.SocketAddress;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.AbstractBootstrap;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.AttributeKey;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Predicate;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.netty.NettyContext;
import io.reactivex.netty.NettyInbound;
import io.reactivex.netty.resources.LoopResources;

/**
 * A common connector builder with low-level connection options including sslContext, tcp
 * configuration, channel init handlers.
 *
 * @param <BOOTSTRAP> A Netty {@link Bootstrap} type
 * @param <SO> A NettyOptions subclass
 *
 * @author Stephane Maldini
 * @author Violeta Georgieva
 */
public abstract class NettyOptions<BOOTSTRAP extends AbstractBootstrap<BOOTSTRAP, ?>, SO extends NettyOptions<BOOTSTRAP, SO>>
		implements Callable<BOOTSTRAP> {

	/**
	 * The default port for rxnetty servers. Defaults to 12012 but can be tuned via
	 * the {@code PORT} <b>environment variable</b>.
	 */
	public static final int     DEFAULT_PORT         =
			System.getenv("PORT") != null ? Integer.parseInt(System.getenv("PORT")) :
					12012;

	private final BOOTSTRAP                        bootstrapTemplate;
	private final boolean                          preferNative;
	private final LoopResources                    loopResources;
	private final SslContext                       sslContext;
	private final long                             sslHandshakeTimeoutMillis;
	private final long                             sslCloseNotifyFlushTimeoutMillis;
	private final long                             sslCloseNotifyReadTimeoutMillis;
	protected final Consumer<? super Channel> afterChannelInit;
	protected final Consumer<? super NettyContext> afterNettyContextInit;
	private final Predicate<? super Channel> onChannelInit;

	protected NettyOptions(NettyOptions.Builder<BOOTSTRAP, SO, ?> builder) {
		this.bootstrapTemplate = builder.bootstrapTemplate;
		this.preferNative = builder.preferNative;
		this.loopResources = builder.loopResources;
		this.sslContext = builder.sslContext;
		this.sslHandshakeTimeoutMillis = builder.sslHandshakeTimeoutMillis;
		this.sslCloseNotifyFlushTimeoutMillis = builder.sslCloseNotifyFlushTimeoutMillis;
		this.sslCloseNotifyReadTimeoutMillis = builder.sslCloseNotifyReadTimeoutMillis;
		this.afterNettyContextInit = builder.afterNettyContextInit;
		this.onChannelInit = builder.onChannelInit;

		Consumer<? super Channel> afterChannel = builder.afterChannelInit;
		if (afterChannel != null && builder.channelGroup != null) {
			this.afterChannelInit = e -> {
				builder.channelGroup.add(e);
				afterChannel.accept(e);
			};
		}
		else if (afterChannel != null) {
			this.afterChannelInit = afterChannel;
		}
		else if (builder.channelGroup != null) {
			this.afterChannelInit = builder.channelGroup::add;
		}
		else {
			this.afterChannelInit = null;
		}
	}

	/**
	 * Returns the callback for post {@link Channel} initialization and rxnetty
	 * pipeline handlers registration.
	 *
	 * @return the post channel setup handler
	 * @see #onChannelInit()
	 * @see #afterNettyContextInit()
	 */
	public final Consumer<? super Channel> afterChannelInit() {
		return afterChannelInit;
	}

	/**
	 * Returns the callback for post {@link Channel} initialization, rxnetty
	 * pipeline handlers registration and {@link NettyContext} initialisation.
	 *
	 * @return the post {@link NettyContext} setup handler
	 * @see #onChannelInit()
	 * @see #afterChannelInit()
	 */
	public final Consumer<? super NettyContext> afterNettyContextInit() {
		return this.afterNettyContextInit;
	}

	/**
	 * Checks if these options denotes secured communication, ie. a {@link javax.net.ssl.SSLContext}
	 * was set other than the default one.
	 *
	 * @return true if the options denote secured communication (SSL is active)
	 */
	public boolean isSecure() {
		return sslContext != null;
	}

	/**
	 * Return a copy of all options and references such as
	 * {@link NettyOptions.Builder#onChannelInit(Predicate)}. Further option uses on the returned builder will
	 * be fully isolated from this option builder.
	 *
	 * @return a new duplicated builder;
	 */
	public abstract SO duplicate();

	@Override
	public BOOTSTRAP call() {
		return bootstrapTemplate.clone();
	}

	/**
	 * Return a new eventual {@link SocketAddress}
	 *
	 * @return the supplied {@link SocketAddress} or null
	 */
	public abstract SocketAddress getAddress();

	/**
	 * Get the configured Loop Resources if any
	 *
	 * @return an eventual {@link LoopResources}
	 */
	public final LoopResources getLoopResources() {
		return loopResources;
	}

	/**
	 * Return a new eventual {@link SslHandler}, optionally with SNI activated
	 *
	 * @param allocator {@link ByteBufAllocator} to allocate for packet storage
	 * @param sniInfo {@link Entry} with hostname and port for SNI (any null will skip SNI).
	 * @return a new eventual {@link SslHandler} with SNI activated
	 */
	public final SslHandler getSslHandler(ByteBufAllocator allocator,
			Entry<String, Integer> sniInfo) {
		SslContext sslContext =
				this.sslContext == null ? defaultSslContext() : this.sslContext;

		if (sslContext == null) {
			return null;
		}

		ObjectHelper.requireNonNull(allocator, "allocator");
		SslHandler sslHandler;
		if (sniInfo != null && sniInfo.getKey() != null && sniInfo.getValue() != null) {
			sslHandler = sslContext.newHandler(allocator, sniInfo.getKey(), sniInfo.getValue());
		}
		else {
			sslHandler = sslContext.newHandler(allocator);
		}
		sslHandler.setHandshakeTimeoutMillis(sslHandshakeTimeoutMillis);
		sslHandler.setCloseNotifyFlushTimeoutMillis(sslCloseNotifyFlushTimeoutMillis);
		sslHandler.setCloseNotifyReadTimeoutMillis(sslCloseNotifyReadTimeoutMillis);
		return sslHandler;
	}

	/**
	 * Returns the predicate used to validate each {@link Channel} post initialization
	 * (but before rxnetty pipeline handlers have been registered).
	 *
	 * @return The channel validator
	 * @see #afterChannelInit()
	 * @see #afterNettyContextInit()
	 */
	public final Predicate<? super Channel> onChannelInit() {
		return this.onChannelInit;
	}

	/**
	 * Is this option preferring native loops (epoll)
	 *
	 * @return true if this option is preferring native loops (epoll)
	 */
	public final boolean preferNative() {
		return this.preferNative;
	}

	/**
	 * Returns the SslContext
	 *
	 * @return the SslContext
	 */
	public final SslContext sslContext() {
		return this.sslContext;
	}

	/**
	 * Returns the SSL handshake timeout in millis
	 *
	 * @return the SSL handshake timeout in millis
	 */
	public final long sslHandshakeTimeoutMillis() {
		return this.sslHandshakeTimeoutMillis;
	}

	/**
	 * Returns the SSL close_notify flush timeout in millis
	 *
	 * @return the SSL close_notify flush timeout in millis
	 */
	public final long sslCloseNotifyFlushTimeoutMillis() {
		return this.sslCloseNotifyFlushTimeoutMillis;
	}

	/**
	 * Returns the SSL close_notify read timeout in millis
	 *
	 * @return the SSL close_notify read timeout in millis
	 */
	public final long sslCloseNotifyReadTimeoutMillis() {
		return this.sslCloseNotifyReadTimeoutMillis;
	}

	/**
	 * Default Ssl context if none configured or null;
	 *
	 * @return a default {@link SslContext}
	 */
	protected SslContext defaultSslContext() {
		return null;
	}

	public String asSimpleString() {
		return this.asDetailedString();
	}

	public String asDetailedString() {
		return "bootstrapTemplate=" + bootstrapTemplate +
				", sslHandshakeTimeoutMillis=" + sslHandshakeTimeoutMillis +
				", sslCloseNotifyFlushTimeoutMillis=" + sslCloseNotifyFlushTimeoutMillis +
				", sslCloseNotifyReadTimeoutMillis=" + sslCloseNotifyReadTimeoutMillis +
				", sslContext=" + sslContext +
				", preferNative=" + preferNative +
				", afterChannelInit=" + afterChannelInit +
				", onChannelInit=" + onChannelInit +
				", loopResources=" + loopResources;
	}

	@Override
	public String toString() {
		return "NettyOptions{" + asDetailedString() + "}";
	}

	public static abstract class Builder<BOOTSTRAP extends AbstractBootstrap<BOOTSTRAP, ?>,
			SO extends NettyOptions<BOOTSTRAP, SO>, BUILDER extends Builder<BOOTSTRAP, SO, BUILDER>>
			implements Callable<BUILDER>{

		private static final boolean DEFAULT_NATIVE =
				Boolean.parseBoolean(System.getProperty("io.reactivex.netty.epoll", "true"));

		protected BOOTSTRAP bootstrapTemplate;
		private boolean                        preferNative                     = DEFAULT_NATIVE;
		private LoopResources                  loopResources                    = null;
		private ChannelGroup                   channelGroup                     = null;
		private SslContext                     sslContext                       = null;
		private long                           sslHandshakeTimeoutMillis        = 10000L;
		private long                           sslCloseNotifyFlushTimeoutMillis = 3000L;
		private long                           sslCloseNotifyReadTimeoutMillis  = 0L;
		private Consumer<? super Channel>      afterChannelInit                 = null;
		private Consumer<? super NettyContext> afterNettyContextInit            = null;
		private Predicate<? super Channel>     onChannelInit                    = null;

		protected Builder(BOOTSTRAP bootstrapTemplate) {
			this.bootstrapTemplate = bootstrapTemplate;
			defaultNettyOptions(this.bootstrapTemplate);
		}

		private void defaultNettyOptions(AbstractBootstrap<?, ?> bootstrap) {
			bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
		}

		/**
		 * Attribute default attribute to the future {@link Channel} connection. They will
		 * be available via {@link NettyInbound#attr(AttributeKey)}.
		 *
		 * @param key the attribute key
		 * @param value the attribute value
		 * @param <T> the attribute type
		 * @return {@code this}
		 * @see Bootstrap#attr(AttributeKey, Object)
		 */
		public <T> BUILDER attr(AttributeKey<T> key, T value) {
			this.bootstrapTemplate.attr(key, value);
			return call();
		}

		/**
		 * Set a {@link ChannelOption} value for low level connection settings like
		 * SO_TIMEOUT or SO_KEEPALIVE. This will apply to each new channel from remote
		 * peer.
		 *
		 * @param key the option key
		 * @param value the option value
		 * @param <T> the option type
		 * @return {@code this}
		 * @see Bootstrap#option(ChannelOption, Object)
		 */
		public <T> BUILDER option(ChannelOption<T> key, T value) {
			this.bootstrapTemplate.option(key, value);
			return call();
		}

		/**
		 * Set the preferred native option. Determine if epoll should be used if available.
		 *
		 * @param preferNative Should the connector prefer native (epoll) if available
		 * @return {@code this}
		 */
		public final BUILDER preferNative(boolean preferNative) {
			this.preferNative = preferNative;
			return call();
		}

		/**
		 * Provide an {@link EventLoopGroup} supplier.
		 * Note that server might call it twice for both their selection and io loops.
		 *
		 * @param channelResources a selector accepting native runtime expectation and
		 * returning an eventLoopGroup
		 * @return {@code this}
		 */
		public final BUILDER loopResources(LoopResources channelResources) {
			this.loopResources = ObjectHelper.requireNonNull(channelResources, "loopResources");
			return call();
		}

		public final boolean isLoopAvailable() {
			return this.loopResources != null;
		}

		/**
		 * Provide a shared {@link EventLoopGroup} each Connector handler.
		 *
		 * @param eventLoopGroup an eventLoopGroup to share
		 * @return {@code this}
		 */
		public final BUILDER eventLoopGroup(EventLoopGroup eventLoopGroup) {
			ObjectHelper.requireNonNull(eventLoopGroup, "eventLoopGroup");
			return loopResources(preferNative -> eventLoopGroup);
		}

		/**
		 * Provide a {@link ChannelGroup} for each active remote channel will be held in the
		 * provided group.
		 *
		 * @param channelGroup a {@link ChannelGroup} to monitor remote channel
		 * @return {@code this}
		 */
		public final BUILDER channelGroup(ChannelGroup channelGroup) {
			this.channelGroup = ObjectHelper.requireNonNull(channelGroup, "channelGroup");
			//the channelGroup being set, afterChannelInit will be augmented to add
			//each channel to the group, when actual Options are constructed
			return call();
		}

		/**
		 * Set the options to use for configuring SSL. Setting this to {@code null} means
		 * don't use SSL at all (the default).
		 *
		 * @param sslContext The context to set when configuring SSL
		 * @return {@code this}
		 */
		public final BUILDER sslContext(SslContext sslContext) {
			this.sslContext = sslContext;
			return call();
		}

		/**
		 * Set the options to use for configuring SSL handshake timeout. Default to 10000 ms.
		 *
		 * @param sslHandshakeTimeout The timeout duration
		 * @param unit The {@link TimeUnit}
		 * @return {@code this}
		 */
		public final BUILDER sslHandshakeTimeout(long sslHandshakeTimeout, TimeUnit unit) {
			ObjectHelper.requireNonNull(sslHandshakeTimeout, "sslHandshakeTimeout");
			return sslHandshakeTimeoutMillis(unit.toMillis(sslHandshakeTimeout));
		}

		/**
		 * Set the options to use for configuring SSL handshake timeout. Default to 10000 ms.
		 *
		 * @param sslHandshakeTimeoutMillis The timeout in milliseconds
		 * @return {@code this}
		 */
		public final BUILDER sslHandshakeTimeoutMillis(long sslHandshakeTimeoutMillis) {
			if(sslHandshakeTimeoutMillis < 0L){
				throw new IllegalArgumentException("ssl handshake timeout must be positive," +
						" was: "+sslHandshakeTimeoutMillis);
			}
			this.sslHandshakeTimeoutMillis = sslHandshakeTimeoutMillis;
			return call();
		}

		/**
		 * Set the options to use for configuring SSL close_notify flush timeout. Default to 3000 ms.
		 *
		 * @param sslCloseNotifyFlushTimeout The timeout duration
		 * @param unit The {@link TimeUnit}
		 * @return {@code this}
		 */
		public final BUILDER sslCloseNotifyFlushTimeout(long sslCloseNotifyFlushTimeout, TimeUnit unit) {
			ObjectHelper.requireNonNull(sslCloseNotifyFlushTimeout, "sslCloseNotifyFlushTimeout");
			return sslCloseNotifyFlushTimeoutMillis(unit.toMillis(sslCloseNotifyFlushTimeout));
		}


		/**
		 * Set the options to use for configuring SSL close_notify flush timeout. Default to 3000 ms.
		 *
		 * @param sslCloseNotifyFlushTimeoutMillis The timeout in milliseconds
		 *
		 * @return {@code this}
		 */
		public final BUILDER sslCloseNotifyFlushTimeoutMillis(long sslCloseNotifyFlushTimeoutMillis) {
			if (sslCloseNotifyFlushTimeoutMillis < 0L) {
				throw new IllegalArgumentException("ssl close_notify flush timeout must be positive," +
						" was: " + sslCloseNotifyFlushTimeoutMillis);
			}
			this.sslCloseNotifyFlushTimeoutMillis = sslCloseNotifyFlushTimeoutMillis;
			return call();
		}


		/**
		 * Set the options to use for configuring SSL close_notify read timeout. Default to 0 ms.
		 *
		 * @param sslCloseNotifyReadTimeout The timeout duration
		 * @param unit The {@link TimeUnit}
		 * @return {@code this}
		 */
		public final BUILDER sslCloseNotifyReadTimeout(long sslCloseNotifyReadTimeout, TimeUnit unit) {
			ObjectHelper.requireNonNull(sslCloseNotifyReadTimeout, "sslCloseNotifyReadTimeout");
			return sslCloseNotifyFlushTimeoutMillis(unit.toMillis(sslCloseNotifyReadTimeout));
		}


		/**
		 * Set the options to use for configuring SSL close_notify read timeout. Default to 0 ms.
		 *
		 * @param sslCloseNotifyReadTimeoutMillis The timeout in milliseconds
		 *
		 * @return {@code this}
		 */
		public final BUILDER sslCloseNotifyReadTimeoutMillis(long sslCloseNotifyReadTimeoutMillis) {
			if (sslCloseNotifyReadTimeoutMillis < 0L) {
				throw new IllegalArgumentException("ssl close_notify read timeout must be positive," +
						" was: " + sslCloseNotifyReadTimeoutMillis);
			}
			this.sslCloseNotifyReadTimeoutMillis = sslCloseNotifyReadTimeoutMillis;
			return call();
		}

		/**
		 * Setup a callback called after each {@link Channel} initialization, once
		 * rxnetty pipeline handlers have been registered.
		 *
		 * @param afterChannelInit the post channel setup handler
		 * @return {@code this}
		 * @see #onChannelInit(Predicate)
		 * @see #afterNettyContextInit(Consumer)
		 */
		public final BUILDER afterChannelInit(Consumer<? super Channel> afterChannelInit) {
			this.afterChannelInit = ObjectHelper.requireNonNull(afterChannelInit, "afterChannelInit");
			return call();
		}

		/**
		 * Setup a {@link Predicate} for each {@link Channel} initialization that can be
		 * used to prevent the Channel's registration.
		 *
		 * @param onChannelInit predicate to accept or reject the newly created Channel
		 * @return {@code this}
		 * @see #afterChannelInit(Consumer)
		 * @see #afterNettyContextInit(Consumer)
		 */
		public final BUILDER onChannelInit(Predicate<? super Channel> onChannelInit) {
			this.onChannelInit = ObjectHelper.requireNonNull(onChannelInit, "onChannelInit");
			return call();
		}

		/**
		 * Setup a callback called after each {@link Channel} initialization, once the
		 * rxnetty pipeline handlers have been registered and the {@link NettyContext}
		 * is available.
		 *
		 * @param afterNettyContextInit the post channel setup handler
		 * @return {@code this}
		 * @see #onChannelInit(Predicate)
		 * @see #afterChannelInit(Consumer)
		 */
		public final BUILDER afterNettyContextInit(Consumer<? super NettyContext> afterNettyContextInit) {
			this.afterNettyContextInit = ObjectHelper.requireNonNull(afterNettyContextInit, "afterNettyContextInit");
			return call();
		}

		/**
		 * Fill the builder with attribute values from the provided options.
		 *
		 * @param options The instance from which to copy values
		 * @return {@code this}
		 */
		public BUILDER from(SO options) {
			this.bootstrapTemplate = options.call();
			this.preferNative = options.preferNative();
			this.loopResources = options.getLoopResources();
			this.sslContext = options.sslContext();
			this.sslHandshakeTimeoutMillis = options.sslHandshakeTimeoutMillis();
			this.sslCloseNotifyFlushTimeoutMillis = options.sslCloseNotifyFlushTimeoutMillis();
			this.sslCloseNotifyReadTimeoutMillis = options.sslCloseNotifyReadTimeoutMillis();
			this.afterChannelInit = options.afterChannelInit();
			this.onChannelInit = options.onChannelInit();
			this.afterNettyContextInit = options.afterNettyContextInit();
			return call();
		}

		@Override
		public abstract BUILDER call();
	}
}
