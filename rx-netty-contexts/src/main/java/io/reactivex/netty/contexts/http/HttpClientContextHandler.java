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

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.reactivex.netty.contexts.RequestCorrelator;
import io.reactivex.netty.contexts.ContextAttributeStorageHelper;
import io.reactivex.netty.contexts.ContextKeySupplier;
import io.reactivex.netty.contexts.ContextsContainer;
import io.reactivex.netty.contexts.RequestIdProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * This handler does the following:
 *
 * <ul>
 <li>Writes any contexts available in {@link ContextsContainer} for the request id obtained by
 {@link RequestIdProvider}</li>
 <li>Reads any contexts written back from the server by calling
 {@link ContextsContainer#consumeBidirectionalContextsFromResponse(ContextKeySupplier)}</li>
 </ul>
 *
 * @author Nitesh Kant
 */
public class HttpClientContextHandler extends ChannelDuplexHandler {

    private static final Logger logger = LoggerFactory.getLogger(HttpClientContextHandler.class);

    private final RequestIdProvider requestIdProvider;
    private final RequestCorrelator contextProvider;

    public HttpClientContextHandler(RequestIdProvider requestIdProvider, RequestCorrelator contextProvider) {
        if (null == requestIdProvider) {
            throw new IllegalArgumentException("Request Id Provider can not be null.");
        }
        if (null == contextProvider) {
            throw new IllegalArgumentException("Client context provider can not be null.");
        }
        this.contextProvider = contextProvider;
        this.requestIdProvider = requestIdProvider;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) msg;
            String requestId = requestIdProvider.onClientResponse(ctx);
            if (null != requestId) {
                ContextsContainer container = ContextAttributeStorageHelper.getContainer(ctx, requestId);
                if (null != container) {
                    ContextKeySupplier keySupplier = new HttpContextKeySupplier(response.headers());
                    container.consumeBidirectionalContextsFromResponse(keySupplier);
                }
            }
        }
        super.channelRead(ctx, msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {

        if (msg instanceof HttpRequest) {
            String requestId = requestIdProvider.beforeClientRequest(ctx);
            if (null != requestId) {
                ContextsContainer container = contextProvider.getContextForClientRequest(requestId, ctx);
                ContextAttributeStorageHelper.setContainer(ctx, requestId, container);

                if (null != container) {
                    HttpRequest request = (HttpRequest) msg;
                    Map<String,String> serializedContexts = container.getSerializedContexts();
                    for (Map.Entry<String, String> entry : serializedContexts.entrySet()) {
                        request.headers().set(entry.getKey(), entry.getValue());
                        if (logger.isDebugEnabled()) {
                            logger.debug("Added an outbound context header. Name: " + entry.getKey() +
                                         ", value: " + entry.getValue());
                        }
                    }
                }
            }
        }

        super.write(ctx, msg, promise);
    }
}
