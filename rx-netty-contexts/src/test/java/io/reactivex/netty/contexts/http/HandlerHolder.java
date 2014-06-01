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

import com.netflix.server.context.ContextSerializationException;
import com.netflix.server.context.ContextSerializer;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.util.AttributeMap;
import io.reactivex.netty.contexts.ContextKeySupplier;
import io.reactivex.netty.contexts.ContextsContainer;
import io.reactivex.netty.contexts.ContextsContainerImpl;
import io.reactivex.netty.contexts.MapBackedKeySupplier;
import io.reactivex.netty.contexts.NoOpChannelHandlerContext;
import io.reactivex.netty.contexts.RequestCorrelator;
import io.reactivex.netty.contexts.RequestIdGenerator;
import io.reactivex.netty.contexts.ThreadLocalRequestCorrelator;

import java.util.Map;

/**
* @author Nitesh Kant
*/
class HandlerHolder {

    final String requestId;
    final HttpRequestIdProvider provider;
    final MapBackedKeySupplier keySupplier;
    final ChannelDuplexHandler handler;
    final NoOpChannelHandlerContext ctx = new NoOpChannelHandlerContext();
    final RequestCorrelator correlator;

    HandlerHolder(boolean server, final String reqId) {
        String headerName = "requestId";
        correlator = new ThreadLocalRequestCorrelator();
        requestId = reqId;
        RequestIdGenerator generator = new RequestIdGenerator() {
            @Override
            public String newRequestId(ContextKeySupplier keySupplier, AttributeMap channelAttributeMap) {
                return requestId;
            }
        };

        provider = new HttpRequestIdProvider(generator, correlator, headerName);
        keySupplier = new MapBackedKeySupplier();
        if (server) {
            handler = new HttpServerContextHandler(provider, correlator);
        } else {
            handler = new HttpClientContextHandler(provider, correlator);
        }
    }

    public String getRequestId() {
        return requestId;
    }

    public HttpRequestIdProvider getProvider() {
        return provider;
    }

    public MapBackedKeySupplier getKeySupplier() {
        return keySupplier;
    }

    public ChannelDuplexHandler getHandler() {
        return handler;
    }

    public void addSerializedContext(HttpMessage httpMessage, String ctxName, String strCtxValue)
    throws ContextSerializationException {
        ContextsContainer contextsContainer = new ContextsContainerImpl(keySupplier);
        contextsContainer.addContext(ctxName, strCtxValue);
        Map<String,String> serializedContexts = contextsContainer.getSerializedContexts();
        for (Map.Entry<String, String> entry : serializedContexts.entrySet()) {
            httpMessage.headers().add(entry.getKey(), entry.getValue());
        }
    }

    public <T> void addSerializedContext(HttpMessage httpMessage, String ctxName, T ctx,
                                     ContextSerializer<T> serializer) throws ContextSerializationException {
        ContextsContainer contextsContainer = new ContextsContainerImpl(keySupplier);
        contextsContainer.addContext(ctxName, ctx, serializer);
        Map<String,String> serializedContexts = contextsContainer.getSerializedContexts();
        for (Map.Entry<String, String> entry : serializedContexts.entrySet()) {
            httpMessage.headers().add(entry.getKey(), entry.getValue());
        }
    }
}
