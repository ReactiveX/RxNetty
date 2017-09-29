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

import java.io.File;
import java.net.URI;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.netty.http.websocket.WebsocketInbound;
import io.reactivex.netty.http.websocket.WebsocketOutbound;
import org.reactivestreams.Publisher;
import io.reactivex.netty.ByteBufFlowable;

/**
 * Server routes are unique and only the first matching in order of declaration will be
 * invoked.
 *
 * @author Stephane Maldini
 */
public interface HttpServerRoutes extends
                                  BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> {

	/**
	 * @return a new default routing registry {@link HttpServerRoutes}
	 */
	static HttpServerRoutes newRoutes() {
		return new DefaultHttpServerRoutes();
	}

	/**
	 * Listen for HTTP DELETE on the passed path to be used as a routing condition.
	 * Incoming connections will query the internal registry to invoke the matching
	 * handlers. <p> Additional regex matching is available, e.g. "/test/{param}".
	 * Params are resolved using {@link HttpServerRequest#param(CharSequence)}
	 *
	 * @param path The DELETE path used by clients.
	 * @param handler an handler to invoke for the given condition
	 *
	 * @return this {@link HttpServerRoutes}
	 */
	default HttpServerRoutes delete(String path,
			BiFunction<? super HttpServerRequest, ? super HttpServerResponse, ? extends Publisher<Void>> handler) {
		return route(HttpPredicate.delete(path), handler);
	}

	/**
	 * Listen for HTTP GET on the passed path to be used as a routing condition. The
	 * content of the provided  {@link File directory} will be served.
	 * <p>
	 * Additional regex matching is available, e.g. "/test/{param}". Params are resolved
	 * using {@link HttpServerRequest#param(CharSequence)}
	 *
	 * @param uri The GET path used by clients
	 * @param directory the root prefix to serve from in file system, e.g.
	 * "/Users/me/resources"
	 *
	 * @return this {@link HttpServerRoutes}
	 */
	default HttpServerRoutes directory(String uri, File directory) {
		return directory(uri, directory, null);
	}

	/**
	 * Listen for HTTP GET on the passed path to be used as a routing condition.The
	 * content of the provided {@link File directory} will be served.
	 * <p>
	 * Additional regex matching is available, e.g. "/test/{param}". Params are resolved
	 * using {@link HttpServerRequest#param(CharSequence)}
	 *
	 * @param uri The GET path used by clients
	 * @param directory the root prefix to serve from in file system, e.g.
	 * "/Users/me/resources"
	 * @param interceptor a pre response processor
	 *
	 * @return this {@link HttpServerRoutes}
	 */
	HttpServerRoutes directory(String uri, File directory,
			Function<HttpServerResponse, HttpServerResponse> interceptor);

	/**
	 * Listen for HTTP GET on the passed path to be used as a routing condition. The
	 * provided {@link java.io.File} will be served.
	 * <p>
	 * Additional regex matching is available, e.g. "/test/{param}". Params are resolved
	 * using {@link HttpServerRequest#param(CharSequence)}
	 *
	 * @param uri The GET path used by clients
	 * @param file the {@link File} to serve
	 *
	 * @return this {@link HttpServerRoutes}
	 */
	default HttpServerRoutes file(String uri, File file) {
		return file(HttpPredicate.get(uri), file, null);
	}

	/**
	 * Listen for HTTP GET on the passed path to be used as a routing condition. The file
	 * at provided path will be served.
	 * <p>
	 * Additional regex matching is available, e.g. "/test/{param}". Params are resolved
	 * using {@link HttpServerRequest#param(CharSequence)}
	 *
	 * @param uri The GET path used by clients
	 * @param path the resource path to serve
	 *
	 * @return this {@link HttpServerRoutes}
	 */
	default HttpServerRoutes file(String uri, String path) {
		return file(HttpPredicate.get(uri), new File(path), null);
	}

	/**
	 * Listen for HTTP GET on the passed path to be used as a routing condition. The
	 * file on the provided {@link File} is served.
	 * <p>
	 * Additional regex matching is available e.g.
	 * "/test/{param}". Params are resolved using {@link HttpServerRequest#param(CharSequence)}
	 *
	 * @param uri The {@link HttpPredicate} to use to trigger the route, pattern matching
	 * and capture are supported
	 * @param file the {@link File} to serve
	 * @param interceptor a channel pre-intercepting handler e.g. for content type header
	 *
	 * @return this {@link HttpServerRoutes}
	 */
	default HttpServerRoutes file(Predicate<HttpServerRequest> uri, File file,
			Function<HttpServerResponse, HttpServerResponse> interceptor) {
		ObjectHelper.requireNonNull(file, "file");
		return route(uri, (req, resp) -> {
			if (!file.canRead()) {
				return resp.send(ByteBufFlowable.fromFile(file));
			}
			if (interceptor != null) {
				return interceptor.apply(resp)
				                  .sendFile(file);
			}
			return resp.sendFile(file);
		});
	}

	/**
	 * Listen for HTTP GET on the passed path to be used as a routing condition. Incoming
	 * connections will query the internal registry to invoke the matching handlers. <p>
	 * Additional regex matching is available e.g.
	 * "/test/{param}". Params are resolved using {@link HttpServerRequest#param(CharSequence)}
	 *
	 * @param path The GET path used by clients
	 * @param handler an handler to invoke for the given condition
	 *
	 * @return this {@link HttpServerRoutes}
	 */
	default HttpServerRoutes get(String path,
			BiFunction<? super HttpServerRequest, ? super HttpServerResponse, ? extends Publisher<Void>> handler) {
		return route(HttpPredicate.get(path), handler);
	}

	/**
	 * This route will be invoked when GET "/path" or "/path/" like uri are requested.
	 *
	 * @param handler an handler to invoke on index/root request
	 *
	 * @return this {@link HttpServerRoutes}
	 */
	default HttpServerRoutes index(final BiFunction<? super HttpServerRequest, ? super HttpServerResponse, ? extends Publisher<Void>> handler) {
		return route(INDEX_PREDICATE, handler);
	}

	/**
	 * Listen for HTTP POST on the passed path to be used as a routing condition. Incoming
	 * connections will query the internal registry to invoke the matching handlers. <p>
	 * Additional regex matching is available e.g.
	 * "/test/{param}". Params are resolved using {@link HttpServerRequest#param(CharSequence)}
	 *
	 * @param path The POST path used by clients
	 * @param handler an handler to invoke for the given condition
	 *
	 * @return this {@link HttpServerRoutes}
	 */
	default HttpServerRoutes post(String path,
			BiFunction<? super HttpServerRequest, ? super HttpServerResponse, ? extends Publisher<Void>> handler) {
		return route(HttpPredicate.post(path), handler);
	}

	/**
	 * Listen for HTTP PUT on the passed path to be used as a routing condition. Incoming
	 * connections will query the internal registry to invoke the matching handlers. <p>
	 * Additional regex matching is available e.g.
	 * "/test/{param}". Params are resolved using {@link HttpServerRequest#param(CharSequence)}
	 *
	 * @param path The PUT path used by clients
	 * @param handler an handler to invoke for the given condition
	 *
	 * @return this {@link HttpServerRoutes}
	 */
	default HttpServerRoutes put(String path,
			BiFunction<? super HttpServerRequest, ? super HttpServerResponse, ? extends Publisher<Void>> handler) {
		return route(HttpPredicate.put(path), handler);
	}

	/**
	 * A generic route predicate that if matched invoke the passed req/resp handler.
	 *
	 * @param condition a predicate given each inbound request
	 * @param handler the handler to invoke on match
	 *
	 * @return this {@link HttpServerRoutes}
	 */
	HttpServerRoutes route(Predicate<? super HttpServerRequest> condition,
			BiFunction<? super HttpServerRequest, ? super HttpServerResponse, ? extends Publisher<Void>> handler);

	/**
	 * Listen for WebSocket on the passed path to be used as a routing condition. Incoming
	 * connections will query the internal registry to invoke the matching handlers. <p>
	 * Additional regex matching is available e.g.
	 * "/test/{param}". Params are resolved using {@link HttpServerRequest#param(CharSequence)}
	 *
	 * @param path The websocket path used by clients
	 * @param handler an handler to invoke for the given condition
	 *
	 * @return this {@link HttpServerRoutes}
	 */
	default HttpServerRoutes ws(String path,
			BiFunction<? super WebsocketInbound, ? super WebsocketOutbound, ? extends
					Publisher<Void>> handler) {
		return ws(path, handler, null);
	}

	/**
	 * Listen for WebSocket on the passed path to be used as a routing condition. Incoming
	 * connections will query the internal registry to invoke the matching handlers. <p>
	 * Additional regex matching is available e.g.
	 * "/test/{param}". Params are resolved using {@link HttpServerRequest#param(CharSequence)}
	 *
	 * @param path The websocket path used by clients
	 * @param handler an handler to invoke for the given condition
	 * @param protocols sub-protocol to use in WS handshake signature
	 *
	 * @return a new handler
	 */
	@SuppressWarnings("unchecked")
	default HttpServerRoutes ws(String path,
			BiFunction<? super WebsocketInbound, ? super WebsocketOutbound, ? extends
					Publisher<Void>> handler,
			String protocols) {
		Predicate<HttpServerRequest> condition = HttpPredicate.get(path);

		return route(condition, (req, resp) -> {
			if (req.requestHeaders()
			       .contains(HttpHeaderNames.CONNECTION, HttpHeaderValues.UPGRADE, true)) {

				HttpServerOperations ops = (HttpServerOperations) req;
				return ops.withWebsocketSupport(req.uri(), protocols,
						handler);
			}
			return resp.sendNotFound();
		});
	}

	Predicate<HttpServerRequest> INDEX_PREDICATE = req -> {
		URI uri = URI.create(req.uri());
		return req.method() == HttpMethod.GET && (uri.getPath()
		                                             .endsWith("/") || uri.getPath()
		                                                                  .indexOf(".",
				                                                                  uri.getPath()
				                                                                     .lastIndexOf(
						                                                                     "/")) == -1);
	};

}
