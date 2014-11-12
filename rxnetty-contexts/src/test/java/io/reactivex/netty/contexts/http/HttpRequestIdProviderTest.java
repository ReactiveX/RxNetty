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

import io.netty.util.AttributeMap;
import io.netty.util.DefaultAttributeMap;
import io.reactivex.netty.contexts.ContextKeySupplier;
import io.reactivex.netty.contexts.ContextsContainerImpl;
import io.reactivex.netty.contexts.MapBackedKeySupplier;
import io.reactivex.netty.contexts.RequestIdProvider;
import io.reactivex.netty.contexts.RxContexts;
import io.reactivex.netty.contexts.ThreadLocalRequestCorrelator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Nitesh Kant
 */
public class HttpRequestIdProviderTest {

    public static final String REQUEST_ID_HEADER_NAME = "request_id";
    public static final ThreadLocalRequestCorrelator CORRELATOR = new ThreadLocalRequestCorrelator();
    public static final String REQUEST_ID = "request_id";

    @After
    public void tearDown() throws Exception {
        if (CORRELATOR.getRequestIdForClientRequest() != null) {
            CORRELATOR.onServerProcessingEnd(REQUEST_ID);
            System.err.println("Sent server processing end callback to correlator.");
            RxContexts.DEFAULT_CORRELATOR.dumpThreadState(System.err);
        }
    }

    @Test
    public void testOnServerRequest() throws Exception {
        RequestIdProvider provider = new HttpRequestIdProvider(REQUEST_ID_HEADER_NAME, CORRELATOR);
        MapBackedKeySupplier keySupplier = new MapBackedKeySupplier(new HashMap<String, String>());
        AttributeMap attributeMap = new DefaultAttributeMap();
        String requestId = provider.onServerRequest(keySupplier, attributeMap);
        Assert.assertNull("Request Id should be null.", requestId);

        String expectedId = "hellothere";
        keySupplier.put(REQUEST_ID_HEADER_NAME, expectedId);

        requestId = provider.onServerRequest(keySupplier, attributeMap);

        Assert.assertEquals("Unexpected request id.", expectedId, requestId);
    }

    @Test
    public void testNewRequestId() throws Exception {
        RequestIdProvider provider = new HttpRequestIdProvider("request_id", CORRELATOR);
        ContextKeySupplier keySupplier = new MapBackedKeySupplier(new HashMap<String, String>());
        AttributeMap attributeMap = new DefaultAttributeMap();
        String requestId = provider.newRequestId(keySupplier, attributeMap);
        ConcurrentLinkedQueue<String> ids = attributeMap.attr(HttpRequestIdProvider.REQUEST_IDS_KEY).get();
        Assert.assertNotNull("Request Id not added to context.", ids);
        Assert.assertNotNull("Request Id not added to context.", ids.peek());
        Assert.assertSame("Unexpected request Id in the context.", requestId, ids.poll());
    }

    @Test
    public void testBeforeServerResponse() throws Exception {
        RequestIdProvider provider = new HttpRequestIdProvider(REQUEST_ID_HEADER_NAME, CORRELATOR);
        AttributeMap attributeMap = new DefaultAttributeMap();
        MapBackedKeySupplier keySupplier = new MapBackedKeySupplier(new HashMap<String, String>());
        String expectedId1 = "hellothere1";
        String expectedId2 = "hellothere2";
        String expectedId3 = "hellothere3";

        // Simulate HTTP pipelining
        onServerRequest(provider, attributeMap, keySupplier, expectedId1);
        onServerRequest(provider, attributeMap, keySupplier, expectedId2);
        onServerRequest(provider, attributeMap, keySupplier, expectedId3);

        Assert.assertSame("Unexpected 1st request Id", expectedId1,
                          provider.beforeServerResponse(keySupplier, attributeMap));
        Assert.assertSame("Unexpected 2nd request Id", expectedId2,
                          provider.beforeServerResponse(keySupplier, attributeMap));
        Assert.assertSame("Unexpected 3rd request Id", expectedId3,
                          provider.beforeServerResponse(keySupplier, attributeMap));
    }

    @Test
    public void testBeforeClientRequest() throws Exception {
        RequestIdProvider provider = new HttpRequestIdProvider(REQUEST_ID, CORRELATOR);
        AttributeMap attributeMap = new DefaultAttributeMap();
        ContextKeySupplier keySupplier = new MapBackedKeySupplier(new HashMap<String, String>());
        String expectedId = "daah";
        CORRELATOR.onNewServerRequest(expectedId, new ContextsContainerImpl(keySupplier));
        String requestId = provider.beforeClientRequest(attributeMap);

        Assert.assertSame("Unexpected request Id", expectedId, requestId);
    }

    @Test
    public void testOnClientResponse() throws Exception {
        RequestIdProvider provider = new HttpRequestIdProvider("request_id", CORRELATOR);
        AttributeMap attributeMap = new DefaultAttributeMap();
        ContextKeySupplier keySupplier = new MapBackedKeySupplier(new HashMap<String, String>());
        String expectedId = "daah";
        CORRELATOR.onNewServerRequest(expectedId, new ContextsContainerImpl(keySupplier));
        String requestId = provider.beforeClientRequest(attributeMap);
        Assert.assertSame("Unexpected request Id", expectedId, requestId);
        Assert.assertSame("Unexpected request Id on client response.", expectedId, provider.onClientResponse(attributeMap));
    }

    private static void onServerRequest(RequestIdProvider provider, AttributeMap attributeMap,
                                        MapBackedKeySupplier keySupplier, String expectedId) {
        keySupplier.put(REQUEST_ID_HEADER_NAME, expectedId);
        provider.onServerRequest(keySupplier, attributeMap);
    }
}
