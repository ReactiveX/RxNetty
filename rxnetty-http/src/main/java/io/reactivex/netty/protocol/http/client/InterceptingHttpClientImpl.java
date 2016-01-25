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
import io.reactivex.netty.protocol.http.client.events.HttpClientEventPublisher;
import io.reactivex.netty.protocol.http.client.events.HttpClientEventsListener;
import rx.Subscription;

class InterceptingHttpClientImpl<I, O> extends InterceptingHttpClient<I, O> {

    private final RequestProvider<I, O> requestProvider;
    private final HttpClientEventPublisher cep;

    public InterceptingHttpClientImpl(RequestProvider<I, O> requestProvider, HttpClientEventPublisher cep) {
        this.requestProvider = requestProvider;
        this.cep = cep;
    }

    @Override
    public HttpClientRequest<I, O> createGet(String uri) {
        return createRequest(HttpMethod.GET, uri);
    }

    @Override
    public HttpClientRequest<I, O> createPost(String uri) {
        return createRequest(HttpMethod.POST, uri);
    }

    @Override
    public HttpClientRequest<I, O> createPut(String uri) {
        return createRequest(HttpMethod.PUT, uri);
    }

    @Override
    public HttpClientRequest<I, O> createDelete(String uri) {
        return createRequest(HttpMethod.DELETE, uri);
    }

    @Override
    public HttpClientRequest<I, O> createHead(String uri) {
        return createRequest(HttpMethod.HEAD, uri);
    }

    @Override
    public HttpClientRequest<I, O> createOptions(String uri) {
        return createRequest(HttpMethod.OPTIONS, uri);
    }

    @Override
    public HttpClientRequest<I, O> createPatch(String uri) {
        return createRequest(HttpMethod.PATCH, uri);
    }

    @Override
    public HttpClientRequest<I, O> createTrace(String uri) {
        return createRequest(HttpMethod.TRACE, uri);
    }

    @Override
    public HttpClientRequest<I, O> createConnect(String uri) {
        return createRequest(HttpMethod.CONNECT, uri);
    }

    @Override
    public HttpClientRequest<I, O> createRequest(HttpMethod method, String uri) {
        return createRequest(HttpVersion.HTTP_1_1, method, uri);
    }

    @Override
    public HttpClientRequest<I, O> createRequest(HttpVersion version, HttpMethod method, String uri) {
        return requestProvider.createRequest(version, method, uri);
    }

    @Override
    public HttpClientInterceptorChain<I, O> intercept() {
        return new HttpClientInterceptorChainImpl<>(requestProvider, cep);
    }

    @Override
    public Subscription subscribe(HttpClientEventsListener listener) {
        return cep.subscribe(listener);
    }
}
