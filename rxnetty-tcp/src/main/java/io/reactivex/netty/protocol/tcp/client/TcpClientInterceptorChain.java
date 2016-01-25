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

/**
 * Interceptor chain for {@link TcpClient}, obtained via {@link TcpClient#intercept()}. <p>
 *
 * Multiple interceptors can be added to this chain by using the various {@code next*()} methods available, before
 * calling {@link #finish()} that returns a new {@link TcpClient} which inherits all the configuration from the parent
 * client (from which this chain was created) and adds these interceptors.
 *
 * <h2>Order of execution</h2>
 *
 * Interceptors are executed in the order in which they are added.
 *
 * @param <W> The type of objects written to the client created by this chain.
 * @param <R> The type of objects read from the client created by this chain.
 */
public interface TcpClientInterceptorChain<W, R> {

    /**
     * Adds a simple interceptor that does not change the type of objects read/written to a connection.
     *
     * @param interceptor Interceptor to add.
     *
     * @return {@code this}
     */
    TcpClientInterceptorChain<W, R> next(Interceptor<W, R> interceptor);

    /**
     * Adds an interceptor that changes the type of objects read from the connections created by the client provided by
     * this chain.
     *
     * @param interceptor Interceptor to add.
     *
     * @return A new chain instance.
     */
    <RR> TcpClientInterceptorChain<W, RR> nextWithReadTransform(TransformingInterceptor<W, R, W, RR> interceptor);

    /**
     * Adds an interceptor that changes the type of objects written to the connections created by the client provided by
     * this chain.
     *
     * @param interceptor Interceptor to add.
     *
     * @return A new chain instance.
     */
    <WW> TcpClientInterceptorChain<WW, R> nextWithWriteTransform(TransformingInterceptor<W, R, WW, R> interceptor);

    /**
     * Adds an interceptor that changes the type of objects read and written to the connections created by the client
     * provided by this chain.
     *
     * @param interceptor Interceptor to add.
     *
     * @return A new chain instance.
     */
    <WW, RR> TcpClientInterceptorChain<WW, RR> nextWithTransform(TransformingInterceptor<W, R, WW, RR> interceptor);

    /**
     * Finish the addition of interceptors and create a new client instance.
     *
     * @return New client instance which inherits all the configuration from the parent client
     * (from which this chain was created) and adds these interceptors.
     */
    InterceptingTcpClient<W, R> finish();
}
