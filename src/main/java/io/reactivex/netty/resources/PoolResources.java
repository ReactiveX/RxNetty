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

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.pool.ChannelPool;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

/**
 * A {@link io.netty.channel.pool.ChannelPool} selector with associated factories.
 *
 * @author Stephane Maldini
 * @since 0.6
 */
@FunctionalInterface
public interface PoolResources extends Disposable {

	/**
	 * Return an existing or new {@link ChannelPool}. The implementation will take care
	 * of pulling {@link Bootstrap} lazily when a {@link ChannelPool} creation is actually
	 * needed.
	 *
	 * @param address the remote address to resolve for existing or
	 * new {@link ChannelPool}
	 * @param bootstrap the {@link Bootstrap} supplier if a {@link ChannelPool} must be
	 * created
	 * @param onChannelCreate callback only when new connection is made
	 * @return an existing or new {@link ChannelPool}
	 */
	ChannelPool selectOrCreate(SocketAddress address,
			Callable<? extends Bootstrap> bootstrap,
			Consumer<? super Channel> onChannelCreate,
			EventLoopGroup group);

	@Override
	default void dispose() {
		//noop default
		disposeLater().subscribe();
	}

	@Override
	default boolean isDisposed() {
		return false;
	}

	/**
	 * Returns a Mono that triggers the disposal of underlying resources when subscribed to.
	 *
	 * @return a Mono representing the completion of resources disposal.
	 **/
	default Flowable<Void> disposeLater() {
		return Flowable.empty(); //noop default
	}
}
