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

package com.netflix.server.context;

/**
 * A replacement for {@link ContextSerializer} for {@link BiDirectional} contexts that requires information about the
 * direction of flow (i.e. request or response). For legacy reasons, this class extends {@link ContextSerializer}, the
 * easier way to implement a direction aware serializer will be to extend {@link DirectionAwareSerializerAdapter} which
 * provides a default implementation of the methods in {@link ContextSerializer} <br/>
 *
 * <h2>Directions</h2>
 *
 * Directions carry different meanings when associated with serialization type. Infact its usually the opposite for
 * serialization and de-serialization. Following is the definition:
 *
 * <h4>{@link Direction#Inbound}</h4>
 * <u>Serialization: </u> Serializing for adding to the response.
 * <u>De-serialization: </u> De-serializing for reading the request.
 *
 * <h4>{@link Direction#Outbound}</h4>
 * <u>Serialization: </u> Serializing for adding to the request.
 * <u>De-serialization: </u> De-serializing for reading the response.
 *
 * @author Nitesh Kant
 */
public interface DirectionAwareContextSerializer<T> extends ContextSerializer<T> {

    /**
     * Directions defined by this interface. See {@link DirectionAwareContextSerializer} for definitions.
     */
    enum Direction {
        // Direction when an app receives the context.
        Inbound,
        Outbound
    }

    /**
     * Serializes the passed object {@code toSerialize}
     *
     * @param toSerialize The object to serialize. This object will never be {@code null}
     * @param direction Direction of message flow. See {@link DirectionAwareContextSerializer} for definitions.
     *
     * @return The serialized string. The serialization will be deemed failed if the returned object is {@code null}
     * and hence the callee will throw a {@link ContextSerializationException} upon receiving a {@code null}
     *
     * @throws ContextSerializationException If the serialization failed.
     *
     * @see DirectionAwareContextSerializer
     */
    String serialize(T toSerialize, Direction direction) throws ContextSerializationException;

    /**
     * De-serializes the passed object {@code serialized}.
     *
     * @param serialized The serialized object to de-serialize. This string will never be {@code null}
     * @param version The version of the serializer as returned by {@link ContextSerializer#getVersion()} used to
     *                serialize the passed string.
     * @param direction Direction of message flow. See {@link DirectionAwareContextSerializer} for definitions.
     *
     * @return The de-serialized object. The de-serialization will be deemed failed if the returned object is {@code null}
     * and hence the callee will throw a {@link ContextSerializationException} upon receiving a {@code null}
     *
     * @throws ContextSerializationException If the de-serialization failed.
     *
     * @see DirectionAwareContextSerializer
     */
    T deserialize(String serialized, Direction direction, int version) throws ContextSerializationException;

}
