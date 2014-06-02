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

import com.netflix.server.context.ContextSerializationException;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.reactivex.netty.contexts.BidirectionalTestContext;
import io.reactivex.netty.contexts.BidirectionalTestContextSerializer;
import io.reactivex.netty.contexts.ContextAttributeStorageHelper;
import io.reactivex.netty.contexts.ContextKeySupplier;
import io.reactivex.netty.contexts.ContextsContainer;
import io.reactivex.netty.contexts.ContextsContainerImpl;
import io.reactivex.netty.contexts.RxContexts;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Nitesh Kant
 */
public class ClientHandlerTest {

    public static final String CTX_1_NAME = "ctx1";
    public static final BidirectionalTestContext CTX_1_VAL = new BidirectionalTestContext(CTX_1_NAME);

    @Before
    public void setUp() throws Exception {
        System.err.print(">>>> ClientHandlerTest.setUp()");
        RxContexts.DEFAULT_CORRELATOR.dumpThreadState(System.err);
    }

    @After
    public void tearDown() throws Exception {
        System.err.print(">>>> ClientHandlerTest.tearDown()");
        RxContexts.DEFAULT_CORRELATOR.dumpThreadState(System.err);
    }

    @Test
    public void testRequest() throws Exception {
        HandlerHolder holder = new HandlerHolder(false, "ClientHandlerTest.testRequest");
        sendRequestAndAssert(holder);
    }

    @Test
    public void testResponse() throws Exception {
        HandlerHolder holder = new HandlerHolder(false, "ClientHandlerTest.testResponse");
        sendRequestAndAssert(holder);


        HttpResponse response = createResponseWithCtxHeaders(holder);

        holder.handler.channelRead(holder.ctx, response);

        ContextsContainer container = ContextAttributeStorageHelper.getContainer(holder.ctx, holder.requestId);
        Assert.assertEquals("Context not available in client response", CTX_1_VAL, container.getContext(CTX_1_NAME));
    }

    private static DefaultHttpResponse createResponseWithCtxHeaders(HandlerHolder holder)
            throws ContextSerializationException {
        DefaultHttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        holder.addSerializedContext(response, CTX_1_NAME, CTX_1_VAL, new BidirectionalTestContextSerializer());
        return response;
    }

    private static void sendRequestAndAssert(HandlerHolder holder) throws Exception {
        holder.correlator.onNewServerRequest(holder.requestId, new ContextsContainerImpl(holder.keySupplier));

        try {
            DefaultHttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "");
            holder.addSerializedContext(request, CTX_1_NAME, CTX_1_VAL, new BidirectionalTestContextSerializer());
            holder.handler.write(holder.ctx, request, holder.ctx.newPromise());

            Assert.assertNotNull("Context container not set after request sent.",
                                 ContextAttributeStorageHelper.getContainer(holder.ctx, holder.requestId));

            ContextKeySupplier supplier = new HttpContextKeySupplier(request.headers());
            ContextsContainer container = new ContextsContainerImpl(supplier);

            Assert.assertEquals("Context not available in the container.", CTX_1_VAL, container.getContext(CTX_1_NAME));
            Assert.assertEquals("Request Id header not added.", holder.getRequestId(),
                                request.headers().get(holder.getProvider().getRequestIdContextKeyName()));
        } finally {
            holder.correlator.onServerProcessingEnd(holder.requestId);
            System.err.println("Sent server processing end callback to correlator.");
            RxContexts.DEFAULT_CORRELATOR.dumpThreadState(System.err);
        }
    }
}
