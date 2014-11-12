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
package io.reactivex.netty.contexts;

import com.netflix.server.context.BiDirectional;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * A generic handler for all protocols that can handle {@link ContextsContainer}s for servers. This handler does the
 * following:
 *
 * <ul>
 <li>Read any serialized contexts in the incoming HTTP requests. If any contexts are found, it will add it to the
 {@link ContextsContainer} for that request. This will create the {@link ContextsContainer} instance using the factory
 method {@link #newContextContainer(ContextKeySupplier)}. In case, any changes to the implementation of
 {@link ContextsContainer} is required, it should be done via overriding this factory method.</li>
 <li>Write any {@link BiDirectional} contexts which are modified back to the response. The modified contexts are found
 by the method {@link ContextsContainer#getModifiedBidirectionalContexts()}</li>
 </ul>
 *
 * All protocol specific actions are delegated to the abstract methods.
 *
 * @param <R> The type of object this handler will read.
 * @param <W> The type of object this handler will write.
 *
 * @author Nitesh Kant
 */
public abstract class AbstractServerContextHandler<R, W> extends AbstractContextHandler<R, W> {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractServerContextHandler.class);

    protected final RequestIdProvider requestIdProvider;
    protected final RequestCorrelator correlator;

    protected AbstractServerContextHandler(RequestCorrelator correlator, RequestIdProvider requestIdProvider) {
        this.correlator = correlator;
        this.requestIdProvider = requestIdProvider;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (isAcceptableToRead(msg)) {
            @SuppressWarnings("unchecked")
            R request = (R) msg;
            ContextKeySupplier keySupplier = newKeySupplierForRead(request);
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
        try {
            if (isAcceptableToWrite(msg)) {
                @SuppressWarnings("unchecked")
                W response = (W) msg;
                ContextKeySupplier keySupplier = newKeySupplierForWrite(response);
                String requestId = requestIdProvider.beforeServerResponse(keySupplier, ctx);
                if (null != requestId) {
                    newRequestIdWritten(requestId);
                    ContextsContainer container = ContextAttributeStorageHelper.getContainer(ctx, requestId);
                    if (null != container) {
                        Map<String,String> modifiedCtxs = container.getModifiedBidirectionalContexts();
                        for (Map.Entry<String, String> modifiedCtxEntry : modifiedCtxs.entrySet()) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Added a modified bi-directional context key. Name: " + modifiedCtxEntry.getKey()
                                             + ", value: " + modifiedCtxEntry.getValue());
                            }
                            addKey(response, modifiedCtxEntry.getKey(), modifiedCtxEntry.getValue());
                        }
                    }
                }
            }

            super.write(ctx, msg, promise);
        } finally {
            String currentRequestId = getCurrentlyProcessingRequestId();
            if (null != currentRequestId && isLastResponseFragmenTotWrite(msg)) {
                correlator.onClientProcessingEnd(currentRequestId);
            }
        }
    }

    protected abstract void newRequestIdWritten(String requestId);

    protected abstract String getCurrentlyProcessingRequestId();

    protected abstract boolean isLastResponseFragmenTotWrite(Object response);

    protected ContextsContainer newContextContainer(ContextKeySupplier keySupplier) {
        return new ContextsContainerImpl(keySupplier);
    }
}
