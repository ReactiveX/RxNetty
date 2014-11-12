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
import com.netflix.server.context.DirectionAwareSerializerAdapter;

/**
 * @author Nitesh Kant (nkant@netflix.com)
 */
public class TestContextDirectionalSerializer extends DirectionAwareSerializerAdapter<BidirectionalTestContext> {

    static int instanceCount; // Thread unsafe

    int serializationInboundCallReceived;
    int deserializationInboundCallReceived;
    int serializationOutboundCallReceived;
    int deserializationOutboundCallReceived;

    public TestContextDirectionalSerializer() {
        instanceCount++;
    }

    @Override
    public int getVersion() {
        return 1;
    }

    void reset() {
        serializationInboundCallReceived = 0;
        deserializationInboundCallReceived = 0;
        serializationOutboundCallReceived = 0;
        deserializationOutboundCallReceived = 0;
    }

    @Override
    public String serialize(BidirectionalTestContext toSerialize, Direction direction)
            throws ContextSerializationException {
        switch (direction) {
            case Inbound:
                serializationInboundCallReceived++;
                break;
            case Outbound:
                serializationOutboundCallReceived++;
                break;
        }
        return toSerialize.getName();
    }

    @Override
    public BidirectionalTestContext deserialize(String serialized, Direction direction, int version)
            throws ContextSerializationException {
        switch (direction) {
            case Inbound:
                deserializationInboundCallReceived++;
                break;
            case Outbound:
                deserializationOutboundCallReceived++;
                break;
        }
        return new BidirectionalTestContext(serialized);
    }
}
