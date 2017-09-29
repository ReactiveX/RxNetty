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
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import io.netty.channel.Channel;
import io.netty.channel.pool.ChannelPool;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.SucceededFuture;
import io.reactivex.MaybeEmitter;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.netty.NettyContext;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.options.ClientOptions;
import io.reactivex.processors.PublishProcessor;
import org.reactivestreams.Publisher;

/**
 * @param <CHANNEL> the channel type
 *
 * @author Stephane Maldini
 */
final class PooledClientContextHandler<CHANNEL extends Channel>
		extends ContextHandler<CHANNEL>
		implements GenericFutureListener<Future<CHANNEL>> {

	final ClientOptions clientOptions;
	final boolean               secure;
	final ChannelPool           pool;
	final PublishProcessor<Void> onReleaseEmitter;

	volatile Future<CHANNEL> future;

	static final AtomicReferenceFieldUpdater<PooledClientContextHandler, Future> FUTURE =
			AtomicReferenceFieldUpdater.newUpdater(PooledClientContextHandler.class,
					Future.class,
					"future");

	static final Future DISPOSED = new SucceededFuture<>(null, null);

	PooledClientContextHandler(ChannelOperations.OnNew<CHANNEL> channelOpFactory,
			ClientOptions options,
			MaybeEmitter<NettyContext> sink,
			LoggingHandler loggingHandler,
			boolean secure,
			SocketAddress providedAddress,
			ChannelPool pool) {
		super(channelOpFactory, options, sink, loggingHandler, providedAddress);
		this.clientOptions = options;
		this.secure = secure;
		this.pool = pool;
		this.onReleaseEmitter = PublishProcessor.create();
	}

	@Override
	public void fireContextActive(NettyContext context) {
		if (!fired) {
			fired = true;
			if (context != null) {
				sink.onSuccess(context);
			}
			else {
				sink.onComplete();
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void setFuture(Future<?> future) {
		ObjectHelper.requireNonNull(future, "future");

		Future<CHANNEL> f;
		for (; ; ) {
			f = this.future;

			if (f == DISPOSED) {
				sink.onComplete();
				return;
			}

			if (FUTURE.compareAndSet(this, f, future)) {
				break;
			}
		}
		((Future<CHANNEL>) future).addListener(this);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void terminateChannel(Channel channel) {
		release((CHANNEL) channel);
	}

	@Override
	public void operationComplete(Future<CHANNEL> future) throws Exception {
		if (future.isCancelled()) {
			return;
		}

		if (DISPOSED == this.future) {
			if (future.isSuccess()) {
				disposeOperationThenRelease(future.get());
			}
			sink.onComplete();
			return;
		}

		if (!future.isSuccess()) {
			if (future.cause() != null) {
				fireContextError(future.cause());
			}
			else {
				fireContextError(new AbortedException("error while acquiring connection"));
			}
			return;
		}

		CHANNEL c = future.get();

		if (c.eventLoop()
		     .inEventLoop()) {
			connectOrAcquire(c);
		}
		else {
			c.eventLoop()
			 .execute(() -> connectOrAcquire(c));
		}
	}

	@Override
	protected Publisher<Void> onCloseOrRelease(Channel channel) {
		return onReleaseEmitter;
	}

	@SuppressWarnings("unchecked")
	final void connectOrAcquire(CHANNEL c) {
		if (DISPOSED == this.future) {
			disposeOperationThenRelease(c);
			sink.onComplete();
			return;
		}

		ChannelOperationsHandler op = c.pipeline()
		                               .get(ChannelOperationsHandler.class);

		if (op == null) {
			c.closeFuture()
			 .addListener(ff -> release(c));
			return;
		}
		if (!c.isActive()) {
			release(c);
			setFuture(pool.acquire());
			return;
		}
		if (createOperations(c, null) == null) {
			setFuture(pool.acquire());
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void cancel() {
		Future<CHANNEL> f = FUTURE.getAndSet(this, DISPOSED);
		if (f == null || f == DISPOSED) {
			return;
		}
		if (!f.isDone()) {
			return;
		}

		try {
			CHANNEL c = f.get();

			if (!c.eventLoop()
			      .inEventLoop()) {
				c.eventLoop()
				 .execute(() -> disposeOperationThenRelease(c));

			}
			else {
				disposeOperationThenRelease(c);
			}

		}
		catch (Exception e) {
			onReleaseEmitter.onError(e);
		}
	}

	final void disposeOperationThenRelease(CHANNEL c) {
		ChannelOperations<?, ?> ops = ChannelOperations.get(c);
		//defer to operation dispose if present
		if (ops != null) {
			ops.inbound.cancel();
			return;
		}

		release(c);
	}

	final void release(CHANNEL c) {
		if (!RxNetty.isPersistent(c) && c.isActive()) {
			c.close();
		}

		pool.release(c)
		    .addListener(f -> {
			    if (f.isSuccess()) {
				    onReleaseEmitter.onComplete();
			    }
			    else {
				    onReleaseEmitter.onError(f.cause());
			    }
		    });

	}

	@Override
	protected void doDropped(Channel channel) {
		cancel();
		fireContextError(new AbortedException("Channel has been dropped"));
	}

	@Override
	protected void doPipeline(Channel ch) {
		//do not add in channelCreated pool
	}

	@Override
	protected Entry<String, Integer> getSNI() {
		if (providedAddress instanceof InetSocketAddress) {
			InetSocketAddress ipa = (InetSocketAddress) providedAddress;
			return new SimpleImmutableEntry<>(ipa.getHostName(), ipa.getPort());
		}
		return null;
	}
}
