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

package io.reactivex.netty.options;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Callable;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.socket.InternetProtocolFamily;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.ssl.JdkSslContext;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.resolver.AddressResolverGroup;
import io.netty.resolver.DefaultAddressResolverGroup;
import io.netty.resolver.NoopAddressResolverGroup;
import io.netty.util.NetUtil;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.netty.resources.LoopResources;
import io.reactivex.netty.resources.PoolResources;

/**
 * A client connector builder with low-level connection options including connection pooling and
 * proxy.
 *
 * @author Stephane Maldini
 * @author Violeta Georgieva
 */
public class ClientOptions extends NettyOptions<Bootstrap, ClientOptions> {

	/**
	 * Creates a builder for {@link ClientOptions ClientOptions}
	 *
	 * @param <BUILDER> A ClientOptions.Builder subclass
	 * @return a new ClientOptions builder
	 */
	public static <BUILDER extends ClientOptions.Builder<BUILDER>> ClientOptions.Builder<BUILDER> builder() {
		return builder(new Bootstrap());
	}

	/**
	 * Creates a builder for {@link ClientOptions ClientOptions}
	 *
	 * @param bootstrap the bootstrap reference to use
	 * @param <BUILDER> A ClientOptions.Builder subclass
	 * @return a new ClientOptions builder
	 */
	public static <BUILDER extends ClientOptions.Builder<BUILDER>> ClientOptions.Builder<BUILDER> builder(Bootstrap bootstrap) {
		return new ClientOptions.Builder<>(bootstrap);
	}

	/**
	 * Client connection pool selector
	 */
	private final PoolResources poolResources;

	/**
	 * Proxy options
	 */
	private final ClientProxyOptions proxyOptions;

	private final InternetProtocolFamily protocolFamily;
	private final Callable<? extends SocketAddress> connectAddress;

	/**
	 * Deep-copy all references from the passed builder into this new
	 * {@link NettyOptions} instance.
	 *
	 * @param builder the ClientOptions builder
	 */
	protected ClientOptions(ClientOptions.Builder<?> builder){
		super(builder);
		this.proxyOptions = builder.proxyOptions;
		if (builder.connectAddress == null) {
			if (builder.port >= 0) {
				if (builder.host == null) {
					this.connectAddress = () -> new InetSocketAddress(NetUtil.LOCALHOST.getHostAddress(), builder.port);
				}
				else {
					this.connectAddress = () -> InetSocketAddress.createUnresolved(builder.host, builder.port);
				}
			}
			else {
				this.connectAddress = null;
			}
		}
		else {
			this.connectAddress = builder.connectAddress;
		}
		this.poolResources = builder.poolResources;
		this.protocolFamily = builder.protocolFamily;
	}

	@Override
	public ClientOptions duplicate() {
		return builder().from(this).build();
	}

	@Override
	public Bootstrap call() {
		Bootstrap b = super.call();
		groupAndChannel(b);
		return b;
	}

	@Override
	public final SocketAddress getAddress() {
		try {
			return connectAddress == null ? null : connectAddress.call();
		} catch (Exception e) {
			throw Exceptions.propagate(e);
		}
	}

	/**
	 * Get the configured Pool Resources if any
	 *
	 * @return an eventual {@link PoolResources}
	 */
	public final PoolResources getPoolResources() {
		return this.poolResources;
	}

	public final ClientProxyOptions getProxyOptions() {
		return this.proxyOptions;
	}

	/**
	 * Return true if {@link io.netty.channel.socket.DatagramChannel} should be used
	 *
	 * @return true if {@link io.netty.channel.socket.DatagramChannel} should be used
	 */
	protected boolean useDatagramChannel() {
		return false;
	}

	/**
	 * Return true if proxy options have been set and the host is not
	 * configured to bypass the proxy.
	 *
	 * @param address The address to which this client should connect.
	 * @return true if proxy options have been set and the host is not
	 * configured to bypass the proxy.
	 */
	public boolean useProxy(SocketAddress address) {
		if (this.proxyOptions != null) { 
			if (this.proxyOptions.getNonProxyHosts() != null && 
					address instanceof InetSocketAddress) {
				String hostName = ((InetSocketAddress) address).getHostName();
				return !this.proxyOptions.getNonProxyHosts().matcher(hostName).matches();
			}
			return true;
		}
		return false;
	}

	/**
	 * Return true if proxy options have been set and the host is not
	 * configured to bypass the proxy.
	 *
	 * @param hostName The host name to which this client should connect.
	 * @return true if proxy options have been set and the host is not
	 * configured to bypass the proxy.
	 */
	public boolean useProxy(String hostName) {
		if (this.proxyOptions != null) { 
			if (this.proxyOptions.getNonProxyHosts() != null && hostName != null) {
				return !this.proxyOptions.getNonProxyHosts().matcher(hostName).matches();
			}
			return true;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	final void groupAndChannel(Bootstrap bootstrap) {
		LoopResources loops = ObjectHelper.requireNonNull(getLoopResources(), "loopResources");

		boolean useNative =
				this.protocolFamily == null && preferNative() && !(sslContext() instanceof JdkSslContext);
		EventLoopGroup elg = loops.onClient(useNative);

		if (this.poolResources != null && elg instanceof Callable) {
			//don't colocate
			try {
				bootstrap.group(((Callable<EventLoopGroup>) elg).call());
			} catch (Exception e) {
				throw Exceptions.propagate(e);
			}
		}
		else {
			bootstrap.group(elg);
		}

		if (useDatagramChannel()) {
			if (useNative) {
				bootstrap.channel(loops.onDatagramChannel(elg));
			}
			else {
				bootstrap.channelFactory(() -> new NioDatagramChannel(protocolFamily));
			}
		}
		else {
			bootstrap.channel(loops.onChannel(elg));
		}
	}


	@Override
	public String asSimpleString() {
		StringBuilder s = new StringBuilder();
		if (getAddress() == null) {
			s.append("connecting to no base address");
		}
		else {
			s.append("connecting to ").append(getAddress());
		}
		if (proxyOptions != null) {
			s.append(" through ").append(proxyOptions.getType()).append(" proxy");
		}
		return s.toString();
	}

	@Override
	public String asDetailedString() {
		if (proxyOptions == null) {
			return "connectAddress=" + getAddress() + ", proxy=null, " + super.asDetailedString();
		}
		return "connectAddress=" + getAddress() +
				", " + proxyOptions.asSimpleString() + ", " +
				super.asDetailedString();
	}

	@Override
	public String toString() {
		return "ClientOptions{" + asDetailedString() + "}";
	}

	public static class Builder<BUILDER extends Builder<BUILDER>>
			extends NettyOptions.Builder<Bootstrap, ClientOptions, BUILDER> {
		private PoolResources poolResources;
		private boolean poolDisabled = false;
		private InternetProtocolFamily protocolFamily;
		private String host;
		private int port = -1;
		private Callable<? extends SocketAddress> connectAddress;
		private ClientProxyOptions proxyOptions;

		/**
		 * Apply common option via super constructor then apply
		 * {@link #defaultClientOptions(Bootstrap)}
		 * to the passed bootstrap.
		 *
		 * @param bootstrapTemplate the bootstrap reference to use
		 */
		protected Builder(Bootstrap bootstrapTemplate) {
			super(bootstrapTemplate);
			defaultClientOptions(bootstrapTemplate);
		}

		private void defaultClientOptions(Bootstrap bootstrap) {
			bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)
			         .option(ChannelOption.AUTO_READ, false)
			         .option(ChannelOption.SO_RCVBUF, 1024 * 1024)
			         .option(ChannelOption.SO_SNDBUF, 1024 * 1024);
		}

		/**
		 * Assign an {@link AddressResolverGroup}.
		 *
		 * @param resolver the new {@link AddressResolverGroup}
		 * @return {@code this}
		 */
		public final BUILDER resolver(AddressResolverGroup<?> resolver) {
			ObjectHelper.requireNonNull(resolver, "resolver");
			this.bootstrapTemplate.resolver(resolver);
			return call();
		}

		/**
		 * Configures the {@link ChannelPool} selector for the socket. Will effectively
		 * enable client connection-pooling.
		 *
		 * @param poolResources the {@link PoolResources} given
		 * an {@link InetSocketAddress}
		 * @return {@code this}
		 */
		public final BUILDER poolResources(PoolResources poolResources) {
			this.poolResources = ObjectHelper.requireNonNull(poolResources, "poolResources");
			this.poolDisabled = false;
			return call();
		}

		/**
		 * Disable current {@link #poolResources}
		 *
		 * @return {@code this}
		 */
		public BUILDER disablePool() {
			this.poolResources = null;
			this.poolDisabled = true;
			return call();
		}

		public final boolean isPoolDisabled() {
			return poolDisabled;
		}

		public final boolean isPoolAvailable() {
			return this.poolResources != null;
		}

		/**
		 * Configures the version family for the socket.
		 *
		 * @param protocolFamily the version family for the socket, or null for the system
		 * default family
		 * @return {@code this}
		 */
		public final BUILDER protocolFamily(InternetProtocolFamily protocolFamily) {
			this.protocolFamily = ObjectHelper.requireNonNull(protocolFamily, "protocolFamily");
			return call();
		}

		/**
		 * Enable default sslContext support
		 *
		 * @return {@code this}
		 */
		public final BUILDER sslSupport() {
			return sslSupport(c -> {
			});
		}

		/**
		 * Enable default sslContext support and enable further customization via the passed
		 * configurator. The builder will then produce the {@link SslContext} to be passed to
		 * {@link #sslContext(SslContext)}.
		 *
		 * @param configurator builder callback for further customization.
		 * @return {@code this}
		 */
		public final BUILDER sslSupport(Consumer<? super SslContextBuilder> configurator) {
			ObjectHelper.requireNonNull(configurator, "configurator");
			try {
				SslContextBuilder builder = SslContextBuilder.forClient();
				configurator.accept(builder);
				return sslContext(builder.build());
			}
			catch (Exception sslException) {
				throw Exceptions.propagate(sslException);
			}
		}

		/**
		 * The host to which this client should connect.
		 *
		 * @param host The host to connect to.
		 * @return {@code this}
		 */
		public final BUILDER host(String host) {
			if (host == null) {
				this.host = NetUtil.LOCALHOST.getHostAddress();
			}
			else {
				this.host = host;
			}
			return call();
		}

		/**
		 * The port to which this client should connect.
		 *
		 * @param port The port to connect to.
		 * @return {@code this}
		 */
		public final BUILDER port(int port) {
			this.port = ObjectHelper.requireNonNull(port, "port");
			return call();
		}

		/**
		 * The address to which this client should connect.
		 *
		 * @param connectAddressSupplier A supplier of the address to connect to.
		 * @return {@code this}
		 */
		public final BUILDER connectAddress(Callable<? extends SocketAddress> connectAddressSupplier) {
			this.connectAddress = ObjectHelper.requireNonNull(connectAddressSupplier, "connectAddressSupplier");
			return call();
		}

		/**
		 * The proxy configuration
		 *
		 * @param proxyOptions the proxy configuration
		 * @return {@code this}
		 */
		public final BUILDER proxy(Function<ClientProxyOptions.TypeSpec, ClientProxyOptions.Builder> proxyOptions) {
			ObjectHelper.requireNonNull(proxyOptions, "proxyOptions");
			try {
				ClientProxyOptions.Builder builder = proxyOptions.apply(ClientProxyOptions.builder());
				this.proxyOptions = builder.build();
				if(bootstrapTemplate.config().resolver() == DefaultAddressResolverGroup.INSTANCE) {
					resolver(NoopAddressResolverGroup.INSTANCE);
				}
				return call();
			} catch (Exception e) {
				throw Exceptions.propagate(e);
			}
		}

		@Override
		public final BUILDER from(ClientOptions options) {
			super.from(options);
			this.proxyOptions = options.proxyOptions;
			this.connectAddress = options.connectAddress;
			this.poolResources = options.poolResources;
			this.protocolFamily = options.protocolFamily;
			return call();
		}

		@Override
		@SuppressWarnings("unchecked")
		public BUILDER call() {
			return (BUILDER) this;
		}

		public ClientOptions build() {
			return new ClientOptions(this);
		}
	}
}
