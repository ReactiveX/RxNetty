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

package io.reactivex.netty.udp;

import java.net.SocketAddress;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.socket.DatagramChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.NetUtil;
import io.reactivex.Flowable;
import io.reactivex.MaybeEmitter;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.netty.NettyConnector;
import io.reactivex.netty.options.ClientOptions;
import io.reactivex.netty.resources.DefaultLoopResources;
import org.reactivestreams.Publisher;
import io.reactivex.netty.NettyHandler;
import io.reactivex.netty.NettyContext;
import io.reactivex.netty.channel.ChannelOperations;
import io.reactivex.netty.channel.ContextHandler;
import io.reactivex.netty.options.NettyOptions;
import io.reactivex.netty.resources.LoopResources;

/**
 * A UDP client connector.
 *
 * @author Stephane Maldini
 * @author Violeta Georgieva
 */
final public class UdpClient extends NettyConnector<UdpInbound, UdpOutbound> {

	/**
	 * Bind a new UDP client to the "localhost" address and {@link NettyOptions#DEFAULT_PORT port 12012}.
	 * Handlers will run on the same thread they have been receiving IO events.
	 * <p> The type of emitted data or received data is {@link ByteBuf}
	 *
	 * @return a new {@link UdpClient}
	 */
	public static UdpClient create() {
		return create(NetUtil.LOCALHOST.getHostAddress());
	}

	/**
	 * Bind a new UDP client to the given bind address and {@link NettyOptions#DEFAULT_PORT port 12012}.
	 * Handlers will run on the same thread they have been receiving IO events.
	 * <p> The type of emitted data or received data is {@link ByteBuf}
	 *
	 * @param bindAddress bind address (e.g. "127.0.0.1") to create the client on default port
	 * @return a new {@link UdpClient}
	 */
	public static UdpClient create(String bindAddress) {
		return create(bindAddress, NettyOptions.DEFAULT_PORT);
	}

	/**
	 * Bind a new UDP client to the "localhost" address and specified port.
	 * Handlers will run on the same thread they have been receiving IO events.
	 * <p> The type of emitted data or received data is {@link ByteBuf}
	 *
	 * @param port the port to listen on the localhost bind address
	 * @return a new {@link UdpClient}
	 */
	public static UdpClient create(int port) {
		return create(NetUtil.LOCALHOST.getHostAddress(), port);
	}

	/**
	 * Bind a new UDP client to the given bind address and port.
	 * Handlers will run on the same thread they have been receiving IO events.
	 * <p> The type of emitted data or received data is {@link ByteBuf}
	 *
	 * @param bindAddress bind address (e.g. "127.0.0.1") to create the client on the
	 * passed port
	 * @param port the port to listen on the passed bind address
	 * @return a new {@link UdpClient}
	 */
	public static UdpClient create(String bindAddress, int port) {
		return create(opts -> opts.host(bindAddress).port(port));
	}

	/**
	 * Bind a new UDP client to the bind address and port provided through the options.
	 * Handlers will run on the same thread they have been receiving IO events.
	 * <p> The type of emitted data or received data is {@link ByteBuf}
	 *
	 * @param options the configurator
	 * @return a new {@link UdpClient}
	 */
	public static UdpClient create(Consumer<? super ClientOptions.Builder<?>> options) {
		return builder().options(options).build();
	}

	/**
	 * Creates a builder for {@link UdpClient UdpClient}
	 *
	 * @return a new UdpClient builder
	 */
	public static UdpClient.Builder builder() {
		return new UdpClient.Builder();
	}

	final UdpClientOptions options;

	private UdpClient(UdpClient.Builder builder) {
		UdpClientOptions.Builder clientOptionsBuilder = UdpClientOptions.builder();
		if (builder.options != null) {
			try {
				builder.options.accept(clientOptionsBuilder);
			} catch (Exception e) {
				throw Exceptions.propagate(e);
			}
		}
		if (!clientOptionsBuilder.isLoopAvailable()) {
			clientOptionsBuilder.loopResources(DEFAULT_UDP_LOOPS);
		}
		this.options = clientOptionsBuilder.build();
	}

	@Override
	public Flowable<? extends NettyContext> newHandler(BiFunction<? super UdpInbound, ? super UdpOutbound, ? extends Publisher<Void>> handler) {
		final BiFunction<? super UdpInbound, ? super UdpOutbound, ? extends Publisher<Void>>
				targetHandler =
				null == handler ? ChannelOperations.noopHandler() : handler;

		return NettyHandler.create(sink -> {
			Bootstrap b = options.call();
			SocketAddress adr = options.getAddress();
			if(adr == null){
				sink.onError(new NullPointerException("Provided ClientOptions do not " +
						"define any address to bind to "));
				return;
			}
			b.localAddress(adr);
			ContextHandler<DatagramChannel> c = doHandler(targetHandler, sink, adr);
			b.handler(c);
			c.setFuture(b.bind());
		});
	}

	/**
	 * Create a {@link ContextHandler} for {@link Bootstrap#handler()}
	 *
	 * @param handler user provided in/out handler
	 * @param sink user provided bind handler
	 *
	 * @return a new {@link ContextHandler}
	 */
	protected ContextHandler<DatagramChannel> doHandler(BiFunction<? super UdpInbound, ? super UdpOutbound, ? extends Publisher<Void>> handler,
			MaybeEmitter<NettyContext> sink,
			SocketAddress providedAddress) {
		return ContextHandler.newClientContext(sink,
				options,
				loggingHandler,
				false,
				providedAddress,
				(ch, c, msg) -> UdpOperations.bind(ch, handler, c));
	}


	static final int DEFAULT_UDP_THREAD_COUNT = Integer.parseInt(System.getProperty(
			"io.reactivex.netty.udp.ioThreadCount",
			"" + Math.max(Runtime.getRuntime().availableProcessors(), 4)));

	static final LoggingHandler loggingHandler = new LoggingHandler(UdpClient.class);

	static final LoopResources DEFAULT_UDP_LOOPS =
			DefaultLoopResources.create("udp", DEFAULT_UDP_THREAD_COUNT, true);

	public static final class Builder {
		private Consumer<? super UdpClientOptions.Builder> options;

		private Builder() {
		}

		/**
		 * The options for the client, including address and port.
		 *
		 * @param options the options for the client, including address and port.
		 * @return {@code this}
		 */
		public final Builder options(Consumer<? super UdpClientOptions.Builder> options) {
			this.options = ObjectHelper.requireNonNull(options, "options");
			return this;
		}

		public UdpClient build() {
			return new UdpClient(this);
		}
	}
}
