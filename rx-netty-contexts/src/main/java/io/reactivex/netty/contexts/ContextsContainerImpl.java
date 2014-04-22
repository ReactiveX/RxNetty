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

import com.netflix.server.context.ContextSerializationException;
import com.netflix.server.context.ContextSerializer;
import com.netflix.server.context.DirectionAwareContextSerializer;
import com.netflix.server.context.MergeableContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * An implementation of {@link ContextsContainer}<br/>
 *
 * <h2>Thread safety</h2>
 *
 * Since the contexts can be concurrently modified/added this class needs to be thread safe. <br/>
 * However, generally the concurrency will be low as typically a requests is not concurrently processed by multiple
 * threads. This is the reason, this implementation chooses to do brute-force thread-safety by making all public methods
 * synchronized. <br/>
 * <b>This context must not be used across requests.</b>
 *
 * @author Nitesh Kant
 */
public class ContextsContainerImpl implements ContextsContainer {

    private static final Logger logger = LoggerFactory.getLogger(ContextsContainerImpl.class);

    @SuppressWarnings("rawtypes") private final Map<String, ContextHolder> contexts = new HashMap<String, ContextHolder>();


    public ContextsContainerImpl(ContextKeySupplier keySupplier) {
        /*
         * This aggressively reads all the required keys so it does not have to store the keySupplier (which will
         * usualy be the request) for the entire lifetime of this container.
         * The keys are then lazily parsed.
         */
        contexts.putAll(ContextSerializationHelper.readContexts(keySupplier));
    }

    @Override
    public synchronized <T> void addContext(String contextName, T context, ContextSerializer<T> serializer) {
        if(null == contextName || contextName.trim().isEmpty() || null == context || null == serializer) {
            throw new IllegalArgumentException("None of the arguments can be null.");
        }
        final ContextHolder<T> newCtxHolder = new ContextHolder<T>(context, contextName, serializer);
        _addContext(contextName, newCtxHolder);
    }

    @Override
    public synchronized void addContext(String contextName, String context) {
        addContext(contextName, context, StringSerializer.INSTANCE);
    }

    @Override
    public synchronized boolean removeContext(String contextName) {
        if(null == contextName || contextName.trim().isEmpty()) {
            throw new IllegalArgumentException("Context name can not be null or empty.");
        }
        return null != contexts.remove(contextName);
    }

    @Override
    public synchronized <T> T getContext(String contextName) throws ContextSerializationException {
        return getContext(contextName, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized <T> T getContext(String contextName, ContextSerializer<T> serializer)
            throws ContextSerializationException {
        ContextHolder<T> contextHolder = contexts.get(contextName);
        if(null == contextHolder) {
            return null;
        }
        return ContextSerializationHelper.deserialize(contextHolder, serializer, DirectionAwareContextSerializer.Direction.Inbound);
    }

    @Override
    public synchronized Map<String, String> getSerializedContexts() throws ContextSerializationException {
        return ContextSerializationHelper.getSerializedContexts(contexts.values());
    }

    @Override
    public synchronized Map<String, String> getModifiedBidirectionalContexts() throws ContextSerializationException {
        return ContextSerializationHelper.getSerializedModifiedBidirectionalCtx(contexts.values());
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public synchronized void consumeBidirectionalContextsFromResponse(ContextKeySupplier contextKeySupplier)
            throws ContextSerializationException {
        if (null == contextKeySupplier) {
            return;
        }
        Map<String, ContextHolder> bidirectionalCtx = ContextSerializationHelper.readBidirectionalContexts(contextKeySupplier);
        for (Map.Entry<String, ContextHolder> entry : bidirectionalCtx.entrySet()) {
            ContextHolder value = entry.getValue();
            _addContext(entry.getKey(), value);
            if (logger.isDebugEnabled()) {
                logger.debug("Found and consumed a modified bi-directional header. Name: " + entry.getKey()
                             + ", value: " + entry.getValue());
            }
            value.markAsUpdatedExternally(); // We always want to pass back the bi-directional ctx that we read from a response.
        }
    }

    @SuppressWarnings("rawtypes")
    /*visible for testing*/ ContextHolder getContextHolder(String contextName) {
        return contexts.get(contextName);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> void _addContext(String contextName, ContextHolder<T> newCtxHolder){
        ContextHolder existingCtxHolder = contexts.put(contextName, newCtxHolder);
        if (newCtxHolder.isBiDirectional() && null != existingCtxHolder) {
            // Here we are de-serializing the new ctx only if we need to merge.
            // We use raw serialized form, if present in existing ctx to equate.
            if (existingCtxHolder.hasRawSerializedForm() && newCtxHolder.hasRawSerializedForm()
                && existingCtxHolder.getRawSerializedForm().equals(newCtxHolder.getRawSerializedForm())) {
                // contexts are equal, so no need to mark as updated or merge
                return;
            }

            try {
                // This call does not de-serialize if context instance is already present.
                ContextSerializationHelper.deserialize(newCtxHolder, null,
                                                       DirectionAwareContextSerializer.Direction.Inbound);
                ContextSerializationHelper.deserialize(existingCtxHolder, null,
                                                       DirectionAwareContextSerializer.Direction.Inbound);
            } catch (ContextSerializationException e) {
                logger.error(String.format("Failed to de-serialize new/existing bi-directional context %s. This will overwrite the existing bi-directional context and assume update.",
                                        contextName), e);
                newCtxHolder.markAsUpdatedExternally();
                return;
            }

            T newCtx = newCtxHolder.getContext();
            if (existingCtxHolder.getContext().getClass().equals(newCtx.getClass())
                && !existingCtxHolder.getContext().equals(newCtx)) {
                // This is the update of a bi-directional context.
                if (MergeableContext.class.isAssignableFrom(newCtx.getClass())) {
                    // Merges the existing value into this new ctx.
                    ((MergeableContext) existingCtxHolder.getContext()).merge((MergeableContext) newCtx);
                }
                newCtxHolder.markAsUpdatedExternally();
            }
        }
    }
}
