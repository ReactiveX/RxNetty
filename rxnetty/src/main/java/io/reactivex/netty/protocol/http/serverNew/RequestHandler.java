/*
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.reactivex.netty.protocol.http.serverNew;

import rx.Observable;

/**
 * A handler for an {@link HttpServerRequest} to produce a {@link HttpServerResponse}
 *
 * @param <I> The type of objects received as content from the request.
 * @param <O> The type of objects written as content from the response.
 */
public interface RequestHandler<I, O> {

    /**
     * Provides a request and response pair to process.
     *
     * @param request Http request to process.
     * @param response Http response to populate after processing the request.
     *
     * @return An {@link Observable} that represents the processing of the request. Subscribing to this should start
     * the request processing and unsubscribing should cancel the processing.
     */
    Observable<Void> handle(HttpServerRequest<I> request, HttpServerResponse<O> response);
}
