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

package io.reactivex.netty.tcp;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.NetUtil;
import io.reactivex.Flowable;
import io.reactivex.MaybeEmitter;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.netty.NettyConnector;
import io.reactivex.netty.NettyHandler;
import io.reactivex.netty.NettyInbound;
import io.reactivex.netty.channel.ChannelOperations;
import io.reactivex.netty.channel.ContextHandler;
import io.reactivex.netty.options.ClientOptions;
import io.reactivex.netty.resources.PoolResources;
import org.reactivestreams.Publisher;
import io.reactivex.netty.NettyContext;
import io.reactivex.netty.NettyOutbound;
import io.reactivex.netty.options.NettyOptions;

/**
 * A TCP client connector.
 *
 * @author Stephane Maldini
 * @author Violeta Georgieva
 */
public class TcpClient extends NettyConnector<NettyInbound, NettyOutbound> {

	/**
	 * Bind a new TCP client to the localhost on {@link NettyOptions#DEFAULT_PORT port 12012}.
	 * <p> The type of emitted data or received data is {@link ByteBuf}
	 *
	 * @return a new {@link TcpClient}
	 */
	public static TcpClient create() {
		return create(NetUtil.LOCALHOST.getHostAddress());
	}

	/**
	 * Bind a new TCP client to the specified connect address and {@link NettyOptions#DEFAULT_PORT port 12012}.
	 * <p> The type of emitted data or received data is {@link ByteBuf}
	 *
	 * @param bindAddress the address to connect to on port 12012
	 * @return a new {@link TcpClient}
	 */
	public static TcpClient create(String bindAddress) {
		return create(bindAddress, NettyOptions.DEFAULT_PORT);
	}

	/**
	 * Bind a new TCP client to "localhost" on the the specified port.
	 * <p> The type of emitted data or received data is {@link ByteBuf}
	 *
	 * @param port the port to connect to on "localhost"
	 * @return a new {@link TcpClient}
	 */
	public static TcpClient create(int port) {
		return create(NetUtil.LOCALHOST.getHostAddress(), port);
	}

	/**
	 * Bind a new TCP client to the specified connect address and port.
	 * <p> The type of emitted data or received data is {@link ByteBuf}
	 *
	 * @param bindAddress the address to connect to
	 * @param port the port to connect to
	 * @return a new {@link TcpClient}
	 */
	public static TcpClient create(String bindAddress, int port) {
		return create(opts -> opts.host(bindAddress).port(port));
	}

	/**
	 * Bind a new TCP client to the specified connect address and port.
	 * <p> The type of emitted data or received data is {@link ByteBuf}
	 *
	 * @param options {@link ClientOptions} configuration input
	 * @return a new {@link TcpClient}
	 */
	public static TcpClient create(Consumer<? super ClientOptions.Builder<?>> options) {
		return builder().options(options).build();
	}

	/**
	 * Creates a builder for {@link TcpClient TcpClient}
	 *
	 * @return a new TcpClient builder
	 */
	public static TcpClient.Builder builder() {
		return new TcpClient.Builder();
	}

	final ClientOptions options;

	protected TcpClient(TcpClient.Builder builder) {
		ClientOptions.Builder<?> clientOptionsBuilder = ClientOptions.builder();
		if (builder.options != null) {
			try {
				builder.options.accept(clientOptionsBuilder);
			} catch (Exception e) {
				throw Exceptions.propagate(e);
			}
		}
		if (!clientOptionsBuilder.isLoopAvailable()) {
			clientOptionsBuilder.loopResources(TcpResources.get());
		}
		if (!clientOptionsBuilder.isPoolAvailable() && !clientOptionsBuilder.isPoolDisabled()) {
			clientOptionsBuilder.poolResources(TcpResources.get());
		}
		this.options = clientOptionsBuilder.build();
	}

	protected TcpClient(ClientOptions options) {
		this.options = ObjectHelper.requireNonNull(options, "options");
	}

	@Override
	public final Flowable<? extends NettyContext> newHandler(BiFunction<? super NettyInbound, ? super NettyOutbound, ? extends Publisher<Void>> handler) {
		ObjectHelper.requireNonNull(handler, "handler");
		return newHandler(handler, null, true, null);
	}

	/**
	 * Get a copy of the {@link ClientOptions} currently in effect.
	 *
	 * @return the client options
	 */
	public final ClientOptions options() {
		return this.options.duplicate();
	}

	@Override
	public String toString() {
		return "TcpClient: " + options.asSimpleString();
	}

	/**
	 * @param handler
	 * @param address
	 * @param secure
	 * @param onSetup
	 *
	 * @return a new Mono to connect on subscribe
	 */
	protected Flowable<NettyContext> newHandler(BiFunction<? super NettyInbound, ? super NettyOutbound, ? extends Publisher<Void>> handler,
			InetSocketAddress address,
			boolean secure,
			Consumer<? super Channel> onSetup) {

		final BiFunction<? super NettyInbound, ? super NettyOutbound, ? extends Publisher<Void>>
				targetHandler =
				null == handler ? ChannelOperations.noopHandler() : handler;

		return NettyHandler.create(sink -> {
			SocketAddress remote = address != null ? address : options.getAddress();

			ChannelPool pool = null;

			PoolResources poolResources = options.getPoolResources();
			if (poolResources != null) {
				pool = poolResources.selectOrCreate(remote, options,
						doHandler(null, sink, secure, remote, null, null),
						options.getLoopResources().onClient(options.preferNative()));
			}

			ContextHandler<SocketChannel> contextHandler =
					doHandler(targetHandler, sink, secure, remote, pool, onSetup);
			sink.setCancellable(contextHandler);

			if (pool == null) {
				Bootstrap b = options.call();
				b.remoteAddress(remote);
				b.handler(contextHandler);
				contextHandler.setFuture(b.connect());
			}
			else {
				contextHandler.setFuture(pool.acquire());
			}
		});
	}

	/**
	 * Create a {@link ContextHandler} for {@link Bootstrap#handler()}
	 *
	 * @param handler user provided in/out handler
	 * @param sink user provided bind handler
	 * @param secure if operation should be secured
	 * @param pool if channel pool
	 * @param onSetup if operation has local setup callback
	 *
	 * @return a new {@link ContextHandler}
	 */
	protected ContextHandler<SocketChannel> doHandler(BiFunction<? super NettyInbound, ? super NettyOutbound, ? extends Publisher<Void>> handler,
			MaybeEmitter<NettyContext> sink,
			boolean secure,
			SocketAddress providedAddress,
			ChannelPool pool,
			Consumer<? super Channel> onSetup) {
		return ContextHandler.newClientContext(sink,
				options,
				loggingHandler,
				secure,
				providedAddress,
				pool,
				handler == null ? EMPTY :
						(ch, c, msg) -> ChannelOperations.bind(ch, handler, c));
	}

	protected static final ChannelOperations.OnNew EMPTY = (a,b,c) -> null;

	static final LoggingHandler loggingHandler = new LoggingHandler(TcpClient.class);


	public static final class Builder {
		private Consumer<? super ClientOptions.Builder<?>> options;

		private Builder() {
		}

		/**
		 * The options for the client, including address and port.
		 *
		 * @param options the options for the client, including address and port.
		 * @return {@code this}
		 */
		public final Builder options(Consumer<? super ClientOptions.Builder<?>> options) {
			this.options = ObjectHelper.requireNonNull(options, "options");
			return this;
		}

		public TcpClient build() {
			return new TcpClient(this);
		}
	}
}
