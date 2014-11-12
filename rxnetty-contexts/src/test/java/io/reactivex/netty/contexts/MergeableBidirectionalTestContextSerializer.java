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

import java.util.regex.Pattern;

/**
 * @author Nitesh Kant (nkant@netflix.com)
 */
public class MergeableBidirectionalTestContextSerializer implements
        ContextSerializer<MergeableBidirectionalTestContext> {

    private static final Pattern COLON_PATTERN = Pattern.compile(":");
    static int instanceCount; // Thread unsafe

    int serializationCallReceived;
    int deserializationCallReceived;

    public MergeableBidirectionalTestContextSerializer() {
        instanceCount++;
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public String serialize(MergeableBidirectionalTestContext toSerialize) throws ContextSerializationException {
        serializationCallReceived++;
        StringBuilder toReturn = new StringBuilder();
        toReturn.append(toSerialize.getName());
        for (String name : toSerialize.getAllNames()) {
            toReturn.append(':');
            toReturn.append(name);
        }
        return toReturn.toString();
    }

    @Override
    public MergeableBidirectionalTestContext deserialize(String serialized, int version) throws
            ContextSerializationException {
        deserializationCallReceived++;
        String[] parts = COLON_PATTERN.split(serialized);
        MergeableBidirectionalTestContext ctx = new MergeableBidirectionalTestContext(parts[0]);
        if (parts.length > 1) {
            for (int i = 1, partsLength = parts.length; i < partsLength; i++) {
                String part = parts[i];
                ctx.addName(part);
            }
        }
        return ctx;
    }
}
