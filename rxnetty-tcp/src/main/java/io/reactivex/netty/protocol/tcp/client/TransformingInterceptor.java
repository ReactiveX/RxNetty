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

package io.reactivex.netty.protocol.tcp.client;

import io.reactivex.netty.client.ConnectionProvider;

/**
 * An interceptor that changes the type of objects read and written to the connection.
 *
 * @param <R> Type of objects read from the connection before applying this interceptor.
 * @param <W> Type of objects written to the connection before applying this interceptor.
 * @param <RR> Type of objects read from the connection after applying this interceptor.
 * @param <WW> Type of objects written to the connection after applying this interceptor.
 */
public interface TransformingInterceptor<W, R, WW, RR> {

    /**
     * Intercepts and changes the passed {@code ConnectionProvider}.
     *
     * @param provider Provider to intercept.
     *
     * @return Provider to use after this transformation.
     */
    ConnectionProvider<WW, RR> intercept(ConnectionProvider<W, R> provider);

}
