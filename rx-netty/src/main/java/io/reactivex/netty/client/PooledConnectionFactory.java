/*
 * Copyright 2014 Netflix, Inc.
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
package io.reactivex.netty.client;

import io.netty.channel.ChannelHandlerContext;

/**
 * @author Nitesh Kant
 */
public class PooledConnectionFactory<I, O> implements ClientConnectionFactory<I, O, PooledConnection<I, O>> {

    private final PoolConfig poolConfig;

    public PooledConnectionFactory(PoolConfig poolConfig) {
        this.poolConfig = poolConfig;
    }

    @Override
    public PooledConnection<I, O> newConnection(ChannelHandlerContext ctx) {
        return new PooledConnection<I, O>(ctx, poolConfig.getMaxIdleTimeMillis());
    }
}
