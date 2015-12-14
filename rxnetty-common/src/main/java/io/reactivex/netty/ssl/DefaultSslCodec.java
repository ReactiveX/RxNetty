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
package io.reactivex.netty.ssl;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.ssl.SslHandler;
import rx.functions.Func1;

import javax.net.ssl.SSLEngine;

/**
 * Default implementation of {@link SslCodec} that uses a statically created {@link SSLEngine} or a provided factory to
 * create one dynamically.
 *
 * No custom configurations are applied to the created {@link SslHandler} unless done so explicity by overriding
 * {@link #configureHandler(SslHandler)}
 */
public class DefaultSslCodec extends SslCodec {

    private final Func1<ByteBufAllocator, SSLEngine> engineFactory;

    public DefaultSslCodec(Func1<ByteBufAllocator, SSLEngine> engineFactory) {
        this.engineFactory = engineFactory;
    }

    public DefaultSslCodec(final SSLEngine sslEngine) {
        this(new Func1<ByteBufAllocator, SSLEngine>() {
            @Override
            public SSLEngine call(ByteBufAllocator allocator) {
                return sslEngine;
            }
        });
    }

    @Override
    protected SslHandler newSslHandler(ChannelPipeline pipeline) {
        SslHandler toReturn = new SslHandler(engineFactory.call(pipeline.channel().alloc()));
        configureHandler(toReturn);
        return toReturn;
    }

    /**
     * An optional method that can be overridden to add any custom configurations to the {@link SslHandler} returned
     * by {@link #newSslHandler(ChannelPipeline)}
     *
     * @param handler Handler to configure.
     */
    protected void configureHandler(@SuppressWarnings("unused")SslHandler handler) {
        // No Op ..
    }
}
