/*
 * Copyright 2016 Netflix, Inc.
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
 *
 */

package io.reactivex.netty.protocol.http.client;

import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

/**
 * An abstraction that creates new instance of {@link HttpClientRequest}.
 *
 * @param <I> The type of the content of request.
 * @param <O> The type of the content of response.
 */
public interface RequestProvider<I, O> {

    /**
     * Creates a new {@link HttpClientRequest} with the provided {@code version}, {@code method} and {@code uri}
     *
     * @param version HTTP version.
     * @param method HTTP method.
     * @param uri URI.
     *
     * @return A new instance of {@code HttpClientRequest}
     */
    HttpClientRequest<I, O> createRequest(HttpVersion version, HttpMethod method, String uri);
}
