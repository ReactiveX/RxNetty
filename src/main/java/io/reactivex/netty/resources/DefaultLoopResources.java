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

package io.reactivex.netty.resources;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Future;
import io.reactivex.Flowable;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.netty.FutureFlowable;

/**
 * An adapted global eventLoop handler.
 *
 * @since 0.6
 */
public final class DefaultLoopResources extends AtomicLong implements LoopResources {

	/**
	 * Default worker thread count, fallback to available processor
	 */
	public static int DEFAULT_IO_WORKER_COUNT = Integer.parseInt(System.getProperty(
			"io.reactivex.netty.workerCount",
			"" + Math.max(Runtime.getRuntime()
					.availableProcessors(), 4)));
	/**
	 * Default selector thread count, fallback to -1 (no selector thread)
	 */
	public static int DEFAULT_IO_SELECT_COUNT = Integer.parseInt(System.getProperty(
			"io.reactivex.netty.selectCount",
			"" + -1));

	/**
	 * Create a delegating {@link EventLoopGroup} which reuse local event loop if already
	 * working
	 * inside one.
	 *
	 * @param group the {@link EventLoopGroup} to decorate
	 *
	 * @return a decorated {@link EventLoopGroup} that will colocate executions on the
	 * same thread stack
	 */
	public static EventLoopGroup colocate(EventLoopGroup group) {
		return new ColocatedEventLoopGroup(group);
	}

	/**
	 * Create a simple {@link LoopResources} to provide automatically for {@link
	 * EventLoopGroup} and {@link Channel} factories
	 *
	 * @param prefix the event loop thread name prefix
	 * @param workerCount number of worker threads
	 * @param daemon should the thread be released on jvm shutdown
	 *
	 * @return a new {@link LoopResources} to provide automatically for {@link
	 * EventLoopGroup} and {@link Channel} factories
	 */
	public static LoopResources create(String prefix, int workerCount, boolean daemon) {
		ObjectHelper.verifyPositive(workerCount, "workerCount");
		return new DefaultLoopResources(prefix, workerCount, daemon);
	}

	/**
	 * Create a simple {@link LoopResources} to provide automatically for {@link
	 * EventLoopGroup} and {@link Channel} factories
	 *
	 * @param prefix the event loop thread name prefix
	 * @param selectCount number of selector threads
	 * @param workerCount number of worker threads
	 * @param daemon should the thread be released on jvm shutdown
	 *
	 * @return a new {@link LoopResources} to provide automatically for {@link
	 * EventLoopGroup} and {@link Channel} factories
	 */
	public static LoopResources create(String prefix,
															int selectCount,
															int workerCount,
															boolean daemon) {
		if (ObjectHelper.requireNonNull(prefix, "prefix").isEmpty()) {
			throw new IllegalArgumentException("Cannot use empty prefix");
		}
		ObjectHelper.verifyPositive(selectCount, "selectCount");
		ObjectHelper.verifyPositive(workerCount, "workerCount");
		return new DefaultLoopResources(prefix, selectCount, workerCount, daemon);
	}

	/**
	 * Create a simple {@link LoopResources} to provide automatically for {@link
	 * EventLoopGroup} and {@link Channel} factories
	 *
	 * @param prefix the event loop thread name prefix
	 *
	 * @return a new {@link LoopResources} to provide automatically for {@link
	 * EventLoopGroup} and {@link Channel} factories
	 */
	public static LoopResources create(String prefix) {
		return new DefaultLoopResources(prefix, DEFAULT_IO_SELECT_COUNT,
				DEFAULT_IO_WORKER_COUNT,
				true);
	}

	final String                          prefix;
	final boolean                         daemon;
	final int                             selectCount;
	final int                             workerCount;
	final EventLoopGroup                  serverLoops;
	final EventLoopGroup                  clientLoops;
	final EventLoopGroup                  serverSelectLoops;
	final AtomicReference<EventLoopGroup> cacheNativeClientLoops;
	final AtomicReference<EventLoopGroup> cacheNativeServerLoops;
	final AtomicReference<EventLoopGroup> cacheNativeSelectLoops;
	final AtomicBoolean                   running;

	static ThreadFactory threadFactory(DefaultLoopResources parent, String prefix) {
		return new EventLoopSelectorFactory(parent.daemon,
				parent.prefix + "-" + prefix,
				parent);
	}

	DefaultLoopResources(String prefix, int workerCount, boolean daemon) {
		this(prefix, -1, workerCount, daemon);
	}

	DefaultLoopResources(String prefix,
			int selectCount,
			int workerCount,
			boolean daemon) {
		this.running = new AtomicBoolean(true);
		this.daemon = daemon;
		this.workerCount = workerCount;
		this.prefix = prefix;

		this.serverLoops = new NioEventLoopGroup(workerCount,
				threadFactory(this, "nio"));

		this.clientLoops = colocate(serverLoops);

		this.cacheNativeClientLoops = new AtomicReference<>();
		this.cacheNativeServerLoops = new AtomicReference<>();

		if (selectCount == -1) {
			this.selectCount = workerCount;
			this.serverSelectLoops = this.serverLoops;
			this.cacheNativeSelectLoops = this.cacheNativeServerLoops;
		}
		else {
			this.selectCount = selectCount;
			this.serverSelectLoops =
					new NioEventLoopGroup(selectCount, threadFactory(this, "select-nio"));
			this.cacheNativeSelectLoops = new AtomicReference<>();
		}
	}

	@Override
	public boolean isDisposed() {
		return !running.get();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Flowable<Void> disposeLater() {
		return Flowable.defer(() -> {
			EventLoopGroup cacheNativeClientGroup = cacheNativeClientLoops.get();
			EventLoopGroup cacheNativeSelectGroup = cacheNativeSelectLoops.get();
			EventLoopGroup cacheNativeServerGroup = cacheNativeServerLoops.get();

			if(running.compareAndSet(true, false)) {
				clientLoops.shutdownGracefully();
				serverSelectLoops.shutdownGracefully();
				serverLoops.shutdownGracefully();
				if(cacheNativeClientGroup != null){
					cacheNativeClientGroup.shutdownGracefully();
				}
				if(cacheNativeSelectGroup != null){
					cacheNativeSelectGroup.shutdownGracefully();
				}
				if(cacheNativeServerGroup != null){
					cacheNativeServerGroup.shutdownGracefully();
				}
			}

			Flowable<Void> cl = FutureFlowable.from((Future) clientLoops.terminationFuture());
			Flowable<Void> ssl = FutureFlowable.from((Future)serverSelectLoops.terminationFuture());
			Flowable<Void> sl = FutureFlowable.from((Future)serverLoops.terminationFuture());
			Flowable<Void> cncl = Flowable.empty();
			if(cacheNativeClientGroup != null){
				cncl = FutureFlowable.from((Future) cacheNativeClientGroup.terminationFuture());
			}
			Flowable<Void> cnsl = Flowable.empty();
			if(cacheNativeSelectGroup != null){
				cnsl = FutureFlowable.from((Future) cacheNativeSelectGroup.terminationFuture());
			}
			Flowable<Void> cnsrvl = Flowable.empty();
			if(cacheNativeServerGroup != null){
				cnsrvl = FutureFlowable.from((Future) cacheNativeServerGroup.terminationFuture());
			}

			return Flowable.mergeArray(cl, ssl, sl, cncl, cnsl, cnsrvl);
		});
	}

	@Override
	public EventLoopGroup onServerSelect(boolean useNative) {
		if (useNative && preferNative()) {
			return cacheNativeSelectLoops();
		}
		return serverSelectLoops;
	}

	@Override
	public EventLoopGroup onServer(boolean useNative) {
		if (useNative && preferNative()) {
			return cacheNativeServerLoops();
		}
		return serverLoops;
	}

	@Override
	public EventLoopGroup onClient(boolean useNative) {
		if (useNative && preferNative()) {
			return cacheNativeClientLoops();
		}
		return clientLoops;
	}

	EventLoopGroup cacheNativeSelectLoops() {
		if (cacheNativeSelectLoops == cacheNativeServerLoops) {
			return cacheNativeServerLoops();
		}

		EventLoopGroup eventLoopGroup = cacheNativeSelectLoops.get();
		if (null == eventLoopGroup) {
			EventLoopGroup newEventLoopGroup = DefaultLoopEpollDetector.newEventLoopGroup(
					selectCount,
					threadFactory(this, "select-epoll"));
			if (!cacheNativeSelectLoops.compareAndSet(null, newEventLoopGroup)) {
				newEventLoopGroup.shutdownGracefully();
			}
			eventLoopGroup = cacheNativeSelectLoops();
		}
		return eventLoopGroup;
	}

	EventLoopGroup cacheNativeServerLoops() {
		EventLoopGroup eventLoopGroup = cacheNativeServerLoops.get();
		if (null == eventLoopGroup) {
			EventLoopGroup newEventLoopGroup = DefaultLoopEpollDetector.newEventLoopGroup(
					workerCount,
					threadFactory(this, "server-epoll"));
			if (!cacheNativeServerLoops.compareAndSet(null, newEventLoopGroup)) {
				newEventLoopGroup.shutdownGracefully();
			}
			eventLoopGroup = cacheNativeServerLoops();
		}
		return eventLoopGroup;
	}

	EventLoopGroup cacheNativeClientLoops() {
		EventLoopGroup eventLoopGroup = cacheNativeClientLoops.get();
		if (null == eventLoopGroup) {
			EventLoopGroup newEventLoopGroup = DefaultLoopEpollDetector.newEventLoopGroup(
					workerCount,
					threadFactory(this, "client-epoll"));
			newEventLoopGroup = colocate(newEventLoopGroup);
			if (!cacheNativeClientLoops.compareAndSet(null, newEventLoopGroup)) {
				newEventLoopGroup.shutdownGracefully();
			}
			eventLoopGroup = cacheNativeClientLoops();
		}
		return eventLoopGroup;
	}

	final static class EventLoopSelectorFactory implements ThreadFactory {

		final boolean    daemon;
		final AtomicLong counter;
		final String     prefix;

		public EventLoopSelectorFactory(boolean daemon,
				String prefix,
				AtomicLong counter) {
			this.daemon = daemon;
			this.counter = counter;
			this.prefix = prefix;
		}

		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r);
			t.setDaemon(daemon);
			t.setName(prefix + "-" + counter.incrementAndGet());
			return t;
		}
	}
}
