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
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import io.reactivex.Flowable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.internal.functions.ObjectHelper;
import org.reactivestreams.Publisher;

/**
 * @author Stephane Maldini
 */
final class DefaultHttpServerRoutes implements HttpServerRoutes {


	private final CopyOnWriteArrayList<HttpRouteHandler> handlers =
			new CopyOnWriteArrayList<>();

	@Override
	public HttpServerRoutes directory(String uri, File directory,
			Function<HttpServerResponse, HttpServerResponse> interceptor) {
		ObjectHelper.requireNonNull(directory, "directory");
		return route(HttpPredicate.prefix(uri), (req, resp) -> {

			String prefix = URI.create(req.uri())
			                   .getPath()
			                   .replaceFirst(uri, "");

			if(prefix.charAt(0) == '/'){
				prefix = prefix.substring(1);
			}

			File f = new File(directory, prefix);
			if (f.canRead()) {

				if (interceptor != null) {
					return interceptor.apply(resp)
					                  .sendFile(f);
				}
				return resp.sendFile(f);
			}

			return resp.sendNotFound();
		});
	}

	@Override
	public HttpServerRoutes route(Predicate<? super HttpServerRequest> condition,
			BiFunction<? super HttpServerRequest, ? super HttpServerResponse, ? extends Publisher<Void>> handler) {
		ObjectHelper.requireNonNull(condition, "condition");
		ObjectHelper.requireNonNull(handler, "handler");

		if (condition instanceof HttpPredicate) {
			handlers.add(new HttpRouteHandler(condition,
					handler,
					(HttpPredicate) condition));
		}
		else {
			handlers.add(new HttpRouteHandler(condition, handler, null));
		}
		return this;
	}

	@Override
	public Publisher<Void> apply(HttpServerRequest request, HttpServerResponse response) {
		final Iterator<HttpRouteHandler> iterator = handlers.iterator();
		HttpRouteHandler cursor;

		try {
			while (iterator.hasNext()) {
				cursor = iterator.next();
				if (cursor.test(request)) {
					return cursor.apply(request, response);
				}
			}
		}
		catch (Throwable t) {
			Exceptions.throwIfFatal(t);
			return Flowable.error(t); //500
		}

		return response.sendNotFound();
	}

	/**
	 */
	static final class HttpRouteHandler
			implements BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>>,
			           Predicate<HttpServerRequest> {

		final Predicate<? super HttpServerRequest>          condition;
		final BiFunction<? super HttpServerRequest, ? super HttpServerResponse, ? extends Publisher<Void>>
		                                                    handler;
		final Function<? super String, Map<String, String>> resolver;

		HttpRouteHandler(Predicate<? super HttpServerRequest> condition,
				BiFunction<? super HttpServerRequest, ? super HttpServerResponse, ? extends Publisher<Void>> handler,
				Function<? super String, Map<String, String>> resolver) {
			this.condition = ObjectHelper.requireNonNull(condition, "condition");
			this.handler = ObjectHelper.requireNonNull(handler, "handler");
			this.resolver = resolver;
		}

		@Override
		public Publisher<Void> apply(HttpServerRequest request,
				HttpServerResponse response) {
			return Flowable.defer(() -> handler.apply(request.paramsResolver(resolver), response));
		}

		@Override
		public boolean test(HttpServerRequest o) throws Exception {
			return condition.test(o);
		}
	}

}
