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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

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
public abstract class AbstractClientContextHandler<R, W> extends AbstractContextHandler<R,W> {

    protected final RequestIdProvider requestIdProvider;
    protected final RequestCorrelator correlator;

    protected AbstractClientContextHandler(RequestCorrelator correlator, RequestIdProvider requestIdProvider) {
        this.correlator = correlator;
        this.requestIdProvider = requestIdProvider;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        try {
            if (isAcceptableToRead(msg)) {
                @SuppressWarnings("unchecked")
                R response = (R) msg;
                String requestId = requestIdProvider.onClientResponse(ctx);

                if (null != requestId) {
                    newRequestIdRead(requestId);
                    ContextsContainer container = ContextAttributeStorageHelper.getContainer(ctx, requestId);
                    if (null != container) {
                        ContextKeySupplier keySupplier = newKeySupplierForRead(response);
                        container.consumeBidirectionalContextsFromResponse(keySupplier);
                    }
                }
            }
            super.channelRead(ctx, msg);
        } finally {
            String currentRequestId = getCurrentlyProcessingRequestId();
            if (null != currentRequestId && isLastResponseFragmentToRead(msg)) {
                correlator.onClientProcessingEnd(currentRequestId);
            }
        }
    }

    protected abstract void newRequestIdRead(String requestId);

    protected abstract String getCurrentlyProcessingRequestId();

    protected abstract boolean isLastResponseFragmentToRead(Object response);

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {

        if (isAcceptableToWrite(msg)) {
            String requestId = requestIdProvider.beforeClientRequest(ctx);

            if (null != requestId) {
                @SuppressWarnings("unchecked")
                W request = (W) msg;

                addKey(request, requestIdProvider.getRequestIdContextKeyName(), requestId);
                ContextsContainer container = correlator.getContextForClientRequest(requestId);
                ContextAttributeStorageHelper.setContainer(ctx, requestId, container);

                if (null != container) {
                    Map<String,String> serializedContexts = container.getSerializedContexts();
                    for (Map.Entry<String, String> entry : serializedContexts.entrySet()) {
                        addKey(request, entry.getKey(), entry.getValue());
                        if (logger.isDebugEnabled()) {
                            logger.debug("Added an outbound context key. Name: " + entry.getKey() +
                                         ", value: " + entry.getValue());
                        }
                    }
                }
            }
        }

        super.write(ctx, msg, promise);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof NewContextEvent) {
            NewContextEvent newContextEvent = (NewContextEvent) evt;
            correlator.beforeNewClientRequest(newContextEvent.getRequestId(), newContextEvent.getContainer());
        }
        super.userEventTriggered(ctx, evt);
    }

    public static class NewContextEvent {

        private final String requestId;
        private final ContextsContainer container;

        public NewContextEvent(String requestId, ContextsContainer container) {
            this.requestId = requestId;
            this.container = container;
        }

        public ContextsContainer getContainer() {
            return container;
        }

        public String getRequestId() {
            return requestId;
        }
    }
}
