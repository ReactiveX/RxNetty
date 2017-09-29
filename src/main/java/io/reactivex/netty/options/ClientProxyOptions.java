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
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.proxy.ProxyHandler;
import io.netty.handler.proxy.Socks4ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;
import io.reactivex.internal.functions.ObjectHelper;

/**
 * Proxy configuration
 *
 * @author Violeta Georgieva
 */
public class ClientProxyOptions {

	/**
	 * Creates a builder for {@link ClientProxyOptions ClientProxyOptions}
	 *
	 * @return a new ClientProxyOptions builder
	 */
	public static ClientProxyOptions.TypeSpec builder() {
		return new ClientProxyOptions.Build();
	}

	private final String username;
	private final Function<? super String, ? extends String> password;
	private final Callable<? extends InetSocketAddress> address;
	private final Pattern nonProxyHosts;
	private final Proxy type;

	private ClientProxyOptions(ClientProxyOptions.Build builder) {
		this.username = builder.username;
		this.password = builder.password;
		if (builder.address == null) {
			this.address = () -> new InetSocketAddress(builder.host, builder.port);
		}
		else {
			this.address = builder.address;
		}
		if (builder.nonProxyHosts != null) {
			this.nonProxyHosts = Pattern.compile(builder.nonProxyHosts, Pattern.CASE_INSENSITIVE);
		}
		else {
			this.nonProxyHosts = null;
		}
		this.type = builder.type;
	}

	/**
	 * The proxy type
	 *
	 * @return The proxy type
	 */
	public final Proxy getType() {
		return this.type;
	}

	/**
	 * The supplier for the address to connect to.
	 *
	 * @return The supplier for the address to connect to.
	 */
	public final Callable<? extends InetSocketAddress> getAddress() {
		return this.address;
	}

	/**
	 * Regular expression (<code>using java.util.regex</code>) for a configured
	 * list of hosts that should be reached directly, bypassing the proxy.
	 *
	 * @return Regular expression (<code>using java.util.regex</code>) for
	 * a configured list of hosts that should be reached directly, bypassing the
	 * proxy.
	 */
	public final Pattern getNonProxyHosts() {
		return this.nonProxyHosts;
	}

	/**
	 * Return a new eventual {@link ProxyHandler}
	 *
	 * @return a new eventual {@link ProxyHandler}
	 */
	public final ProxyHandler newProxyHandler() {
		InetSocketAddress proxyAddr;
		String username, password;
		try {
			proxyAddr = this.address.call();
			username = this.username;
			password = username != null && this.password != null ?
					this.password.apply(username) : null;
		} catch (Exception e) {
			throw Exceptions.propagate(e);
		}

		switch (this.type) {
			case HTTP:
				return username != null && password != null ?
						new HttpProxyHandler(proxyAddr, username, password) :
						new HttpProxyHandler(proxyAddr);
			case SOCKS4:
				return username != null ? new Socks4ProxyHandler(proxyAddr, username) :
						new Socks4ProxyHandler(proxyAddr);
			case SOCKS5:
				return username != null && password != null ?
						new Socks5ProxyHandler(proxyAddr, username, password) :
						new Socks5ProxyHandler(proxyAddr);
		}
		throw new IllegalArgumentException("Proxy type unsupported : " + this.type);
	}

	/**
	 * Proxy Type
	 */
	public enum Proxy {
		HTTP, SOCKS4, SOCKS5
	}


	public String asSimpleString() {
		try {
			return "proxy=" + this.type +
					"(" + this.address.call() + ")";
		} catch (Exception e) {
			throw Exceptions.propagate(e);
		}
	}

	public String asDetailedString() {
		try {
			return "address=" + this.address.call() +
					", nonProxyHosts=" + this.nonProxyHosts +
					", type=" + this.type;
		} catch (Exception e) {
			throw Exceptions.propagate(e);
		}
	}

	@Override
	public String toString() {
		return "ClientProxyOptions{" + asDetailedString() + "}";
	}

	private static final class Build implements TypeSpec, AddressSpec, Builder {
		private String username;
		private Function<? super String, ? extends String> password;
		private String host;
		private int port;
		private Callable<? extends InetSocketAddress> address;
		private String nonProxyHosts;
		private Proxy type;

		private Build() {
		}

		@Override
		public final Builder username(String username) {
			this.username = username;
			return this;
		}

		@Override
		public final Builder password(Function<? super String, ? extends String> password) {
			this.password = password;
			return this;
		}

		@Override
		public final Builder host(String host) {
			this.host = ObjectHelper.requireNonNull(host, "host");
			return this;
		}

		@Override
		public final Builder port(int port) {
			this.port = ObjectHelper.requireNonNull(port, "port");
			return this;
		}

		@Override
		public final Builder address(InetSocketAddress address) {
			ObjectHelper.requireNonNull(address, "address");
			this.address = address.isUnresolved() ?
					() -> new InetSocketAddress(address.getHostName(),
							address.getPort()) :
					() -> address;
			return this;
		}

		@Override
		public final Builder address(Callable<? extends InetSocketAddress> addressSupplier) {
			this.address = ObjectHelper.requireNonNull(addressSupplier, "addressSupplier");
			return this;
		}

		@Override
		public final Builder nonProxyHosts(String nonProxyHostsPattern) {
			this.nonProxyHosts = nonProxyHostsPattern;
			return this;
		}

		@Override
		public final AddressSpec type(Proxy type) {
			this.type = ObjectHelper.requireNonNull(type, "type");
			return this;
		}

		@Override
		public ClientProxyOptions build() {
			return new ClientProxyOptions(this);
		}
	}

	public interface TypeSpec {

		/**
		 * The proxy type.
		 *
		 * @param type The proxy type.
		 * @return {@code this}
		 */
		public AddressSpec type(Proxy type);
	}

	public interface AddressSpec {

		/**
		 * The proxy host to connect to.
		 *
		 * @param host The proxy host to connect to.
		 * @return {@code this}
		 */
		public Builder host(String host);

		/**
		 * The address to connect to.
		 *
		 * @param address The address to connect to.
		 * @return {@code this}
		 */
		public Builder address(InetSocketAddress address);

		/**
		 * The supplier for the address to connect to.
		 *
		 * @param addressSupplier The supplier for the address to connect to.
		 * @return {@code this}
		 */
		public Builder address(Callable<? extends InetSocketAddress> addressSupplier);
	}

	public interface Builder {

		/**
		 * The proxy username.
		 *
		 * @param username The proxy username.
		 * @return {@code this}
		 */
		public Builder username(String username);

		/**
		 * A function to supply the proxy's password from the username.
		 *
		 * @param password A function to supply the proxy's password from the username.
		 * @return {@code this}
		 */
		public Builder password(Function<? super String, ? extends String> password);

		/**
		 * The proxy port to connect to.
		 *
		 * @param port The proxy port to connect to.
		 * @return {@code this}
		 */
		public Builder port(int port);

		/**
		 * Regular expression (<code>using java.util.regex</code>) for a configured
		 * list of hosts that should be reached directly, bypassing the proxy.
		 *
		 * @param nonProxyHostsPattern Regular expression (<code>using java.util.regex</code>)
		 * for a configured list of hosts that should be reached directly, bypassing the proxy.
		 * @return {@code this}
		 */
		public Builder nonProxyHosts(String nonProxyHostsPattern);

		/**
		 * Builds new ClientProxyOptions
		 *
		 * @return builds new ClientProxyOptions
		 */
		public ClientProxyOptions build();
	}
}
