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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.reactivex.Flowable;
import io.reactivex.netty.NettyContext;

/**
 * Wrap a {@link NettyContext} obtained from a {@link Flowable} and offer methods to manage
 * its lifecycle in a blocking fashion.
 *
 * @author Simon Baslé
 */
public class BlockingNettyContext {

	private final NettyContext context;
	private final String description;

	private long lifecycleTimeout;
	private TimeUnit unit;
	private Thread shutdownHook;

	public BlockingNettyContext(Flowable<? extends NettyContext> contextAsync,
															String description) {
		this(contextAsync, description, 3, TimeUnit.SECONDS);
	}

	public BlockingNettyContext(Flowable<? extends NettyContext> contextAsync,
															String description, long lifecycleTimeout, TimeUnit unit) {
		this.description = description;
		this.lifecycleTimeout = lifecycleTimeout;
		this.unit = unit;
		this.context = contextAsync
				.timeout(lifecycleTimeout, unit, Flowable.error(new TimeoutException(description + " couldn't be started within " + unit.toMillis(lifecycleTimeout) + "ms")))
				.blockingSingle();
	}

	/**
	 * Change the lifecycle timeout applied to the {@link #shutdown()} operation (as this can
	 * only be called AFTER the {@link NettyContext} has been "started").
	 *
	 * @param timeout the new timeout to apply on shutdown.
	 */
	public void setLifecycleTimeout(long timeout, TimeUnit unit) {
		this.lifecycleTimeout = timeout;
		this.unit = unit;
	}

	/**
	 * Get the {@link NettyContext} wrapped by this facade.
	 * @return the original NettyContext.
	 */
	public NettyContext getContext() {
		return context;
	}

	/**
	 * Return this server's port.
	 * @return The port the server is bound to.
	 */
	public int getPort() {
		return context.address().getPort();
	}

	/**
	 * Return the server's host String. That is, the hostname or in case the server was bound
	 * to a literal IP adress, the IP string representation (rather than performing a reverse-DNS
	 * lookup).
	 *
	 * @return the host string, without reverse DNS lookup
	 * @see NettyContext#address()
	 * @see InetSocketAddress#getHostString()
	 */
	public String getHost() {
		return context.address().getHostString();
	}

	/**
	 * Install a {@link Runtime#addShutdownHook(Thread) JVM shutdown hook} that will
	 * shutdown this {@link BlockingNettyContext} if the JVM is terminated externally.
	 * <p>
	 * The hook is removed if shutdown manually, and subsequent calls to this method are
	 * no-op.
	 */
	public void installShutdownHook() {
		//don't return the hook to discourage uninstalling it externally
		if (this.shutdownHook != null) {
			return;
		}
		this.shutdownHook = new Thread(this::shutdownFromJVM);
		Runtime.getRuntime().addShutdownHook(this.shutdownHook);
	}

	/**
	 * Remove a {@link Runtime#removeShutdownHook(Thread) JVM shutdown hook} if one was
	 * {@link #installShutdownHook() installed} by this {@link BlockingNettyContext}.
	 *
	 * @return true if there was a hook and it was removed, false otherwise.
	 */
	public boolean removeShutdownHook() {
		if (this.shutdownHook != null && Thread.currentThread() != this.shutdownHook) {
			Thread sdh = this.shutdownHook;
			this.shutdownHook = null;
			return Runtime.getRuntime().removeShutdownHook(sdh);
		}
		return false;
	}

	/**
	 * @return the current JVM shutdown hook. Shouldn't be passed to users.
	 */
	protected Thread getShutdownHook() {
		return this.shutdownHook;
	}

	/**
	 * Shut down the {@link NettyContext} and wait for its termination, up to the
	 * {@link #setLifecycleTimeout(long, TimeUnit) lifecycle timeout}.
	 */
	public void shutdown() {
		if (context.isDisposed()) {
			return;
		}

		removeShutdownHook(); //only applies if not called from the hook's thread

		context.dispose();
		context.onClose()
				.timeout(lifecycleTimeout, unit, Flowable.error(new TimeoutException(description + " couldn't be stopped within " + unit.toMillis(lifecycleTimeout) + "ms")))
				.blockingSubscribe();
	}

	protected void shutdownFromJVM() {
		if (context.isDisposed()) {
			return;
		}

		context.dispose();
		context.onClose()
				.timeout(lifecycleTimeout, unit, Flowable.error(new TimeoutException(description +
						" couldn't be stopped within " + unit.toMillis(lifecycleTimeout) + "ms")))
				.blockingSubscribe();
	}
}
