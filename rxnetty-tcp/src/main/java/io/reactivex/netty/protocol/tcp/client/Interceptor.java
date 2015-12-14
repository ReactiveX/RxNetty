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
 *
 */

package io.reactivex.netty.protocol.tcp.client;

import io.reactivex.netty.client.ConnectionProvider;

/**
 * An interceptor that preserves the type of objects read and written to the connection.
 *
 * @param <R> Type of objects read from the connection handled by this interceptor.
 * @param <W> Type of objects written to the connection handled by this interceptor.
 */
public interface Interceptor<W, R> {

    ConnectionProvider<W, R> intercept(ConnectionProvider<W, R> provider);

}
