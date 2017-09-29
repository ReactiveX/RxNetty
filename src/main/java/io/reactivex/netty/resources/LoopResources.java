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

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.functions.ObjectHelper;

/**
 * An {@link EventLoopGroup} selector with associated
 * {@link io.netty.channel.Channel} factories.
 *
 * @author Stephane Maldini
 * @since 0.6
 */
@FunctionalInterface
public interface LoopResources extends Disposable {

	/**
	 * Callback for client or generic channel factory selection.
	 *
	 * @param group the source {@link EventLoopGroup} to assign a loop from
	 *
	 * @return a {@link Class} target for the underlying {@link Channel} factory
	 */
	default Class<? extends Channel> onChannel(EventLoopGroup group) {
		return preferNative() ? DefaultLoopEpollDetector.getChannel(group) :
				NioSocketChannel.class;
	}

	/**
	 * Callback for client {@link EventLoopGroup} creation.
	 *
	 * @param useNative should use native group if current {@link #preferNative()} is also
	 * true
	 *
	 * @return a new {@link EventLoopGroup}
	 */
	default EventLoopGroup onClient(boolean useNative) {
		return onServer(useNative);
	}

	/**
	 * Callback for UDP channel factory selection.
	 *
	 * @param group the source {@link EventLoopGroup} to assign a loop from
	 *
	 * @return a {@link Class} target for the underlying {@link Channel} factory
	 */
	default Class<? extends DatagramChannel> onDatagramChannel(EventLoopGroup group) {
		return preferNative() ? DefaultLoopEpollDetector.getDatagramChannel(group) :
				NioDatagramChannel.class;
	}

	/**
	 * Callback for server {@link EventLoopGroup} creation.
	 *
	 * @param useNative should use native group if current {@link #preferNative()} is also
	 * true
	 *
	 * @return a new {@link EventLoopGroup}
	 */
	EventLoopGroup onServer(boolean useNative);

	/**
	 * Callback for server channel factory selection.
	 *
	 * @param group the source {@link EventLoopGroup} to assign a loop from
	 *
	 * @return a {@link Class} target for the underlying {@link ServerChannel} factory
	 */
	default Class<? extends ServerChannel> onServerChannel(EventLoopGroup group) {
		return preferNative() ? DefaultLoopEpollDetector.getServerChannel(group) :
				NioServerSocketChannel.class;
	}

	/**
	 * Create a server select {@link EventLoopGroup} for servers to be used
	 *
	 * @param useNative should use native group if current {@link #preferNative()} is also
	 * true
	 *
	 * @return a new {@link EventLoopGroup}
	 */
	default EventLoopGroup onServerSelect(boolean useNative) {
		return onServer(useNative);
	}

	/**
	 * Rreturn true if should default to native {@link EventLoopGroup} and {@link Channel}
	 *
	 * @return true if should default to native {@link EventLoopGroup} and {@link Channel}
	 */
	default boolean preferNative() {
		return DefaultLoopEpollDetector.hasEpoll();
	}

	/**
	 * return true if {@link EventLoopGroup} should not be shutdown
	 *
	 * @return true if {@link EventLoopGroup} should not be shutdown
	 */
	default boolean daemon() {
		return false;
	}

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
