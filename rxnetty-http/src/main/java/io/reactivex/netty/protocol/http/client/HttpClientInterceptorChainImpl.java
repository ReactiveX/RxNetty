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

import io.reactivex.netty.protocol.http.client.events.HttpClientEventPublisher;

final class HttpClientInterceptorChainImpl<I, O> implements HttpClientInterceptorChain<I, O> {

    private final RequestProvider<I, O> rp;
    private final HttpClientEventPublisher cep;

    HttpClientInterceptorChainImpl(RequestProvider<I, O> rp, HttpClientEventPublisher cep) {
        this.rp = rp;
        this.cep = cep;
    }

    @Override
    public HttpClientInterceptorChain<I, O> next(Interceptor<I, O> i) {
        return new HttpClientInterceptorChainImpl<>(i.intercept(rp), cep);
    }

    @Override
    public <OO> HttpClientInterceptorChain<I, OO> nextWithReadTransform(TransformingInterceptor<I, O, I, OO> i) {
        return new HttpClientInterceptorChainImpl<>(i.intercept(rp), cep);
    }

    @Override
    public <II> HttpClientInterceptorChain<II, O> nextWithWriteTransform(TransformingInterceptor<I, O, II, O> i) {
        return new HttpClientInterceptorChainImpl<>(i.intercept(rp), cep);
    }

    @Override
    public <II, OO> HttpClientInterceptorChain<II, OO> nextWithTransform(TransformingInterceptor<I, O, II, OO> i) {
        return new HttpClientInterceptorChainImpl<>(i.intercept(rp), cep);
    }

    @Override
    public InterceptingHttpClient<I, O> finish() {
        return new InterceptingHttpClientImpl<>(rp, cep);
    }
}
