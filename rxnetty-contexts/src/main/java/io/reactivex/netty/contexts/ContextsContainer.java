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

import java.util.Map;

/**
 * A contract to hold multiple request level contexts. <br/>
 *
 * <b>All implementations must be thread safe.</b>
 *
 * @author Nitesh Kant
 */
public interface ContextsContainer {

    /**
     * Adds a custom context (arbitrary object) to this container. <p/>
     * This context will be passed for all outbound service calls made by RxClient. <br/>
     * Serialization/de-serialization mechanism for this context will be delegated to {@code serializer}.
     * Serialization will be done when an outbound service call is made, to send this context on the wire.<p/>
     * De-serialization will be done in the outbound service for the above call, <em>if and only if</em>, a client
     * invokes {@link ContextsContainer#getContext(String)} for the same context name.
     * <b>This method overwrites any existing value with the passed context name.</b>
     *
     * <h2>Context updates:</h2>
     * If the context is mutable, then every mutation should add the context back here otherwise, the mutation will be
     * lost when the context gets passed to a remote application.
     *
     * <h2>Bi-directional contexts:</h2>
     * The following scenario counts as an update to a bi-directional context:
     * <ul>
     <li>The added context is bi-directional.</li>
     <li>There already is a value associated with this context name.</li>
     <li>The associated value is of the same class as the new value.</li>
     <li>The associated value is not equal to the new value as indicated by {@link Object#equals(Object)}</li>
     </ul>
     * In case this is actually an update, any subsequent calls to {@link #getModifiedBidirectionalContexts()} will also
     * include this context.
     *
     * @param contextName Name of the context.
     * @param context Context instance.
     * @param serializer Serializer for the context.
     * @param <T> Type of the context.
     *
     * @throws IllegalArgumentException If any of the arguments are null.
     */
    <T> void addContext(String contextName, T context, ContextSerializer<T> serializer);

    /**
     * Map of bi-directional context names and values (serialized form) that were modified
     * within the current application. See {@link BiDirectional} for details. <p/>
     *
     * @return Immutable map of name and value of the contexts. Empty map if none present.
     *
     * @throws ContextSerializationException If the serialization fails.
     */
    Map<String, String> getModifiedBidirectionalContexts() throws ContextSerializationException;

    /**
     * Reads the bi-directional contexts from the HTTP response and updates this {@link ContextsContainer} with those
     * contexts.
     *
     * @param contextKeySupplier The {@link ContextKeySupplier} for the response.
     *
     * @throws ContextSerializationException If the de-serialization of the bidrectional context fails.
     */
    void consumeBidirectionalContextsFromResponse(ContextKeySupplier contextKeySupplier)
            throws ContextSerializationException;

    /**
     * Adds a custom {@link String} context to this request scope. <br/>
     * This has exactly the same effect as calling {@link #addContext(String, Object, ContextSerializer)} with the
     * serializer as {@link StringSerializer}
     *
     * @param contextName Name of the context.
     * @param context Context instance.
     *
     * @throws IllegalArgumentException If any of the arguments are null.
     */
    void addContext(String contextName, String context);

    /**
     * Removes a context with name {@code contextName} added previously in this request scope via
     * {@link #addContext(String, String)} or {@link #addContext(String, Object, ContextSerializer)} <p/>
     * The context could have been set in a different service and transferred here on the wire.
     *
     * @param contextName Name of the context to remove.
     *
     * @return {@code true} if the context existed and was removed. {@code false}  if the context did not exist.
     */
    boolean removeContext(String contextName);

    /**
     * Retrieves the value of the context added previously in this request scope via
     * {@link #addContext(String, String)} or {@link #addContext(String, Object, ContextSerializer)} <p/>
     * The context could have been set in a different service and transferred here on the wire. In such a case, the
     * serializer passed during addition of the context will be used for de-serialization. <p/>
     * All values of the context after de-serialization are cached, unless overwritten by a corresponding
     * {@link #addContext(String, Object, ContextSerializer)}. Also, the serializer instance will be
     * cached once created.
     *
     * @param contextName Name of the context.
     */
    <T> T getContext(String contextName) throws ContextSerializationException;

    /**
     * Retrieves the value of the context added previously in this request scope via
     * {@link #addContext(String, String)} or {@link #addContext(String, Object, ContextSerializer)} <p/>
     * The context could have been set in a different service and transferred here on the wire. In such a case, the
     * serializer passed will be used for de-serialization and the serializer passed during context addition will be
     * ignored.<p/>
     *
     * <em>Word of caution: If the value is cached as a result of a prior invocation of this method or
     * {@link #getContext(String)}, the passed serializer will <b>not</b> be used.</em> <p/>
     *
     * This method should only be used if your serializer is stateful or requires custom initializations. Otherwise, the
     * serializer associated with this context must be used i.e. by calling {@link #getContext(String)} <p/>
     *
     * All values of the context after de-serialization are cached, unless overwritten by a corresponding
     * {@link #addContext(String, Object, ContextSerializer)}. Also, the serializer instance will be
     * cached once created.
     *
     * @param contextName Name of the context.
     * @param serializer The serializer instance to use if the context is required to be de-serialized.
     */
    @SuppressWarnings("unchecked")
    <T> T getContext(String contextName, ContextSerializer<T> serializer)
            throws ContextSerializationException;

    /**
     * Map of context names and values (serialized form) associated with this request.<p/>
     *
     * @return Immutable map of name and value of the contexts. Empty map if none present.
     *
     * @throws ContextSerializationException If the serialization fails.
     */
    Map<String, String> getSerializedContexts() throws ContextSerializationException;
}
