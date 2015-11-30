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
package io.reactivex.netty.protocol.http.context.server;

import io.netty.channel.ChannelPipeline;
import io.reactivex.netty.contexts.RequestCorrelator;
import io.reactivex.netty.contexts.RequestIdProvider;
import io.reactivex.netty.protocol.http.server.HttpServer;
import rx.functions.Action1;

/**
 * Action to configure an {@link HttpServer} with the context handling.
 */
public class HttpServerContextConfigurator implements Action1<ChannelPipeline> {

    public static final String CTX_HANDLER_NAME = "http-server-context-handler";

    private final RequestCorrelator correlator;
    private final RequestIdProvider requestIdProvider;

    public HttpServerContextConfigurator(RequestIdProvider requestIdProvider, RequestCorrelator correlator) {
        this.requestIdProvider = requestIdProvider;
        this.correlator = correlator;
    }

    @Override
    public void call(ChannelPipeline pipeline) {
        pipeline.addLast(CTX_HANDLER_NAME, new HttpServerContextHandler(requestIdProvider, correlator));
    }
}
