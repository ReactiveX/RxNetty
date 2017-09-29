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

import java.util.concurrent.TimeUnit;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AttributeKey;
import io.reactivex.Flowable;
import io.reactivex.functions.Action;
import io.reactivex.functions.BiConsumer;
import io.reactivex.internal.functions.ObjectHelper;
import org.reactivestreams.Publisher;

/**
 * Internal helpers for rxnetty contracts
 *
 * @author Stephane Maldini
 */
public final class RxNetty {

	static final AttributeKey<Boolean> PERSISTENT_CHANNEL = AttributeKey.newInstance("PERSISTENT_CHANNEL");

	/**
	 * A common implementation for the {@link NettyContext#addHandlerLast(String, ChannelHandler)}
	 * method that can be reused by other implementors.
	 * <p>
	 * This implementation will look for rxnetty added handlers on the right hand side of
	 * the pipeline, provided they are identified with the {@link NettyPipeline#RIGHT}
	 * prefix, and add the handler just before the first of these.
	 *
	 * @param context the {@link NettyContext} on which to add the decoder.
	 * @param name the name of the decoder.
	 * @param handler the decoder to add before the final rxnetty-specific handlers.
	 * @see NettyContext#addHandlerLast(String, ChannelHandler).
	 */
	static void addHandlerBeforeRxNettyEndHandlers(NettyContext context, String
			name, ChannelHandler handler) {
		ObjectHelper.requireNonNull(name, "name");
		ObjectHelper.requireNonNull(handler, "handler");

		Channel channel = context.channel();
		boolean exists = channel.pipeline().get(name) != null;

		if (exists) {
			return;
		}

		//we need to find the correct position
		String before = null;
		for (String s : channel.pipeline().names()) {
			if (s.startsWith(NettyPipeline.RIGHT)) {
				before = s;
				break;
			}
		}

		if (before == null) {
			channel.pipeline().addLast(name, handler);
		}
		else {
			channel.pipeline().addBefore(NettyPipeline.ReactiveBridge, name, handler);
		}

		registerForClose(shouldCleanupOnClose(channel),  name, context);
	}

	/**
	 * A common implementation for the {@link NettyContext#addHandlerFirst(String, ChannelHandler)}
	 * method that can be reused by other implementors.
	 * <p>
	 * This implementation will look for rxnetty added handlers on the left hand side of
	 * the pipeline, provided they are identified with the {@link NettyPipeline#LEFT}
	 * prefix, and add the handler just after the last of these.
	 *
	 * @param context the {@link NettyContext} on which to add the decoder.
	 * @param name the name of the encoder.
	 * @param handler the encoder to add after the initial rxnetty-specific handlers.
	 * @see NettyContext#addHandlerFirst(String, ChannelHandler)
	 */
	static void addHandlerAfterRxNettyCodecs(NettyContext context, String
			name,
			ChannelHandler handler) {
		ObjectHelper.requireNonNull(name, "name");
		ObjectHelper.requireNonNull(handler, "handler");

		Channel channel = context.channel();
		boolean exists = channel.pipeline().get(name) != null;

		if (exists) {
			return;
		}

		//we need to find the correct position
		String after = null;
		for (String s : channel.pipeline().names()) {
			if (s.startsWith(NettyPipeline.LEFT)) {
				after = s;
			}
		}

		if (after == null) {
			channel.pipeline().addFirst(name, handler);
		}
		else {
			channel.pipeline().addAfter(after, name, handler);
		}

		registerForClose(shouldCleanupOnClose(channel), name, context);
	}

	static void registerForClose(boolean shouldCleanupOnClose,
			String name,
			NettyContext context) {
		if (!shouldCleanupOnClose) return;
		context.onClose(() -> context.removeHandler(name));
	}

	static void removeHandler(Channel channel, String name){
		if (channel.isActive() && channel.pipeline()
		                                 .context(name) != null) {
			channel.pipeline()
			       .remove(name);
		}
	}

	static void replaceHandler(Channel channel, String name, ChannelHandler handler){
		if (channel.isActive() && channel.pipeline()
		                                 .context(name) != null) {
			channel.pipeline()
			       .replace(name, name, handler);
		}
	}

	/**
	 * Determines if user-provided handlers registered on the given channel should
	 * automatically be registered for removal through a {@link NettyContext#onClose(Action)}
	 * (or similar on close hook). This depends on the
	 * {@link RxNetty#isPersistent(Channel)} ()}
	 * attribute.
	 */
	public static boolean shouldCleanupOnClose(Channel channel) {
		boolean registerForClose = true;
		if (!isPersistent(channel)) {
			registerForClose = false;
		}
		return registerForClose;
	}

	/**
	 * Return false if it will force a close on terminal protocol events thus defeating
	 * any pooling strategy
	 * Return true (default) if it will release on terminal protocol events thus
	 * keeping alive the channel if possible.
	 *
	 * @return whether or not the underlying {@link Channel} will be closed on terminal
	 * handler event
	 */
	public static boolean isPersistent(Channel channel) {
		return !channel.hasAttr(PERSISTENT_CHANNEL) ||
				channel.attr(PERSISTENT_CHANNEL).get();
	}

	RxNetty(){
	}

	static final class TerminatedHandlerEvent {
		@Override
		public String toString() {
			return "[Handler Terminated]";
		}
	}

	static final class ResponseWriteCompleted {
		@Override
		public String toString() {
			return "[Response Write Completed]";
		}
	}

	/**
	 * An appending write that delegates to its origin context and append the passed
	 * publisher after the origin success if any.
	 */
	static final class OutboundThen implements NettyOutbound {

		final NettyContext sourceContext;
		final Flowable<Void> thenFlowable;

		OutboundThen(NettyOutbound source, Publisher<Void> thenPublisher) {
			this.sourceContext = source.context();

			Flowable<Void> parentFlowable = source.then();

			if (parentFlowable == Flowable.<Void>empty()) {
				this.thenFlowable = Flowable.fromPublisher(thenPublisher);
			}
			else {
				this.thenFlowable = parentFlowable.ignoreElements().andThen(thenPublisher);
			}
		}

		@Override
		public NettyContext context() {
			return sourceContext;
		}

		@Override
		public Flowable<Void> then() {
			return thenFlowable;
		}
	}

	final static class OutboundIdleStateHandler extends IdleStateHandler {

		final Runnable onWriteIdle;

		OutboundIdleStateHandler(long idleTimeout, Runnable onWriteIdle) {
			super(0, idleTimeout, 0, TimeUnit.MILLISECONDS);
			this.onWriteIdle = onWriteIdle;
		}

		@Override
		protected void channelIdle(ChannelHandlerContext ctx,
				IdleStateEvent evt) throws Exception {
			if (evt.state() == IdleState.WRITER_IDLE) {
				onWriteIdle.run();
			}
			super.channelIdle(ctx, evt);
		}
	}

	final static class InboundIdleStateHandler extends IdleStateHandler {

		final Runnable onReadIdle;

		InboundIdleStateHandler(long idleTimeout, Runnable onReadIdle) {
			super(idleTimeout, 0, 0, TimeUnit.MILLISECONDS);
			this.onReadIdle = onReadIdle;
		}

		@Override
		protected void channelIdle(ChannelHandlerContext ctx,
				IdleStateEvent evt) throws Exception {
			if (evt.state() == IdleState.READER_IDLE) {
				onReadIdle.run();
			}
			super.channelIdle(ctx, evt);
		}
	}

	static final Object TERMINATED                 = new TerminatedHandlerEvent();
	static final Object RESPONSE_COMPRESSION_EVENT = new ResponseWriteCompleted();

	/**
	 * A handler that can be used to extract {@link ByteBuf} out of {@link ByteBufHolder},
	 * optionally also outputting additional messages
	 *
	 * @author Stephane Maldini
	 * @author Simon Baslé
	 */
	@ChannelHandler.Sharable
	static final class ExtractorHandler extends ChannelInboundHandlerAdapter {

		final BiConsumer<? super ChannelHandlerContext, Object> extractor;

		ExtractorHandler(BiConsumer<? super ChannelHandlerContext, Object> extractor) {
			this.extractor = ObjectHelper.requireNonNull(extractor, "extractor");
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			extractor.accept(ctx, msg);
		}
	}
}
