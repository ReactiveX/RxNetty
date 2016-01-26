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
import io.reactivex.netty.events.EventSource;
import io.reactivex.netty.protocol.http.client.events.HttpClientEventsListener;

public abstract class InterceptingHttpClient<I, O> implements EventSource<HttpClientEventsListener> {

    /**
     * Creates a GET request for the passed URI.
     *
     * @param uri The URI for the request. The URI should be absolute and should not contain the scheme, host and port.
     *
     * @return New {@link HttpClientRequest}.
     */
    public abstract HttpClientRequest<I, O> createGet(String uri);

    /**
     * Creates a POST request for the passed URI.
     *
     * @param uri The URI for the request. The URI should be absolute and should not contain the scheme, host and port.
     *
     * @return New {@link HttpClientRequest}.
     */
    public abstract HttpClientRequest<I, O> createPost(String uri);

    /**
     * Creates a PUT request for the passed URI.
     *
     * @param uri The URI for the request. The URI should be absolute and should not contain the scheme, host and port.
     *
     * @return New {@link HttpClientRequest}.
     */
    public abstract HttpClientRequest<I, O> createPut(String uri);

    /**
     * Creates a DELETE request for the passed URI.
     *
     * @param uri The URI for the request. The URI should be absolute and should not contain the scheme, host and port.
     *
     * @return New {@link HttpClientRequest}.
     */
    public abstract HttpClientRequest<I, O> createDelete(String uri);

    /**
     * Creates a HEAD request for the passed URI.
     *
     * @param uri The URI for the request. The URI should be absolute and should not contain the scheme, host and port.
     *
     * @return New {@link HttpClientRequest}.
     */
    public abstract HttpClientRequest<I, O> createHead(String uri);

    /**
     * Creates an OPTIONS request for the passed URI.
     *
     * @param uri The URI for the request. The URI should be absolute and should not contain the scheme, host and port.
     *
     * @return New {@link HttpClientRequest}.
     */
    public abstract HttpClientRequest<I, O> createOptions(String uri);

    /**
     * Creates a PATCH request for the passed URI.
     *
     * @param uri The URI for the request. The URI should be absolute and should not contain the scheme, host and port.
     *
     * @return New {@link HttpClientRequest}.
     */
    public abstract HttpClientRequest<I, O> createPatch(String uri);

    /**
     * Creates a TRACE request for the passed URI.
     *
     * @param uri The URI for the request. The URI should be absolute and should not contain the scheme, host and port.
     *
     * @return New {@link HttpClientRequest}.
     */
    public abstract HttpClientRequest<I, O> createTrace(String uri);

    /**
     * Creates a CONNECT request for the passed URI.
     *
     * @param uri The URI for the request. The URI should be absolute and should not contain the scheme, host and port.
     *
     * @return New {@link HttpClientRequest}.
     */
    public abstract HttpClientRequest<I, O> createConnect(String uri);

    /**
     * Creates a request for the passed HTTP method and URI.
     *
     * @param method Http Method.
     * @param uri The URI for the request. The URI should be absolute and should not contain the scheme, host and port.
     *
     * @return New {@link HttpClientRequest}.
     */
    public abstract HttpClientRequest<I, O> createRequest(HttpMethod method, String uri);

    /**
     * Creates a request for the passed HTTP version, method and URI.
     *
     * @param version HTTP version
     * @param method Http Method.
     * @param uri The URI for the request. The URI should be absolute and should not contain the scheme, host and port.
     *
     * @return New {@link HttpClientRequest}.
     */
    public abstract HttpClientRequest<I, O> createRequest(HttpVersion version, HttpMethod method, String uri);

    /**
     * Starts the process of adding interceptors to this client. Interceptors help in achieving various usecases of
     * instrumenting and transforming connections.
     *
     * @return A new interceptor chain to add the various interceptors.
     */
    public abstract HttpClientInterceptorChain<I, O> intercept();
}
