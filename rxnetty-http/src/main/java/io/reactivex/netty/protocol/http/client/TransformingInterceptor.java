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

/**
 * An interceptor that preserves the type of client request and response content.
 *
 * @param <I> The type of the content of request.
 * @param <O> The type of the content of response.
 */
public interface TransformingInterceptor<I, O, II, OO> {

    /**
     * Intercepts and changes the passed {@code RequestProvider}.
     *
     * @param provider Provider to intercept.
     *
     * @return Provider to use after this transformation.
     */
    RequestProvider<II, OO> intercept(RequestProvider<I, O> provider);

}
