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

package io.reactivex.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.functions.ObjectHelper;
import org.reactivestreams.Publisher;

/**
 * Constant for names used when adding/removing {@link io.netty.channel.ChannelHandler}.
 *
 * Order of placement :
 * <p>
 * {@code
 * -> proxy ? [ProxyHandler]
 * -> ssl ? [SslHandler]
 * -> ssl & trace log ? [SslLoggingHandler]
 * -> ssl ? [SslReader]
 * -> log ? [LoggingHandler]
 * -> http ? [HttpCodecHandler]
 * -> http ws ? [HttpAggregator]
 * -> http server  ? [HttpServerHandler]
 * -> onWriteIdle ? [OnChannelWriteIdle]
 * -> onReadIdle ? [OnChannelReadIdle]
 * -> http form/multipart ? [ChunkedWriter]
 * => [ReactiveBridge]
 * }
 *
 * @author Stephane Maldini
 * @since 0.6
 */
public final class NettyPipeline {

	public static String LEFT = "rxnetty.left.";
	public static String RIGHT = "rxnetty.right.";

	public static String SslHandler         = LEFT + "sslHandler";
	public static String SslReader          = LEFT + "sslReader";
	public static String SslLoggingHandler  = LEFT + "sslLoggingHandler";
	public static String ProxyHandler       = LEFT + "proxyHandler";
	public static String ReactiveBridge     = RIGHT + "reactiveBridge";
	public static String HttpEncoder        = LEFT + "httpEncoder";
	public static String HttpDecoder        = LEFT + "httpDecoder";
	public static String HttpDecompressor   = LEFT + "decompressor";
	public static String HttpCompressor     = LEFT + "compressor";
	public static String HttpAggregator     = LEFT + "httpAggregator";
	public static String HttpServerHandler  = LEFT + "httpServerHandler";
	public static String OnChannelWriteIdle = LEFT + "onChannelWriteIdle";
	public static String OnChannelReadIdle  = LEFT + "onChannelReadIdle";
	public static String ChunkedWriter      = LEFT + "chunkedWriter";
	public static String LoggingHandler     = LEFT + "loggingHandler";
	public static String CompressionHandler = LEFT + "compressionHandler";

	/**
	 * A builder for sending strategy, similar prefixed methods being mutually exclusive
	 * (flushXxx, prefetchXxx, requestXxx).
	 */
	public interface SendOptions {

		/**
		 * Make the underlying channel flush on a terminated {@link Publisher} (default).
		 *
		 * @return this builder
		 */
		SendOptions flushOnBoundary();

		/**
		 * Make the underlying channel flush item by item.
		 *
		 * @return this builder
		 */
		SendOptions flushOnEach();


	}

	/**
	 * An container transporting a new {@link SendOptions}, eventually bound to a
	 * specific {@link Publisher}
	 */
	public static final class SendOptionsChangeEvent {

		final Consumer<? super SendOptions> configurator;
		final Publisher<?>                  source;

		SendOptionsChangeEvent(Consumer<? super SendOptions> configurator,
				Publisher<?> source) {
			this.configurator = ObjectHelper.requireNonNull(configurator, "configurator");
			this.source = source;
		}

		/**
		 * Return the send configurator
		 *
		 * @return the send configurator
		 */
		public Consumer<? super SendOptions> configurator() {
			return configurator;
		}
	}

	/**
	 * Create a new {@link ChannelInboundHandler} that will invoke
	 * {@link BiConsumer#accept} on
	 * {@link ChannelInboundHandler#channelRead(ChannelHandlerContext, Object)}.
	 *
	 * @param handler the channel-read callback
	 *
	 * @return a marking event used when a netty connector handler terminates
	 */
	public static ChannelInboundHandler inboundHandler(BiConsumer<? super ChannelHandlerContext, Object> handler) {
		return new RxNetty.ExtractorHandler(handler);
	}

	/**
	 * Return a marking event used when a netty connector handler terminates
	 *
	 * @return a marking event used when a netty connector handler terminates
	 */
	public static Object handlerTerminatedEvent() {
		return RxNetty.TERMINATED;
	}

	public static Object responseCompressionEvent() {
		return RxNetty.RESPONSE_COMPRESSION_EVENT;
	}
}
