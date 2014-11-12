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
import com.netflix.server.context.ContextSerializationException;
import com.netflix.server.context.ContextSerializer;
import com.netflix.server.context.DirectionAwareContextSerializer;

/**
 * A holder of an contexts containing augmenting information about the context.
 *
 * @author Nitesh Kant (nkant@netflix.com)
 */
class ContextHolder<T> {

    private T context;
    private final String contextName;
    private DirectionAwareContextSerializer<T> serializer;
    private String serialized;
    private String serializerClassName;
    private int serializedVersion;
    private String rawSerializedForm;
    private boolean updatedExternally;
    private final boolean receivedFromParent;
    private boolean biDirectional;

    private boolean isRaw;

    ContextHolder(T context, String contextName, ContextSerializer<T> serializer) {
        this.context = context;
        this.contextName = contextName;
        this.serializer = unifySerializerTypes(serializer);
        serializerClassName = serializer.getClass().getName();
        biDirectional = context.getClass().getAnnotation(BiDirectional.class) != null;
        receivedFromParent = false;
    }

    ContextHolder(String contextName, String rawSerializedForm, boolean biDirectional) {
        this.contextName = contextName;
        this.rawSerializedForm = rawSerializedForm;
        this.biDirectional = biDirectional;
        receivedFromParent = true;
        isRaw = true;
    }

    String getRawSerializedForm() {
        return rawSerializedForm;
    }

    boolean hasRawSerializedForm() {
        return null != rawSerializedForm;
    }

    /**
     * Asserts whether this context is a raw context i.e. it just has raw serialized context and nothing else.
     *
     * @return {@code true} if this context is a raw context.
     */
    boolean isRaw() {
        return isRaw;
    }

    String getContextName() {
        return contextName;
    }

    /**
     * Returns whether there is a serialized representation of the context already present.
     *
     * @return {@code true}  if there is a serialized representation of the context already present.
     * {@code false} otherwise.
     */
    boolean hasSerialized() {
        return null != serialized;
    }

    /**
     * Returns whether there is a context instance already present.
     *
     * @return {@code true}  if there is a context instance already present. {@code false} otherwise.
     */
    boolean hasContext() {
        return null != context;
    }

    /**
     * Returns the serialized string, if present ({@link ContextHolder#hasSerialized()} returns
     * {@code true} ).
     *
     * @return The serialized string. {@code null}  if none exists.
     */
    String getSerialized() {
        return serialized;
    }

    /**
     * Returns the version of the serialized string, if present ({@link ContextHolder#hasSerialized()} returns
     * {@code true} ).
     *
     * @return The version of the serialized string, if present. -1 if the serialized string is not present.
     */
    int getSerializedVersion() {
        return serializedVersion;
    }

    /**
     * Returns the context, if present ({@link ContextHolder#hasContext()} returns {@code true} )
     *
     * @return The context or {@code null}  if none present.
     */
    T getContext() {
        return context;
    }

    /**
     * Returns if this context was updated externally as defined by
     * {@link ContextsContainer#addContext(String, Object, ContextSerializer)}
     *
     * @return {@code true} if this context was updated externally.
     */
    boolean isUpdatedExternally() {
        return updatedExternally;
    }

    /**
     * Returns whether this context was received from the caller of this service or it was created inside this service.
     *
     * @return {@code true} if received from the caller of this service. {@code false} otherwise.
     */
    public boolean isReceivedFromParent() {
        return receivedFromParent;
    }

    /**
     * Asserts whether this context should flow back from this service in response. <br/>
     * This will be true if & only if,
     * <ul>
     <li>The context is bidirectional i.e. {@link #isBiDirectional()} returns {@code true} </li>
     <li>The context is updated externally i.e. {@link #isUpdatedExternally()} returns {@code true} OR the context
     is added for the first time in this service i.e. {@link #isReceivedFromParent()} returns {@code false} </li>
     </ul>
     *
     * @return {@code true} if the context should flow back, {@code false} otherwise.
     */
    public boolean shouldFlowBackInResponse() {
        return isBiDirectional() && (isUpdatedExternally() || !isReceivedFromParent());
    }

    /**
     * Sets that this context was updated externally i.e. by calling
     * {@link ContextsContainer#addContext(String, Object, ContextSerializer)} with a different value than what is
     * existing.
     */
    void markAsUpdatedExternally() {
        updatedExternally = true;
    }

    boolean isBiDirectional() {
        return biDirectional;
    }

    /**
     * Returns the serializer associated with this context. This method will instantiate a serializer instance if it is
     * not already present via reflection for class name provided. <p/>
     * In case, the serializer is {@link StringSerializer}, then {@link StringSerializer#INSTANCE} will be returned.
     *
     * @return The serializer for this context.
     *
     * @throws ContextSerializationException If the serializer is not found, not of required type or could not be
     * instantiated.
     */
    @SuppressWarnings("unchecked")
    DirectionAwareContextSerializer<T> getSerializer() throws ContextSerializationException {
        if(null != serializer) {
            return serializer;
        }

        if(null != serializerClassName) {

            if(serializerClassName.equals(StringSerializer.class.getName())) {
                return (DirectionAwareContextSerializer<T>) StringSerializer.INSTANCE;
            }
            try {
                @SuppressWarnings("rawtypes")
                Class<ContextSerializer> aClass = (Class<ContextSerializer>) Class.forName(serializerClassName);
                serializer = unifySerializerTypes(aClass.newInstance());
                return serializer;
            } catch (InstantiationException e) {
                throw new ContextSerializationException("Can not instantiate the serializer for class:" + serializerClassName, e);
            } catch (IllegalAccessException e) {
                throw new ContextSerializationException("Can not instantiate the serializer for class:" + serializerClassName, e);
            } catch (ClassNotFoundException e) {
                throw new ContextSerializationException("Can not instantiate the serializer for class:" + serializerClassName, e);
            } catch (ClassCastException e) {
                throw new ContextSerializationException("Serializer:" + serializerClassName + " is not of the required type.", e);
            }
        }

        throw new ContextSerializationException("No serializer found.");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    /* package private only for testing*/ContextSerializer<T> getRawSerializer() throws ContextSerializationException {
        DirectionAwareContextSerializer<T> serializer = getSerializer();
        if (DirectionalSerializerAdapter.class.isAssignableFrom(serializer.getClass())) {
            return ((DirectionalSerializerAdapter) serializer).actualSerializer;
        } else {
            return serializer;
        }
    }

    static <T> DirectionAwareContextSerializer<T> unifySerializerTypes(ContextSerializer<T> serializer) {
        return DirectionAwareContextSerializer.class.isAssignableFrom(serializer.getClass())
               ? (DirectionAwareContextSerializer<T>)serializer
               : new DirectionalSerializerAdapter<T>(serializer);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    String getSerializerClassName() {
        if (null == serializerClassName && null != serializer
                && serializer.getClass().equals(DirectionalSerializerAdapter.class)) {
            return ((DirectionalSerializerAdapter)serializer).actualSerializer.getClass().getName();
        }
        return serializerClassName;
    }

    void update(String serialized, int serializedVersion, String serializerClassName) {
        this.serialized = serialized;
        this.serializedVersion = serializedVersion;
        this.serializerClassName = serializerClassName;
        isRaw = false;
    }

    void update(T context) {
        this.context = context;
        biDirectional = context.getClass().getAnnotation(BiDirectional.class) != null;
    }

    private static class DirectionalSerializerAdapter<T> implements DirectionAwareContextSerializer<T> {

        private final ContextSerializer<T> actualSerializer;

        private DirectionalSerializerAdapter(ContextSerializer<T> actualSerializer) {
            this.actualSerializer = actualSerializer;
        }

        @Override
        public String serialize(T toSerialize, Direction direction)
                throws ContextSerializationException {
            return actualSerializer.serialize(toSerialize);
        }

        @Override
        public T deserialize(String serialized, Direction direction, int version)
                throws ContextSerializationException {
            return actualSerializer.deserialize(serialized, version);
        }

        @Override
        public int getVersion() {
            return actualSerializer.getVersion();
        }

        @Override
        public String serialize(T toSerialize) throws ContextSerializationException {
            return actualSerializer.serialize(toSerialize);
        }

        @Override
        public T deserialize(String serialized, int version) throws ContextSerializationException {
            return actualSerializer.deserialize(serialized, version);
        }
    }
}
