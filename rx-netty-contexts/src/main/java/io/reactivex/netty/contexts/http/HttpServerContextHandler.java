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

package io.reactivex.netty.contexts.http;

import com.netflix.server.context.BiDirectional;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.reactivex.netty.contexts.ContextAttributeStorageHelper;
import io.reactivex.netty.contexts.ContextKeySupplier;
import io.reactivex.netty.contexts.ContextsContainer;
import io.reactivex.netty.contexts.ContextsContainerImpl;
import io.reactivex.netty.contexts.RequestCorrelator;
import io.reactivex.netty.contexts.RequestIdProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * This handler does the following:
 * <ul>
 <li>Read any serialized contexts in the incoming HTTP requests. If any contexts are found, it will add it to the
 {@link ContextsContainer} for that request. This will create the {@link ContextsContainer} instance using the factory
 method {@link #newContextContainer(ContextKeySupplier)}. In case, any changes to the implementation of
 {@link ContextsContainer} is required, it should be done via overriding this factory method.</li>
 <li>Write any {@link BiDirectional} contexts which are modified back to the response. The modified contexts are found
 by the method {@link ContextsContainer#getModifiedBidirectionalContexts()}</li>
 </ul>
 *
 * @author Nitesh Kant
 */
@ChannelHandler.Sharable
public class HttpServerContextHandler extends ChannelDuplexHandler {

    private static final Logger logger = LoggerFactory.getLogger(HttpServerContextHandler.class);

    private final RequestIdProvider requestIdProvider;
    private final RequestCorrelator correlator;

    public HttpServerContextHandler(RequestIdProvider requestIdProvider, RequestCorrelator correlator) {
        if (null == requestIdProvider) {
            throw new IllegalArgumentException("Request Id Provider can not be null.");
        }
        if (null == correlator) {
            throw new IllegalArgumentException("Request correlator can not be null.");
        }
        this.correlator = correlator;
        this.requestIdProvider = requestIdProvider;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) msg;
            ContextKeySupplier keySupplier = new HttpContextKeySupplier(request.headers());
            String requestId = requestIdProvider.onServerRequest(keySupplier, ctx);
            if (null == requestId) {
                requestId = requestIdProvider.newRequestId(keySupplier, ctx);
            }

            ContextsContainer contextsContainer = newContextContainer(keySupplier);
            ContextAttributeStorageHelper.setContainer(ctx, requestId, contextsContainer);

            correlator.onNewServerRequest(requestId, contextsContainer);
        }

        super.channelRead(ctx, msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) msg;
            ContextKeySupplier keySupplier = new HttpContextKeySupplier(response.headers());
            String requestId = requestIdProvider.beforeServerResponse(keySupplier, ctx);
            if (null != requestId) {
                ContextsContainer container = ContextAttributeStorageHelper.getContainer(ctx, requestId);
                if (null != container) {
                    Map<String,String> modifiedCtxs = container.getModifiedBidirectionalContexts();
                    for (Map.Entry<String, String> modifiedCtxEntry : modifiedCtxs.entrySet()) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Added a modified bi-directional header. Name: " + modifiedCtxEntry.getKey()
                                         + ", value: " + modifiedCtxEntry.getValue());
                        }
                        response.headers().set(modifiedCtxEntry.getKey(), modifiedCtxEntry.getValue());
                    }
                }
            }
        }

        super.write(ctx, msg, promise);
    }

    protected ContextsContainer newContextContainer(ContextKeySupplier keySupplier) {
        return new ContextsContainerImpl(keySupplier);
    }
}
