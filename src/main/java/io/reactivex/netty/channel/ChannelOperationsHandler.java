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

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.buffer.EmptyByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel.Unsafe;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPromise;
import io.netty.channel.FileRegion;
import io.netty.util.ReferenceCountUtil;
import io.reactivex.Flowable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.BiPredicate;
import io.reactivex.internal.queue.SpscLinkedArrayQueue;
import io.reactivex.internal.subscriptions.ScalarSubscription;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.BackpressureHelper;
import io.reactivex.netty.NettyPipeline;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import io.reactivex.netty.NettyOutbound;

/**
 * Netty {@link io.netty.channel.ChannelDuplexHandler} implementation that bridge data
 * via an IPC {@link NettyOutbound}
 *
 * @author Stephane Maldini
 */
final class ChannelOperationsHandler extends ChannelDuplexHandler
		implements NettyPipeline.SendOptions, ChannelFutureListener {

	final PublisherSender                inner;
	final BiConsumer<?, ? super ByteBuf> encoder;
	final int                            prefetch;
	final ContextHandler<?>              originContext;

	/**
	 * Cast the supplied queue (SpscLinkedArrayQueue) to use its atomic dual-insert
	 * backed by {@link BiPredicate#test}
	 **/
	SpscLinkedArrayQueue<Object>        pendingWrites;
	ChannelHandlerContext               ctx;
	boolean                             flushOnEach;

	long                                pendingBytes;
	ContextHandler<?>                   lastContext;

	private Unsafe                      unsafe;

	volatile boolean innerActive;
	volatile boolean removed;
	volatile int     wip;

	@SuppressWarnings("unchecked")
	ChannelOperationsHandler(ContextHandler<?> contextHandler) {
		this.inner = new PublisherSender(this);
		this.prefetch = 32;
		this.encoder = NOOP_ENCODER;
		this.lastContext = null;
		this.originContext = contextHandler; // only set if parent context is closable,
		// pool will usually fetch context via lastContext()
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		originContext.createOperations(ctx.channel(), null);
	}

	@Override
	final public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		try {
			ChannelOperations<?, ?> ops = ChannelOperations.get(ctx.channel());
			if (ops != null) {
				ops.onHandlerTerminate();
			}
			else {
				if (lastContext != null) {
					lastContext.terminateChannel(ctx.channel());
					lastContext.fireContextError(new AbortedException());
				}
			}
		}
		catch (Throwable err) {
			Exceptions.throwIfFatal(err);
			exceptionCaught(ctx, err);
		}
	}

	@Override
	final public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		if (msg == null || msg == Unpooled.EMPTY_BUFFER || msg instanceof EmptyByteBuf) {
			return;
		}
		try {
			ChannelOperations<?, ?> ops = ChannelOperations.get(ctx.channel());
			if (ops != null) {
				ops.onInboundNext(ctx, msg);
			}
			else {
				ReferenceCountUtil.release(msg);
			}
		}
		catch (Throwable err) {
			Exceptions.throwIfFatal(err);
			exceptionCaught(ctx, err);
			ReferenceCountUtil.safeRelease(msg);
		}
	}

	@Override
	public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
		drain();
	}

	@Override
	final public void exceptionCaught(ChannelHandlerContext ctx, Throwable err)
			throws Exception {
		Exceptions.throwIfFatal(err);
		ChannelOperations<?, ?> ops = ChannelOperations.get(ctx.channel());
		if (ops != null) {
			ops.onInboundError(err);
		}
		else {
			if (lastContext != null) {
				lastContext.terminateChannel(ctx.channel());
				lastContext.fireContextError(err);
			}
		}
	}

	@Override
	public void flush(ChannelHandlerContext ctx) throws Exception {
		drain();
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		this.ctx = ctx;
		this.unsafe = ctx.channel().unsafe();
		inner.request(prefetch);
	}

	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
		if (!removed) {
			removed = true;

			inner.cancel();
			drain();
		}
	}

	@Override
	final public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
			throws Exception {
		if (evt == NettyPipeline.handlerTerminatedEvent()) {
			ContextHandler<?> c = lastContext;
			if (c == null){
				return;
			}
			lastContext = null;
			c.terminateChannel(ctx.channel());
			return;
		}
		if (evt instanceof NettyPipeline.SendOptionsChangeEvent) {
			((NettyPipeline.SendOptionsChangeEvent) evt).configurator()
			                                            .accept(this);
			return;
		}

		ctx.fireUserEventTriggered(evt);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
			throws Exception {
		if (pendingWrites == null) {
			this.pendingWrites = new SpscLinkedArrayQueue<>(Flowable.bufferSize());
		}

		if (!pendingWrites.offer(promise, msg)) {
			promise.setFailure(new IllegalStateException("Send Queue full?!"));
		}
	}

	@Override
	public NettyPipeline.SendOptions flushOnBoundary() {
		flushOnEach = false;
		return this;
	}

	@Override
	public NettyPipeline.SendOptions flushOnEach() {
		flushOnEach = true;
		return this;
	}

	@Override
	public void operationComplete(ChannelFuture future) throws Exception {
		if (future.isSuccess()) {
			inner.request(1L);
		}
	}

	ChannelFuture doWrite(Object msg, ChannelPromise promise, PublisherSender inner) {
		if (flushOnEach || //fastpath
				inner == null && pendingWrites.isEmpty() || //last drained element
				!ctx.channel()
				    .isWritable() //force flush if write buffer full
				) {
			pendingBytes = 0L;

			ChannelFuture future = ctx.writeAndFlush(msg, promise);
			
			if (inner != null && !hasPendingWriteBytes()) {
				inner.justFlushed = true;
			}
			return future;
		}
		else {
			if (msg instanceof ByteBuf) {
				pendingBytes =
						BackpressureHelper.addCap(pendingBytes, ((ByteBuf) msg).readableBytes());
			}
			else if (msg instanceof ByteBufHolder) {
				pendingBytes = BackpressureHelper.addCap(pendingBytes,
						((ByteBufHolder) msg).content()
						                     .readableBytes());
			}
			else if (msg instanceof FileRegion) {
				pendingBytes = BackpressureHelper.addCap(pendingBytes, ((FileRegion) msg).count());
			}
			if (inner != null && inner.justFlushed) {
				inner.justFlushed = false;
			}
			ChannelFuture future = ctx.write(msg, promise);
			if (!ctx.channel().isWritable()) {
				pendingBytes = 0L;
				ctx.flush();
				if (inner != null && !hasPendingWriteBytes()) {
					inner.justFlushed = true;
				}
			}
			return future;
		}
	}

	void discard() {
		for (; ; ) {
			if (pendingWrites == null || pendingWrites.isEmpty()) {
				return;
			}

			ChannelPromise promise;
			Object v = pendingWrites.poll();

			try {
				promise = (ChannelPromise) v;
			}
			catch (Throwable e) {
				ctx.fireExceptionCaught(e);
				return;
			}
			v = pendingWrites.poll();
			ReferenceCountUtil.release(v);
			promise.tryFailure(new AbortedException("Connection has been closed"));
		}
	}

	@SuppressWarnings("unchecked")
	void drain() {
		if (WIP.getAndIncrement(this) == 0) {

			for (; ; ) {
				if (removed) {
					discard();
					return;
				}

				if (pendingWrites == null || innerActive || !ctx.channel()
				                                                .isWritable()) {
					if (!ctx.channel().isWritable() && hasPendingWriteBytes()) {
						ctx.flush();
					}
					if (WIP.decrementAndGet(this) == 0) {
						break;
					}
					continue;
				}

				ChannelFuture future;
				Object v = pendingWrites.poll();

				try {
					future = (ChannelFuture) v;
				}
				catch (Throwable e) {
					ctx.fireExceptionCaught(e);
					return;
				}

				boolean empty = future == null;

				if (empty) {
					if (WIP.decrementAndGet(this) == 0) {
						break;
					}
					continue;
				}

				v = pendingWrites.poll();

				if (!innerActive && v == PublisherSender.PENDING_WRITES) {
					boolean last = pendingWrites.isEmpty();
					if (!future.isDone() && hasPendingWriteBytes()) {
						ctx.flush();
						if (!future.isDone() && hasPendingWriteBytes()) {
							pendingWrites.offer(future, v);
						}
					}
					if (last && WIP.decrementAndGet(this) == 0) {
						break;
					}
				}
				else if (future instanceof ChannelPromise) {
					ChannelPromise promise = (ChannelPromise) future;
					if (v instanceof Publisher) {
						Publisher<?> p = (Publisher<?>) v;
	
						if (p instanceof Callable) {
							@SuppressWarnings("unchecked") Callable<?> supplier =
									(Callable<?>) p;
	
							Object vr;
	
							try {
								vr = supplier.call();
							}
							catch (Throwable e) {
								promise.setFailure(e);
								continue;
							}
	
							if (vr == null) {
								promise.setSuccess();
								continue;
							}
	
							if (inner.unbounded) {
								doWrite(vr, promise, null);
							}
							else {
								innerActive = true;
								inner.promise = promise;
								inner.onSubscribe(new ScalarSubscription<>(inner, vr));
							}
						}
						else {
							innerActive = true;
							inner.promise = promise;
							p.subscribe(inner);
						}
					}
					else {
						doWrite(v, promise, null);
					}
				}
			}
		}
	}

	private boolean hasPendingWriteBytes() {
		// On close the outboundBuffer is made null. After that point
		// adding messages and flushes to outboundBuffer is not allowed.
		ChannelOutboundBuffer outBuffer = this.unsafe.outboundBuffer();
		return outBuffer != null && outBuffer.totalPendingWriteBytes() > 0;
	}

	static final class PublisherSender
			implements Subscriber<Object>, Subscription, ChannelFutureListener {

		final ChannelOperationsHandler parent;

		volatile Subscription missedSubscription;
		volatile long         missedRequested;
		volatile long         missedProduced;
		volatile int          wip;

		boolean        inactive;
		boolean        justFlushed;
		/**
		 * The current outstanding request amount.
		 */
		long           requested;
		boolean        unbounded;
		/**
		 * The current subscription which may null if no Subscriptions have been set.
		 */
		Subscription   actual;
		long           produced;
		ChannelPromise promise;
		ChannelFuture  lastWrite;

		PublisherSender(ChannelOperationsHandler parent) {
			this.parent = parent;
		}

		@Override
		public final void cancel() {
			if (!inactive) {
				inactive = true;

				drain();
			}
		}

		@Override
		public void onComplete() {
			if (parent.ctx.pipeline().get(NettyPipeline.CompressionHandler) != null) {
				parent.ctx.pipeline()
				          .fireUserEventTriggered(NettyPipeline.responseCompressionEvent());
			}
			long p = produced;
			ChannelFuture f = lastWrite;
			parent.innerActive = false;

			if (p != 0L) {
				produced = 0L;
				produced(p);
				if (!justFlushed) {
					if (parent.ctx.channel()
					              .isActive()) {
						parent.ctx.flush();
						if (!parent.hasPendingWriteBytes()) {
						    justFlushed = true;
						}
					}
					else {
						promise.setFailure(new AbortedException("Connection has been closed"));
						return;
					}
				}
			}

			if (f != null) {
				if (!f.isDone() && parent.hasPendingWriteBytes()) {
					parent.pendingWrites.offer(f, PENDING_WRITES);
				}
				f.addListener(this);
			}
			else {
				promise.setSuccess();
				parent.drain();
			}
		}

		@Override
		public void onError(Throwable t) {
			long p = produced;
			ChannelFuture f = lastWrite;
			parent.innerActive = false;

			if (p != 0L) {
				produced = 0L;
				produced(p);
				if (parent.ctx.channel()
				              .isActive()) {
					parent.ctx.flush();
					if (!parent.hasPendingWriteBytes()) {
					    justFlushed = true;
					}
				}
				else {
					promise.setFailure(new AbortedException("Connection has been closed"));
					return;
				}
			}

			if (f != null) {
				if (!f.isDone() && parent.hasPendingWriteBytes()) {
					parent.pendingWrites.offer(f, PENDING_WRITES);
				}
				f.addListener(this);
			}
			else {
				promise.setFailure(t);
				parent.drain();
			}
		}

		@Override
		public void onNext(Object t) {
			produced++;

			lastWrite = parent.doWrite(t, parent.ctx.newPromise(), this);
			if (parent.ctx.channel()
			              .isWritable()) {
				request(1L);
			}
			else {
				lastWrite.addListener(parent);
			}
		}

		@Override
		public void onSubscribe(Subscription s) {
			if (inactive) {
				s.cancel();
				return;
			}

			if (wip == 0 && WIP.compareAndSet(this, 0, 1)) {
				actual = s;

				long r = requested;

				if (WIP.decrementAndGet(this) != 0) {
					drainLoop();
				}

				if (r != 0L) {
					s.request(r);
				}

				return;
			}

			MISSED_SUBSCRIPTION.set(this, s);
			drain();
		}

		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			if (future.isSuccess()) {
				promise.setSuccess();
			}
			else {
				promise.setFailure(future.cause());
			}
		}

		@Override
		public final void request(long n) {
			if (SubscriptionHelper.validate(n)) {
				if (unbounded) {
					return;
				}
				if (wip == 0 && WIP.compareAndSet(this, 0, 1)) {
					long r = requested;

					if (r != Long.MAX_VALUE) {
						r = BackpressureHelper.addCap(r, n);
						requested = r;
						if (r == Long.MAX_VALUE) {
							unbounded = true;
						}
					}
					Subscription a = actual;

					if (WIP.decrementAndGet(this) != 0) {
						drainLoop();
					}

					if (a != null) {
						a.request(n);
					}

					return;
				}

				addCap(MISSED_REQUESTED, n);

				drain();
			}
		}

		final void drain() {
			if (WIP.getAndIncrement(this) != 0) {
				return;
			}
			drainLoop();
		}

		final void drainLoop() {
			int missed = 1;

			long requestAmount = 0L;
			Subscription requestTarget = null;

			for (; ; ) {

				Subscription ms = missedSubscription;

				if (ms != null) {
					ms = MISSED_SUBSCRIPTION.getAndSet(this, null);
				}

				long mr = missedRequested;
				if (mr != 0L) {
					mr = MISSED_REQUESTED.getAndSet(this, 0L);
				}

				long mp = missedProduced;
				if (mp != 0L) {
					mp = MISSED_PRODUCED.getAndSet(this, 0L);
				}

				Subscription a = actual;

				if (inactive) {
					if (a != null) {
						a.cancel();
						actual = null;
					}
					if (ms != null) {
						ms.cancel();
					}
				}
				else {
					long r = requested;
					if (r != Long.MAX_VALUE) {
						long u = BackpressureHelper.addCap(r, mr);

						if (u != Long.MAX_VALUE) {
							long v = u - mp;
							if (v < 0L) {
								v = 0;
							}
							r = v;
						}
						else {
							r = u;
						}
						requested = r;
					}

					if (ms != null) {
						actual = ms;
						if (r != 0L) {
							requestAmount = BackpressureHelper.addCap(requestAmount, r);
							requestTarget = ms;
						}
					}
					else if (mr != 0L && a != null) {
						requestAmount = BackpressureHelper.addCap(requestAmount, mr);
						requestTarget = a;
					}
				}

				missed = WIP.addAndGet(this, -missed);
				if (missed == 0) {
					if (requestAmount != 0L) {
						requestTarget.request(requestAmount);
					}
					return;
				}
			}
		}

		final void produced(long n) {
			if (unbounded) {
				return;
			}
			if (wip == 0 && WIP.compareAndSet(this, 0, 1)) {
				long r = requested;

				if (r != Long.MAX_VALUE) {
					long u = r - n;
					if (u < 0L) {
						u = 0;
					}
					requested = u;
				}
				else {
					unbounded = true;
				}

				if (WIP.decrementAndGet(this) == 0) {
					return;
				}

				drainLoop();

				return;
			}

			addCap(MISSED_PRODUCED, n);

			drain();
		}

		final long addCap(AtomicLongFieldUpdater<PublisherSender> updater, long toAdd) {
			long r, u;
			for (;;) {
				r = updater.get(this);
				if (r == Long.MAX_VALUE) {
					return Long.MAX_VALUE;
				}
				u = BackpressureHelper.addCap(r, toAdd);
				if (updater.compareAndSet(this, r, u)) {
					return r;
				}
			}
		}

		@SuppressWarnings("rawtypes")
		static final AtomicReferenceFieldUpdater<PublisherSender, Subscription>
				                                                MISSED_SUBSCRIPTION =
				AtomicReferenceFieldUpdater.newUpdater(PublisherSender.class,
						Subscription.class,
						"missedSubscription");
		@SuppressWarnings("rawtypes")
		static final AtomicLongFieldUpdater<PublisherSender>    MISSED_REQUESTED    =
				AtomicLongFieldUpdater.newUpdater(PublisherSender.class,
						"missedRequested");
		@SuppressWarnings("rawtypes")
		static final AtomicLongFieldUpdater<PublisherSender>    MISSED_PRODUCED     =
				AtomicLongFieldUpdater.newUpdater(PublisherSender.class,
						"missedProduced");
		@SuppressWarnings("rawtypes")
		static final AtomicIntegerFieldUpdater<PublisherSender> WIP                 =
				AtomicIntegerFieldUpdater.newUpdater(PublisherSender.class, "wip");

		private static final PendingWritesOnCompletion PENDING_WRITES = new PendingWritesOnCompletion();
	}

	@SuppressWarnings("rawtypes")
	static final AtomicIntegerFieldUpdater<ChannelOperationsHandler> WIP =
			AtomicIntegerFieldUpdater.newUpdater(ChannelOperationsHandler.class, "wip");

	static final BiConsumer<?, ? super ByteBuf> NOOP_ENCODER = (a, b) -> {
	};

	private static final class PendingWritesOnCompletion {
		@Override
		public String toString() {
			return "[Pending Writes on Completion]";
		}
	}
}
