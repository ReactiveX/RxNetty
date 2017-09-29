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

package io.reactivex.netty.http.client;

import java.net.URI;
import java.net.URISyntaxException;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.AsciiString;
import io.reactivex.Flowable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.netty.NettyInbound;
import io.reactivex.netty.channel.AbortedException;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import io.reactivex.netty.NettyOutbound;

/**
 * @author Stephane Maldini
 */
final class FlowableHttpClientResponse extends Flowable<HttpClientResponse> {

	final HttpClient                                                     parent;
	final URI                                                            startURI;
	final HttpMethod                                                     method;
	final Function<? super HttpClientRequest, ? extends Publisher<Void>> handler;

	static final AsciiString ALL = new AsciiString("*/*");

	FlowableHttpClientResponse(HttpClient parent, String url,
														 HttpMethod method,
														 Function<? super HttpClientRequest, ? extends Publisher<Void>> handler) {
		this.parent = parent;
		try {
			this.startURI = new URI(parent.options.formatSchemeAndHost(url,
					method == HttpClient.WS));
		}
		catch (URISyntaxException e) {
			throw Exceptions.propagate(e);
		}
		this.method = method == HttpClient.WS ? HttpMethod.GET : method;
		this.handler = handler;

	}

	@Override
	@SuppressWarnings("unchecked")
	public void subscribeActual(final Subscriber<? super HttpClientResponse> subscriber) {
		ReconnectableBridge bridge = new ReconnectableBridge();
		bridge.activeURI = startURI;

		Flowable.defer(() -> parent.client.newHandler(new HttpClientHandler(this, bridge),
				parent.options.getRemoteAddress(bridge.activeURI),
				HttpClientOptions.isSecure(bridge.activeURI),
				bridge))
		    .retry(bridge)
		    .cast(HttpClientResponse.class)
		    .subscribe(subscriber);
	}

	static final class HttpClientHandler
			implements BiFunction<NettyInbound, NettyOutbound, Publisher<Void>> {

		final FlowableHttpClientResponse parent;
		final ReconnectableBridge    bridge;

		HttpClientHandler(FlowableHttpClientResponse parent, ReconnectableBridge bridge) {
			this.bridge = bridge;
			this.parent = parent;
		}

		@Override
		public Publisher<Void> apply(NettyInbound in, NettyOutbound out) {
			try {
				URI uri = bridge.activeURI;
				HttpClientOperations ch = (HttpClientOperations) in;
				String host = uri.getHost();
				int port = uri.getPort();
				if (port != -1 && port != 80 && port != 443) {
					host = host + ':' + port;
				}
				ch.getNettyRequest()
				  .setUri(uri.getRawPath() + (uri.getQuery() == null ? "" :
						  "?" + uri.getRawQuery()))
				  .setMethod(parent.method)
				  .setProtocolVersion(HttpVersion.HTTP_1_1)
				  .headers()
				  .add(HttpHeaderNames.HOST, host)
				  .add(HttpHeaderNames.ACCEPT, ALL);

				if (parent.method == HttpMethod.GET
						|| parent.method == HttpMethod.HEAD
						|| parent.method == HttpMethod.DELETE) {
					ch.chunkedTransfer(false);
				}

				if (parent.handler != null) {
					return parent.handler.apply(ch);
				}
				else {
					return ch.send();
				}
			}
			catch (Throwable t) {
				return Flowable.error(t);
			}
		}

		@Override
		public String toString() {
			return "HttpClientHandler{" + "startURI=" + bridge.activeURI + ", method=" + parent.method + ", handler=" + parent.handler + '}';
		}

	}

	static final class ReconnectableBridge
			implements Predicate<Throwable>, Consumer<Channel> {

		volatile URI      activeURI;
		volatile String[] redirectedFrom;

		ReconnectableBridge() {
		}

		void redirect(String to) {
			String[] redirectedFrom = this.redirectedFrom;
			URI from = activeURI;
			try {
				activeURI = new URI(to);
			}
			catch (URISyntaxException e) {
				throw Exceptions.propagate(e);
			}
			if (redirectedFrom == null) {
				this.redirectedFrom = new String[]{from.toString()};
			}
			else {
				String[] newRedirectedFrom = new String[redirectedFrom.length + 1];
				System.arraycopy(redirectedFrom,
						0,
						newRedirectedFrom,
						0,
						redirectedFrom.length);
				newRedirectedFrom[redirectedFrom.length] = from.toString();
				this.redirectedFrom = newRedirectedFrom;
			}
		}

		@Override
		public void accept(Channel channel) {
			String[] redirectedFrom = this.redirectedFrom;
			if (redirectedFrom != null) {
				channel.attr(HttpClientOperations.REDIRECT_ATTR_KEY)
				       .set(redirectedFrom);
			}
		}

		@Override
		public boolean test(Throwable throwable) {
			if (throwable instanceof RedirectClientException) {
				RedirectClientException re = (RedirectClientException) throwable;
				redirect(re.location);
				return true;
			}
			if (AbortedException.isConnectionReset(throwable)) {
				redirect(activeURI.toString());
				return true;
			}
			return false;
		}
	}


}

