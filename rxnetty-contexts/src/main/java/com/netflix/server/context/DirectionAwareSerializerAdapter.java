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
 * An implementation of {@link DirectionAwareContextSerializer} to provide consistent behavior for callers who are
 * unaware of this interface and treat all serializers as {@link ContextSerializer}
 *
 * @author Nitesh Kant
 */
public abstract class DirectionAwareSerializerAdapter<T> implements DirectionAwareContextSerializer<T> {

    @Override
    public String serialize(T toSerialize) throws ContextSerializationException {
        return serialize(toSerialize, Direction.Outbound);
    }

    @Override
    public T deserialize(String serialized, int version) throws ContextSerializationException {
        return deserialize(serialized, Direction.Inbound, version);
    }
}
