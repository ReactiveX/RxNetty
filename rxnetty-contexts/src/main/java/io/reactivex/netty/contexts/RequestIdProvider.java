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
 * All contexts held by {@link ContextsContainer} are specific to a request and in order to uniquiely identify a request
 * we require an identifier. This interface defines the contract to provide a globally unique identifier for a request.
 *
 * @author Nitesh Kant
 */
public interface RequestIdProvider extends RequestIdGenerator {

    /**
     * Inspects the {@code keySupplier} or {@code context} to see if there is a request Id already defined for this
     * request. If it is, then it returns the same, else returns {@code null}
     * * <b>This method is NOT idempotent</b>
     *
     * @param keySupplier The key supplier where usually an externally defined request Id is present.
     * @param channelAttributeMap Channel attribute map, normally obtained as {@link ChannelHandlerContext#channel()}
     *
     * @return The request Identified or {@code null} if none exists.
     */
    String onServerRequest(ContextKeySupplier keySupplier, AttributeMap channelAttributeMap);

    /**
     * This is primarily for response-request correlation for protocols that support multiple requests on the same
     * connection, eg: using HTTP pipelining. <br/>
     * <b>This method is NOT idempotent</b>
     *
     *
     * @param responseKeySupplier {@link ContextKeySupplier} for the response.
     * @param channelAttributeMap Channel attribute map, normally obtained as {@link ChannelHandlerContext#channel()}
     *
     * @return The request id, if exists. {@code null} otherwise.
     */
    String beforeServerResponse(ContextKeySupplier responseKeySupplier, AttributeMap channelAttributeMap);

    /**
     * This does the correlation between an inbound request and all outbound requests that are made during the
     * processing of the same request. <br/>
     * <b>This method is NOT idempotent</b>
     *
     * @param clientAttributeMap Client's channel attribute map, normally obtained as
     * {@link ChannelHandlerContext#channel()}
     *
     * @return The request id, if exists. {@code null} otherwise.
     */
    String beforeClientRequest(AttributeMap clientAttributeMap);

    /**
     * Specifies the request id to be used while receiving a response for a client. <br/>
     * <b>This method is NOT idempotent</b>
     *
     * @param clientAttributeMap Client's channel attribute map, normally obtained as
     * {@link ChannelHandlerContext#channel()}
     *
     * @return The request id, if exists. {@code null} otherwise.
     */
    String onClientResponse(AttributeMap clientAttributeMap);

    /**
     * Name of the context key (to be obtained via {@link ContextKeySupplier}) that is to be used to transfer the
     * request identifier from one machine to another.
     *
     * @return The name of the context key for request id.
     */
    String getRequestIdContextKeyName();
}
