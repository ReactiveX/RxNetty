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
package io.reactivex.netty.http.server;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.cookie.Cookie;
import io.reactivex.Flowable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.netty.NettyPipeline;
import io.reactivex.netty.http.websocket.WebsocketInbound;
import io.reactivex.netty.http.websocket.WebsocketOutbound;
import org.reactivestreams.Publisher;
import io.reactivex.netty.NettyContext;
import io.reactivex.netty.NettyOutbound;
import io.reactivex.netty.http.HttpInfos;

/**
 *
 * An Http Reactive Channel with several accessor related to HTTP flow : headers, params,
 * URI, method, websocket...
 *
 * @author Stephane Maldini
 * @since 0.5
 */
public interface HttpServerResponse extends NettyOutbound, HttpInfos {

	/**
	 * Add an outbound cookie
	 *
	 * @return this outbound
	 */
	HttpServerResponse addCookie(Cookie cookie);

	/**
	 * Add an outbound http header, appending the value if the header already exist.
	 *
	 * @param name header name
	 * @param value header value
	 *
	 * @return this outbound
	 */
	HttpServerResponse addHeader(CharSequence name, CharSequence value);

	/**
	 * Set transfer-encoding header
	 *
	 * @param chunked true if transfer-encoding:chunked
	 *
	 * @return this outbound
	 */
	HttpServerResponse chunkedTransfer(boolean chunked);

	@Override
	default HttpServerResponse context(Consumer<NettyContext> contextCallback){
		try {
			contextCallback.accept(context());
		} catch (Exception e) {
			throw Exceptions.propagate(e);
		}
		return this;
	}

	/**
	 * Return  true if headers and status have been sent to the client
	 *
	 * @return true if headers and status have been sent to the client
	 */
	boolean hasSentHeaders();

	/**
	 * Set an outbound header, replacing any pre-existing value.
	 *
	 * @param name headers key
	 * @param value header value
	 *
	 * @return this outbound
	 */
	HttpServerResponse header(CharSequence name, CharSequence value);

	/**
	 * Set outbound headers, replacing any pre-existing value for these headers.
	 *
	 * @param headers netty headers map
	 *
	 * @return this outbound
	 */
	HttpServerResponse headers(HttpHeaders headers);

	/**
	 * Set the request keepAlive if true otherwise remove the existing connection keep alive header
	 *
	 * @return this outbound
	 */
	HttpServerResponse keepAlive(boolean keepAlive);

	@Override
	default HttpServerResponse onWriteIdle(long idleTimeout, Runnable onWriteIdle){
		NettyOutbound.super.onWriteIdle(idleTimeout, onWriteIdle);
		return this;
	}

	@Override
	default HttpServerResponse options(Consumer<? super NettyPipeline.SendOptions> configurator){
		NettyOutbound.super.options(configurator);
		return this;
	}

	/**
	 * Return headers sent back to the clients
	 * @return headers sent back to the clients
	 */
	HttpHeaders responseHeaders();

	/**
	 * Send headers and empty content thus delimiting a full empty body http response.
	 *
	 * @return a {@link Flowable} successful on committed response
	 * @see #send(Publisher)
	 */
	default Flowable<Void> send(){
		return sendObject(Unpooled.EMPTY_BUFFER).then();
	}

	/**
	 * Return a {@link NettyOutbound} successful on committed response
	 *
	 * @return a {@link NettyOutbound} successful on committed response
	 */
	NettyOutbound sendHeaders();

	/**
	 * Send 404 status {@link HttpResponseStatus#NOT_FOUND}.
	 *
	 * @return a {@link Flowable} successful on flush confirmation
	 */
	Flowable<Void> sendNotFound();

	/**
	 * Send redirect status {@link HttpResponseStatus#FOUND} along with a location
	 * header to the remote client.
	 *
	 * @param location the location to redirect to
	 *
	 * @return a {@link Flowable} successful on flush confirmation
	 */
	Flowable<Void> sendRedirect(String location);

	/**
	 * Upgrade connection to Websocket. Flowable and Callback are invoked on handshake
	 * success, otherwise the returned {@link Flowable} fails.
	 *
	 * @param websocketHandler the in/out handler for ws transport
	 * @return a {@link Flowable} completing when upgrade is confirmed
	 */
	default Flowable<Void> sendWebsocket(BiFunction<? super WebsocketInbound, ? super WebsocketOutbound, ? extends Publisher<Void>> websocketHandler) {
		return sendWebsocket(uri(), websocketHandler);
	}

	/**
	 * Upgrade connection to Websocket with optional subprotocol(s). Flowable and Callback
	 * are invoked on handshake success, otherwise the returned {@link Flowable} fails.
	 *
	 * @param protocols optional sub-protocol
	 * @param websocketHandler the in/out handler for ws transport
	 *
	 * @return a {@link Flowable} completing when upgrade is confirmed
	 */
	Flowable<Void> sendWebsocket(String protocols,
			BiFunction<? super WebsocketInbound, ? super WebsocketOutbound, ? extends Publisher<Void>> websocketHandler);


	/**
	 * Add Server-Side-Event content-type
	 * @return this response
	 */
	HttpServerResponse sse();

	/**
	 * Return the assigned HTTP status
	 * @return the assigned HTTP status
	 */
	HttpResponseStatus status();

	/**
	 * Set an HTTP status to be sent along with the headers
	 * @param status an HTTP status to be sent along with the headers
	 * @return this response
	 */
	HttpServerResponse status(HttpResponseStatus status);

	/**
	 * Set an HTTP status to be sent along with the headers
	 * @param status an HTTP status to be sent along with the headers
	 * @return this response
	 */
	default HttpServerResponse status(int status){
		return status(HttpResponseStatus.valueOf(status));
	}


}
