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

/**
 * Passing contexts from inbound request processing to outbound request sending can be very application specific. Some
 * applications may just use a single thread between the inbound and outbound processing and some may spawn off multiple
 * threads to process the request and hence needs to transfer the {@link ContextsContainer} instance from the server
 * processing the request to a client sending an outbound request as part of the original request processing. <br/>
 * Since, this is application specific, this module delegates this particular decision to an implementation of this
 * interface. The actual implementation can choose a medium specific to their application.
 *
 * @author Nitesh Kant
 */
public interface RequestCorrelator extends ContextCapturer {

    /**
     * This does the correlation between an inbound request and all outbound requests that are made during the
     * processing of the same request. <br/>
     *
     * @return The request id, if exists. {@code null} otherwise.
     */
    String getRequestIdForClientRequest();

    /**
     * This method does the correlation between the inbound {@link ContextsContainer} to a fresh client request.
     *
     * @param requestId Request Id for this request. Can be {@code null}
     *
     * @return {@link ContextsContainer} for this request, if any. {@code null} if none exists.
     */
    ContextsContainer getContextForClientRequest(String requestId);

    /**
     * A callback for a fresh request on the underlying channel.
     *
     * @param requestId Request Id for this request.
     * @param contextsContainer Container for this request.
     */
    void onNewServerRequest(String requestId, ContextsContainer contextsContainer);

    /**
     * A callback before a client request is made. This will almost always be identical to
     * {@link #onNewServerRequest(String, ContextsContainer)}. This method is provided to be explicit on when these
     * callbacks are made. <br/>
     * This will generally be made at a different point (usually when the connection is established by the client
     * for making a request) than {@link #getRequestIdForClientRequest()} and
     * {@link #getContextForClientRequest(String)} which is the reason why this context setting is
     * required.
     *
     * @param requestId Request Id for this request.
     * @param contextsContainer Container for this request.
     */
    void beforeNewClientRequest(String requestId, ContextsContainer contextsContainer);

    /**
     * A callback when a client is done with the processing of the passed {@code requestId}. <br/>
     * All implementations must be aware of the fact that there may be many clients invoked in a processing of a
     * single request,
     *
     * @param requestId Request id for which the processing is over.
     */
    void onClientProcessingEnd(String requestId);

    /**
     * A callback when the entire request processing is over for the passed {@code requestId}
     *
     * @param requestId Request id for which the processing is over.
     */
    void onServerProcessingEnd(String requestId);
}
