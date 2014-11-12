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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A helper class to aid contexts serialization/de-serialization. <p/>
 *
 * <h1>Serialization Format:</h1>
 *
 * Any context sent over the wire will be sent in the following format:
 <PRE>
    [serialization_format_version][separator][fully qualified class name of the serializer][separator][serializer version][separator][serialized context data]
 </PRE>
 *
 * The [serialization_format_version] above is {@link ContextSerializationHelper#SERIALIZATION_FORMAT_VER} <p/>
 * The [separator] above is {@link ContextSerializationHelper#CONTEXT_SERIALIZATION_PROPS_SEPARATOR} <p/>
 *
 * This class also adds a separate key by the name {@link #ALL_CONTEXTS_NAMES_KEY_NAME} to have a
 * {@link ContextSerializationHelper#CONTEXT_HEADER_NAMES_SEPARATOR} separated list of key names corresponding to the
 * contexts.
 *
 * @author Nitesh Kant (nkant@netflix.com)
 */
final class ContextSerializationHelper {

    /*
     * Key name prefix for any contexts present as part of the wire-protocol. eg: For HTTP it will be the
     * prefix for the key name.
     */
    static final String CONTEXT_PROTOCOL_KEY_NAME_PREFIX = "X-Netflix.request.sub.context.";

    /*
     * Key name for a list of all context keys added to the outbound service request. This is used to avoid
     * storing the request instance for lazy fetching of the contexts. We will get all the
     * context headers aggressively in construction and store the serialized form locally.
     */
    static final String ALL_CONTEXTS_NAMES_KEY_NAME = "X-Netflix.request.sub.context.names";

    @SuppressWarnings("unused") private static final int CONTEXT_SERIALIZATION_PROPS_SERIALIZATION_FORMAT_VER_INDEX = 0;
    private static final int CONTEXT_SERIALIZATION_PROPS_SERIALIZER_INDEX = 1;
    private static final int CONTEXT_SERIALIZATION_PROPS_VERSION_INDEX = 2;
    private static final int CONTEXT_SERIALIZATION_PROPS_DATA_INDEX = 3;
    private static final int CONTEXT_SERIALIZATION_PROPS_COUNT = 4;

    private static final String CONTEXT_SERIALIZATION_PROPS_SEPARATOR_REGEX = "\\|";

    static final int SERIALIZATION_FORMAT_VER = 1;
    static final String CONTEXT_SERIALIZATION_PROPS_SEPARATOR = "|";
    static final String CONTEXT_HEADER_NAMES_SEPARATOR = ",";
    public static final String EMPTY_SERIALIZED_DATA = "EMPTY";
    private static final Pattern CONTEXT_HEADER_NAMES_SEPARATOR_PATTERN = Pattern.compile(CONTEXT_HEADER_NAMES_SEPARATOR);
    private static final Pattern CONTEXT_SERIALIZATION_PROPS_SEPARATOR_PATTERN = Pattern.compile(CONTEXT_SERIALIZATION_PROPS_SEPARATOR_REGEX);

    private ContextSerializationHelper() {
    }

    /**
     * Serializes all contexts present in the passed {@code contextsContainer}. This also adds a index
     * key for the name of all the headers added corresponding to the contained contexts. See
     * {@link ContextSerializationHelper} for serialization format.
     *
     * @param contextHolders {@link ContextHolder} instances to be serialized.
     *
     * @return Immutable map of key name against the serialized contexts. Empty map if no contexts are
     * present.
     */
    static Map<String, String> getSerializedContexts(@SuppressWarnings("rawtypes") Collection<ContextHolder> contextHolders)
            throws ContextSerializationException {
        return getSerializedHeader(contextHolders, false, DirectionAwareContextSerializer.Direction.Outbound);
    }

    /**
     * Serializes all bi-directional contexts present in the passed {@code contextsContainer} that were updated
     * in the current application scope. This also adds a index
     * key for the name of all the headers added corresponding to the contained contexts. See
     * {@link ContextSerializationHelper} for serialization format.
     *
     * @param contextHolders {@link ContextHolder} instances to be serialized.
     *
     * @return Immutable map of key name against the serialized contexts. Empty map if no modified bi-directional
     * contexts are present.
     */
    static Map<String, String> getSerializedModifiedBidirectionalCtx(
            @SuppressWarnings("rawtypes") Collection<ContextHolder> contextHolders)
            throws ContextSerializationException {
        return getSerializedHeader(contextHolders, true, DirectionAwareContextSerializer.Direction.Inbound);
    }

    /**
     * De-serializes the context contained in the passed {@code contextHolder}. This method will use the context
     * value if de-serialized before. In such a case, the passed serializer will be ignored. <p/>
     * In case, there context is not de-serialized before, the string value will be de-serialized using the metadata
     * associated with {@link ContextHolder#getRawSerializedForm()}. See
     * {@link ContextSerializationHelper} for serialization format. <p/>
     * The serializer specified in the metadata will be initialized using reflection, if the passed serializer is
     * {@code null}
     *
     * @param contextHolder Context holder containing the context data.
     * @param serializer Serializer to be used if the serializer associated with the metadata is to be ignored.
     *
     * @param direction The direction of message flow for this deserialization.
     * @return The context object.
     *
     * @throws ContextSerializationException If the de-serialization failed.
     */
    static <T> T deserialize(ContextHolder<T> contextHolder, ContextSerializer<T> serializer, DirectionAwareContextSerializer.Direction direction)
            throws ContextSerializationException {
        if (contextHolder.hasContext()) {
            return contextHolder.getContext();
        }

        if (contextHolder.isRaw()) {
            unwrapSerializationMetadata(contextHolder); // updates the context.
        }

        DirectionAwareContextSerializer<T> directionAwareContextSerializer;
        if (null == serializer) {
            directionAwareContextSerializer = contextHolder.getSerializer();
        } else {
            directionAwareContextSerializer = ContextHolder.unifySerializerTypes(serializer);
        }

        T deserialized = directionAwareContextSerializer.deserialize(contextHolder.getSerialized(), direction,
                                                                     contextHolder.getSerializedVersion());
        if (null == deserialized) {
            throw new ContextSerializationException("Deserializer for context name: " + contextHolder.getContextName() + " returned null.");
        }
        contextHolder.update(deserialized); // caching the deserialized form

        return deserialized;
    }

    /**
     * Reads the contexts, if present, in the passed http request. The context key names are
     * retrieved by names specified in the key {@link #ALL_CONTEXTS_NAMES_KEY_NAME} separated by
     * {@link ContextSerializationHelper#CONTEXT_HEADER_NAMES_SEPARATOR} <p/>
     * <b>Neither the serializers will be instantiated nor the contexts will be de-serialized at this point.</b> All
     * de-serialization related tasks will be done lazily when the context is required.
     *
     * @param contextKeySupplier {@link ContextKeySupplier} to provide the context keys and values.
     *
     * @return Map of context names against {@link ContextHolder}s
     */
    @SuppressWarnings("rawtypes")
    static Map<String, ContextHolder> readContexts(ContextKeySupplier contextKeySupplier) {
        // Some unit tests pass the request as null.
        if (null == contextKeySupplier) {
            return Collections.emptyMap();
        }

        return readContexts(contextKeySupplier, false);
    }

    /**
     * Reads the bi-directional contexts, if present, from the passed headers map. The context key names are
     * retrieved by names specified in the key {@link #ALL_CONTEXTS_NAMES_KEY_NAME} separated by
     * {@link ContextSerializationHelper#CONTEXT_HEADER_NAMES_SEPARATOR} <p/>
     *
     * @param keySupplier {@link ContextKeySupplier} to provide the context keys and values.
     *
     * @return Map of context names against {@link ContextHolder}s. Empty map if none present.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static Map<String, ContextHolder> readBidirectionalContexts(ContextKeySupplier keySupplier) {
        if (null == keySupplier) {
            return Collections.emptyMap();
        }

        return readContexts(keySupplier, true);
    }

    @SuppressWarnings("rawtypes")
    private static Map<String, ContextHolder> readContexts(ContextKeySupplier keySupplier, boolean bidirectional) {
        String allHeaderNames = keySupplier.getContextValue(ALL_CONTEXTS_NAMES_KEY_NAME);
        if (null == allHeaderNames || allHeaderNames.isEmpty()) {
            return Collections.emptyMap();
        }

        String[] headerNames = CONTEXT_HEADER_NAMES_SEPARATOR_PATTERN.split(allHeaderNames);
        Map<String, ContextHolder> toReturn = new HashMap<String, ContextHolder>(headerNames.length);
        for (String headerName : headerNames) {
            String rawSerializedForm = keySupplier.getContextValue(headerName);
            if (null == rawSerializedForm) { // All names key is malformed for any reason
                continue;
            }

            String contextName = getContextNameFromHeaderName(headerName);

            if (null == contextName) { // Header name is malformed in all names key.
                continue;
            }
            toReturn.put(contextName, new ContextHolder(contextName, rawSerializedForm, bidirectional));
        }
        return toReturn;
    }

    private static String getContextNameFromHeaderName(String headerName) {
        if(headerName.length() <= CONTEXT_PROTOCOL_KEY_NAME_PREFIX.length()) {
            return null;
        }

        return headerName.substring(CONTEXT_PROTOCOL_KEY_NAME_PREFIX.length());
    }

    @SuppressWarnings("rawtypes")
    private static String constructHeaderName(ContextHolder contextHolder) {
        return CONTEXT_PROTOCOL_KEY_NAME_PREFIX + contextHolder.getContextName();
    }

    private static <T> void unwrapSerializationMetadata(ContextHolder<T> holder) throws ContextSerializationException {
        String rawSerializedForm = holder.getRawSerializedForm();
        String[] properties = CONTEXT_SERIALIZATION_PROPS_SEPARATOR_PATTERN.split(rawSerializedForm);
        if(properties.length < CONTEXT_SERIALIZATION_PROPS_COUNT) {
            throw new ContextSerializationException(
                    "Invalid serialized context key format. Number of properties in the serialized form: "
                    + properties.length + " is less than expected: " + CONTEXT_SERIALIZATION_PROPS_COUNT);
        }

        String data = properties[CONTEXT_SERIALIZATION_PROPS_DATA_INDEX];

        if (properties.length > CONTEXT_SERIALIZATION_PROPS_COUNT) {
            // Serialized data has the separator, so we should concat them again
            StringBuilder dataBuilder = new StringBuilder();
            for (int i = CONTEXT_SERIALIZATION_PROPS_DATA_INDEX; i < properties.length; i++) {
                String fragment = properties[i];
                if (dataBuilder.length() > 0) {
                    dataBuilder.append(CONTEXT_SERIALIZATION_PROPS_SEPARATOR);
                }
                dataBuilder.append(fragment);
            }
            data = dataBuilder.toString();
        } else {
            data = unmaskIfEmpty(data);
        }

        int version;
        try {
            version = Integer.parseInt(properties[CONTEXT_SERIALIZATION_PROPS_VERSION_INDEX]);
        } catch (NumberFormatException e) {
            throw new ContextSerializationException(
                    "Illegal serialization version: " + properties[CONTEXT_SERIALIZATION_PROPS_VERSION_INDEX], e);
        }
        holder.update(data, version, properties[CONTEXT_SERIALIZATION_PROPS_SERIALIZER_INDEX]);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String _serialize(ContextHolder contextHolder, DirectionAwareContextSerializer.Direction direction)
            throws ContextSerializationException {
        if(contextHolder.hasRawSerializedForm()) {
            return contextHolder.getRawSerializedForm();
        }

        DirectionAwareContextSerializer serializer = contextHolder.getSerializer();

        String data = contextHolder.hasSerialized()
                      ? contextHolder.getSerialized()
                      : serializer.serialize(contextHolder.getContext(), direction);

        if (null == data) {
            throw new ContextSerializationException("Serializer returned null for context name: " + contextHolder.getContextName());
        }

        if (data.isEmpty()) {
            data = maskEmptyString();
        }

        StringBuilder builder = new StringBuilder();
        builder.append(SERIALIZATION_FORMAT_VER);
        builder.append(CONTEXT_SERIALIZATION_PROPS_SEPARATOR);
        builder.append(contextHolder.getSerializerClassName());
        builder.append(CONTEXT_SERIALIZATION_PROPS_SEPARATOR);
        builder.append(serializer.getVersion());
        builder.append(CONTEXT_SERIALIZATION_PROPS_SEPARATOR);
        builder.append(data);
        return builder.toString();
    }

    private static Map<String, String> getSerializedHeader(@SuppressWarnings("rawtypes") Collection<ContextHolder> contextHolders,
                                                           boolean onlyBidirectionalUpdatedExternally,
                                                           DirectionAwareContextSerializer.Direction direction)
            throws ContextSerializationException {
        StringBuilder allContextHeaderNames = new StringBuilder();
        Map<String, String> toReturn = new HashMap<String, String>(contextHolders.size());
        for (@SuppressWarnings("rawtypes") ContextHolder contextHolder : contextHolders) {
            if (onlyBidirectionalUpdatedExternally && !contextHolder.shouldFlowBackInResponse()) {
                continue;
            }
            String headerName = constructHeaderName(contextHolder);
            if (allContextHeaderNames.length() > 0) {
                allContextHeaderNames.append(CONTEXT_HEADER_NAMES_SEPARATOR);
            }
            allContextHeaderNames.append(headerName);
            toReturn.put(headerName, _serialize(contextHolder, direction));
        }

        if(!toReturn.isEmpty()) {
            toReturn.put(ALL_CONTEXTS_NAMES_KEY_NAME, allContextHeaderNames.toString());
        }
        return Collections.unmodifiableMap(toReturn);
    }

    private static String maskEmptyString() {
        return EMPTY_SERIALIZED_DATA;
    }

    private static String unmaskIfEmpty(String serialized) {
        return EMPTY_SERIALIZED_DATA.equals(serialized) ? "" : serialized;
    }
}
