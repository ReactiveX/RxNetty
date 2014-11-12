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

import io.reactivex.netty.contexts.ContextsContainer;

/**
 * A serializer for serialization/de-serialization any arbitrary context objects added to {@link ContextsContainer}. <p/>
 * This serializer will be used to serialize when the context objects are to be sent across the wire to another service
 * vai NIWS client. <p/>
 * This serializer will be used to de-serialize when the context objects are received on the wire from another service
 * as HTTP request headers.
 *
 * <h1>Versioning</h1>
 * A context serializer provides a serialization mechanism version which will be passed during de-serialization. The
 * intention of this versioning is <imp>not</imp> to select which versions can be de-serialized but to give a hint of
 * what is the format of the content to be de-serialized. Any serializer is always expected to be backward compatible
 * to handle services with multiple serializer versions in the request processing pipeline.
 *
 * @param <T> The object type that the serializer can serialize/de-serialize
 *
 * @author Nitesh Kant (nkant@netflix.com)
 */
public interface ContextSerializer<T> {

    /**
     * Returns the version of this serializer.
     *
     * @return The version of this serializer.
     */
    int getVersion();

    /**
     * Serializes the passed object {@code toSerialize}
     *
     * @param toSerialize The object to serialize. This object will never be {@code null}
     *
     * @return The serialized string. The serialization will be deemed failed if the returned object is {@code null}
     * and hence the callee will throw a {@link ContextSerializationException} upon receiving a {@code null}
     *
     * @throws ContextSerializationException If the serialization failed.
     */
    String serialize(T toSerialize) throws ContextSerializationException;

    /**
     * De-serializes the passed object {@code serialized}.
     *
     * @param serialized The serialized object to de-serialize. This string will never be {@code null}
     * @param version The version of the serializer as returned by {@link ContextSerializer#getVersion()} used to
     *                serialize the passed string.
     *
     * @return The de-serialized object. The de-serialization will be deemed failed if the returned object is {@code null}
     * and hence the callee will throw a {@link ContextSerializationException} upon receiving a {@code null}
     *
     * @throws ContextSerializationException If the de-serialization failed.
     */
    T deserialize(String serialized, int version) throws ContextSerializationException;
}
