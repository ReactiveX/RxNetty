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
package io.reactivex.netty.contexts.http;

import io.netty.util.AttributeKey;
import io.netty.util.AttributeMap;
import io.reactivex.netty.contexts.ContextKeySupplier;
import io.reactivex.netty.contexts.RequestCorrelator;
import io.reactivex.netty.contexts.RequestIdGenerator;
import io.reactivex.netty.contexts.RequestIdProvider;

import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * An implementation of {@link RequestIdProvider} for HTTP protocol. <br/>
 * This implementation honors HTTP pipelining and hence maintains a queue of request Ids generated on the same
 * connection. The request Ids from the queue are removed when a response is set in the order they were received. <br/>
 * Since, HTTP pipelining defines that the responses should be sent from the server in the same order they were received,
 * this implementation uses a Queue to store and find request Ids for the responses sent from here.
 *
 * @author Nitesh Kant
 */
public class HttpRequestIdProvider implements RequestIdProvider {

    public static final AttributeKey<ConcurrentLinkedQueue<String>> REQUEST_IDS_KEY =
            AttributeKey.valueOf("rxnetty_http_request_ids_queue");

    private final RequestIdGenerator requestIdGenerator;
    private final RequestCorrelator requestCorrelator;
    private final String requestIdHeaderName;

    public HttpRequestIdProvider(RequestIdGenerator requestIdGenerator, RequestCorrelator requestCorrelator,
                                 String requestIdHeaderName) {
        this.requestIdGenerator = requestIdGenerator;
        this.requestCorrelator = requestCorrelator;
        this.requestIdHeaderName = requestIdHeaderName;
    }

    public HttpRequestIdProvider(String requestIdHeaderName, RequestCorrelator requestCorrelator) {
        this(new RequestIdGenerator() {
            @Override
            public String newRequestId(ContextKeySupplier keySupplier, AttributeMap channelAttributeMap) {
                return UUID.randomUUID().toString();
            }
        }, requestCorrelator, requestIdHeaderName);
    }

    @Override
    public String newRequestId(ContextKeySupplier keySupplier, AttributeMap channelAttributeMap) {
        String requestId = requestIdGenerator.newRequestId(keySupplier, channelAttributeMap);
        addRequestId(channelAttributeMap, requestId);
        return requestId;
    }

    @Override
    public String onServerRequest(ContextKeySupplier keySupplier, AttributeMap channelAttributeMap) {
        String requestId = keySupplier.getContextValue(requestIdHeaderName);
        if (null != requestId) {
            addRequestId(channelAttributeMap, requestId);
        }
        return requestId;
    }

    @Override
    public String beforeServerResponse(ContextKeySupplier responseKeySupplier, AttributeMap channelAttributeMap) {
        return getRequestIdFromQueue(channelAttributeMap);
    }

    @Override
    public String beforeClientRequest(AttributeMap clientAttributeMap) {
        String requestId = requestCorrelator.getRequestIdForClientRequest();
        if (null != requestId) {
            addRequestId(clientAttributeMap, requestId);
        }
        return requestId;
    }

    @Override
    public String onClientResponse(AttributeMap clientAttributeMap) {
        return getRequestIdFromQueue(clientAttributeMap);
    }

    @Override
    public String getRequestIdContextKeyName() {
        return requestIdHeaderName;
    }

    private static void addRequestId(AttributeMap channelAttributeMap, String requestId) {
        ConcurrentLinkedQueue<String> requestIdsQueue = channelAttributeMap.attr(REQUEST_IDS_KEY).get();
        if (null == requestIdsQueue) {
            requestIdsQueue = new ConcurrentLinkedQueue<String>();
            ConcurrentLinkedQueue<String> existingQueue = channelAttributeMap.attr(REQUEST_IDS_KEY).setIfAbsent(requestIdsQueue);
            if (null != existingQueue) {
                requestIdsQueue = existingQueue;
            }
        }
        if (null != requestIdsQueue) {
            requestIdsQueue.offer(requestId); // We can even use add() as this is a lock-free queue. Just using offer() to not confuse folks.
        }
    }

    private static String getRequestIdFromQueue(AttributeMap channelAttributeMap) {
        ConcurrentLinkedQueue<String> requestIdsQueue = channelAttributeMap.attr(REQUEST_IDS_KEY).get();
        if (null != requestIdsQueue) {
            return requestIdsQueue.poll(); // Responses should be sent in the same order as the requests were received (HTTP pipelining)
        }
        return null;
    }
}
