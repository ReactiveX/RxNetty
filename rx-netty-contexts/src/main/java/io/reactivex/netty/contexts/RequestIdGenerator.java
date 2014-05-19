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

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeMap;

/**
 * @author Nitesh Kant
 */
public interface RequestIdGenerator {

    /**
     * Generates a <em>globally unique</em> request identifier.
     *
     * @param keySupplier {@link ContextKeySupplier} for the request.
     * @param channelAttributeMap Channel attribute map, normally obtained as {@link ChannelHandlerContext#channel()}
     *
     * @return The newly generated request id.
     */
    String newRequestId(ContextKeySupplier keySupplier, AttributeMap channelAttributeMap);
}
