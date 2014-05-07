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
import com.netflix.server.context.DirectionAwareContextSerializer;

/**
 * A dumb serializer that does nothing. Its existence is to follow the pattern that a context always has a serializer.
 *
 * @author Nitesh Kant (nkant@netflix.com)
 */
class StringSerializer implements DirectionAwareContextSerializer<String> {

    static final DirectionAwareContextSerializer<String> INSTANCE = new StringSerializer();

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public String serialize(String toSerialize) throws ContextSerializationException {
        return toSerialize;
    }

    @Override
    public String deserialize(String serialized, int version) throws ContextSerializationException {
        return serialized;
    }

    @Override
    public String serialize(String toSerialize, Direction direction)
            throws ContextSerializationException {
        return toSerialize;
    }

    @Override
    public String deserialize(String serialized, Direction direction, int version)
            throws ContextSerializationException {
        return serialized;
    }
}
