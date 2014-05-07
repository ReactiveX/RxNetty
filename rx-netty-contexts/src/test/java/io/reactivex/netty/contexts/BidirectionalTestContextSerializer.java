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

/**
 * @author Nitesh Kant (nkant@netflix.com)
 */
public class BidirectionalTestContextSerializer implements ContextSerializer<BidirectionalTestContext> {

    static int instanceCount; // Thread unsafe

    int serializationCallReceived;
    int deserializationCallReceived;

    public BidirectionalTestContextSerializer() {
        instanceCount++;
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public String serialize(BidirectionalTestContext toSerialize) throws ContextSerializationException {
        serializationCallReceived++;
        return toSerialize.getName();
    }

    @Override
    public BidirectionalTestContext deserialize(String serialized, int version) throws ContextSerializationException {
        deserializationCallReceived++;
        return new BidirectionalTestContext(serialized);
    }
}
