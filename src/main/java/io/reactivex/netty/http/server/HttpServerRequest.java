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

import java.util.Map;

import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.reactivex.Flowable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.netty.NettyContext;
import io.reactivex.netty.NettyInbound;
import io.reactivex.netty.http.HttpInfos;

/**
 * An Http Reactive Channel with several accessor related to HTTP flow : headers, params,
 * URI, method, websocket...
 *
 * @author Stephane Maldini
 * @since 0.5
 */
public interface HttpServerRequest extends NettyInbound, HttpInfos {

	@Override
	default HttpServerRequest onReadIdle(long idleTimeout, Runnable onReadIdle) {
		NettyInbound.super.onReadIdle(idleTimeout, onReadIdle);
		return this;
	}

	@Override
	default HttpServerRequest context(Consumer<NettyContext> contextCallback) {
		NettyInbound.super.context(contextCallback);
		return this;
	}

	/**
	 * URI parameter captured via {} "/test/{var}"
	 *
	 * @param key param var name
	 *
	 * @return the param captured value
	 */
	String param(CharSequence key);

	/**
	 * Return the param captured key/value map
	 *
	 * @return the param captured key/value map
	 */
	Map<String, String> params();

	/**
	 * @param headerResolver provide a params
	 *
	 * @return this request
	 */
	HttpServerRequest paramsResolver(Function<? super String, Map<String, String>> headerResolver);

	/**
	 * Return a {@link Flowable} of {@link HttpContent} containing received chunks
	 *
	 * @return a {@link Flowable} of {@link HttpContent} containing received chunks
	 */
	default Flowable<HttpContent> receiveContent() {
		return receiveObject().ofType(HttpContent.class);
	}

	/**
	 * Return inbound {@link HttpHeaders}
	 *
	 * @return inbound {@link HttpHeaders}
	 */
	HttpHeaders requestHeaders();

}
