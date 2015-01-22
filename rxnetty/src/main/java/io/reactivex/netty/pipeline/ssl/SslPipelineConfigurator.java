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
 */
package io.reactivex.netty.pipeline.ssl;

import io.netty.channel.ChannelPipeline;
import io.netty.handler.ssl.SslHandler;
import io.reactivex.netty.pipeline.PipelineConfigurator;

/**
 * @author Tomasz Bak
 */
public class SslPipelineConfigurator<I, O> implements PipelineConfigurator<I, O> {

    public static final String SSL_HANDLER_NAME = "ssl-handler";
    public static final String SSL_COMPLETION_HANDLER_NAME = "ssl-completion-handler";
    private final SSLEngineFactory sslEngineFactory;

    public SslPipelineConfigurator(SSLEngineFactory sslEngineFactory) {
        this.sslEngineFactory = sslEngineFactory;
    }

    @Override
    public void configureNewPipeline(ChannelPipeline pipeline) {
        final SslHandler sslHandler = new SslHandler(sslEngineFactory.createSSLEngine(pipeline.channel().alloc()));
        pipeline.addFirst(SSL_HANDLER_NAME, sslHandler);
        pipeline.addAfter(SSL_HANDLER_NAME, SSL_COMPLETION_HANDLER_NAME,
                          new SslCompletionHandler(sslHandler.handshakeFuture()));
    }
}
