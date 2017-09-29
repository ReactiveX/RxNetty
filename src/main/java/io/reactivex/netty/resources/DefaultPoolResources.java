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

import java.net.SocketAddress;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.pool.*;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import io.netty.util.internal.PlatformDependent;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Consumer;

/**
 * @author Stephane Maldini
 */
public final class DefaultPoolResources implements PoolResources {

	/**
	 * Default max connection, if -1 will never wait to acquire before opening new
	 * connection in an unbounded fashion. Fallback to
	 * available number of processors.
	 */
	public static int DEFAULT_POOL_MAX_CONNECTION =
			Integer.parseInt(System.getProperty("io.reactivex.netty.pool.maxConnections",
					"" + Math.max(Runtime.getRuntime()
							.availableProcessors(), 8) * 2));

	/**
	 * Default acquisition timeout before error. If -1 will never wait to
	 * acquire before opening new
	 * connection in an unbounded fashion. Fallback to
	 * available number of processors.
	 */
	public static long DEFAULT_POOL_ACQUIRE_TIMEOUT = Long.parseLong(System.getProperty(
			"io.reactivex.netty.pool.acquireTimeout",
			"" + 45000));

	/**
	 * Create an uncapped {@link PoolResources} to provide automatically for {@link
	 * ChannelPool}.
	 * <p>An elastic {@link PoolResources} will never wait before opening a new
	 * connection. The reuse window is limited but it cannot starve an undetermined volume
	 * of clients using it.
	 *
	 * @param name the channel pool map name
	 *
	 * @return a new {@link PoolResources} to provide automatically for {@link
	 * ChannelPool}
	 */
	public static PoolResources elastic(String name) {
		return new DefaultPoolResources(name, SimpleChannelPool::new);
	}

	/**
	 * Create a capped {@link PoolResources} to provide automatically for {@link
	 * ChannelPool}.
	 * <p>A Fixed {@link PoolResources} will open up to the given max number of
	 * processors observed by this jvm (minimum 4).
	 * Further connections will be pending acquisition indefinitely.
	 *
	 * @param name the channel pool map name
	 *
	 * @return a new {@link PoolResources} to provide automatically for {@link
	 * ChannelPool}
	 */
	public static PoolResources fixed(String name) {
		return fixed(name, DEFAULT_POOL_MAX_CONNECTION);
	}

	/**
	 * Create a capped {@link PoolResources} to provide automatically for {@link
	 * ChannelPool}.
	 * <p>A Fixed {@link PoolResources} will open up to the given max connection value.
	 * Further connections will be pending acquisition indefinitely.
	 *
	 * @param name the channel pool map name
	 * @param maxConnections the maximum number of connections before starting pending
	 * acquisition on existing ones
	 *
	 * @return a new {@link PoolResources} to provide automatically for {@link
	 * ChannelPool}
	 */
	public static PoolResources fixed(String name, int maxConnections) {
		return fixed(name, maxConnections, DEFAULT_POOL_ACQUIRE_TIMEOUT);
	}

	/**
	 * Create a capped {@link PoolResources} to provide automatically for {@link
	 * ChannelPool}.
	 * <p>A Fixed {@link PoolResources} will open up to the given max connection value.
	 * Further connections will be pending acquisition indefinitely.
	 *
	 * @param name the channel pool map name
	 * @param maxConnections the maximum number of connections before starting pending
	 * @param acquireTimeout the maximum time in millis to wait for aquiring
	 *
	 * @return a new {@link PoolResources} to provide automatically for {@link
	 * ChannelPool}
	 */
	public static PoolResources fixed(String name, int maxConnections, long acquireTimeout) {
		if (maxConnections == -1) {
			return elastic(name);
		}
		if (maxConnections <= 0) {
			throw new IllegalArgumentException("Max Connections value must be strictly " + "positive");
		}
		if (acquireTimeout != -1L && acquireTimeout < 0) {
			throw new IllegalArgumentException("Acquire Timeout value must " + "be " + "positive");
		}
		return new DefaultPoolResources(name,
				(bootstrap, handler, checker) -> new FixedChannelPool(bootstrap,
						handler,
						checker,
						FixedChannelPool.AcquireTimeoutAction.FAIL,
						acquireTimeout,
						maxConnections,
						Integer.MAX_VALUE
				));
	}

	interface PoolFactory {

		ChannelPool newPool(Bootstrap b,
				ChannelPoolHandler handler,
				ChannelHealthChecker checker);
	}

	final ConcurrentMap<SocketAddress, Pool> channelPools;
	final String                             name;
	final PoolFactory                        provider;

	DefaultPoolResources(String name, PoolFactory provider) {
		this.name = name;
		this.provider = provider;
		this.channelPools = PlatformDependent.newConcurrentHashMap();
	}

	@Override
	public ChannelPool selectOrCreate(SocketAddress remote,
			Callable<? extends Bootstrap> bootstrap,
			Consumer<? super Channel> onChannelCreate,
			EventLoopGroup group) {
		SocketAddress address = remote;
		for (; ; ) {
			Pool pool = channelPools.get(remote);
			if (pool != null) {
				return pool;
			}
			try {
				Bootstrap b = bootstrap.call();
				if (remote != null) {
					b = b.remoteAddress(remote);
				} else {
					address = b.config()
							.remoteAddress();
				}
				pool = new Pool(b, provider, onChannelCreate, group);
			} catch (Exception e) {
				throw Exceptions.propagate(e);
			}
			if (channelPools.putIfAbsent(address, pool) == null) {
				return pool;
			}
			pool.close();
		}
	}

	final static class Pool extends AtomicBoolean
			implements ChannelPoolHandler, ChannelPool, ChannelHealthChecker {

		final ChannelPool               pool;
		final Consumer<? super Channel> onChannelCreate;
		final EventLoopGroup            defaultGroup;

		final AtomicInteger activeConnections = new AtomicInteger();

		final Future<Boolean> HEALTHY;
		final Future<Boolean> UNHEALTHY;

		@SuppressWarnings("unchecked")
		Pool(Bootstrap bootstrap,
				PoolFactory provider,
				Consumer<? super Channel> onChannelCreate,
				EventLoopGroup group) {
			this.pool = provider.newPool(bootstrap, this, this);
			this.onChannelCreate = onChannelCreate;
			this.defaultGroup = group;
			HEALTHY = group.next()
			               .newSucceededFuture(true);
			UNHEALTHY = group.next()
			                 .newSucceededFuture(false);
		}

		@Override
		public Future<Boolean> isHealthy(Channel channel) {
			return channel.isActive() ? HEALTHY : UNHEALTHY;
		}

		@Override
		public Future<Channel> acquire() {
			return pool.acquire();
		}

		@Override
		public Future<Channel> acquire(Promise<Channel> promise) {
			return pool.acquire(promise);
		}

		@Override
		public Future<Void> release(Channel channel) {
			return pool.release(channel);
		}

		@Override
		public Future<Void> release(Channel channel, Promise<Void> promise) {
			return pool.release(channel, promise);
		}

		@Override
		public void close() {
			if(compareAndSet(false, true)) {
				pool.close();
			}
		}

		@Override
		public void channelReleased(Channel ch) throws Exception {
			activeConnections.decrementAndGet();
		}

		@Override
		public void channelAcquired(Channel ch) throws Exception {
			activeConnections.incrementAndGet();
		}

		@Override
		public void channelCreated(Channel ch) throws Exception {
			activeConnections.incrementAndGet();
			if (onChannelCreate != null) {
				onChannelCreate.accept(ch);
			}
		}

		@Override
		public String toString() {
			return pool.getClass()
			           .getSimpleName() + "{" + "activeConnections=" + activeConnections + '}';
		}
	}

	@Override
	public void dispose() {
		disposeLater().subscribe();
	}

	@Override
	public Flowable<Void> disposeLater() {
		return Completable.fromRunnable(() -> {
			Pool pool;
			for (SocketAddress key: channelPools.keySet()) {
				pool = channelPools.remove(key);
				if(pool != null){
					pool.close();
				}
			}
		}).toFlowable();
	}

	@Override
	public boolean isDisposed() {
		for (Pool pool: channelPools.values()) {
			if (!pool.get()) {
				return false;
			}
		}
		return true;
	}
}
