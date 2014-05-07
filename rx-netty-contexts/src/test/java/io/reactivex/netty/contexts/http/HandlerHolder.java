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

    HandlerHolder(boolean server) {
        String headerName = "requestId";
        correlator = new ThreadLocalRequestCorrelator();
        requestId = "baaaa";
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
