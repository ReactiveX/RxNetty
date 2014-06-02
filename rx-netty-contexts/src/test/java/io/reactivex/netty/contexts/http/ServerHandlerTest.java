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
public class ServerHandlerTest {

    public static final String CTX_1_NAME = "ctx1";
    public static final String CTX_1_VAL = "doom";
    private String requestId;

    @Before
    public void setUp() throws Exception {
        System.err.print(">>>> ServerHandlerTest.setUp()");
        RxContexts.DEFAULT_CORRELATOR.dumpThreadState(System.err);
    }

    @After
    public void tearDown() throws Exception {
        if (null != requestId) {
            RxContexts.DEFAULT_CORRELATOR.onServerProcessingEnd(requestId);
        }
        System.err.print(">>>> ServerHandlerTest.tearDown()");
        RxContexts.DEFAULT_CORRELATOR.dumpThreadState(System.err);
    }

    @Test
    public void testRequest() throws Exception {
        requestId = "ServerHandlerTest.testRequest";
        HandlerHolder holder = new HandlerHolder(true, requestId);
        readRequestAndAssert(holder);
    }

    @Test
    public void testResponse() throws Exception {
        requestId = "ServerHandlerTest.testResponse";
        HandlerHolder holder = new HandlerHolder(true, requestId);
        readRequestAndAssert(holder);


        ContextsContainer container = ContextAttributeStorageHelper.getContainer(holder.ctx, holder.requestId);
        String ctx2Name = "ctx2";
        BidirectionalTestContext ctx2 = new BidirectionalTestContext(ctx2Name);

        container.addContext(ctx2Name, ctx2, new BidirectionalTestContextSerializer());

        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);

        holder.handler.write(holder.ctx, response, holder.ctx.newPromise());

        ContextKeySupplier supplier = new HttpContextKeySupplier(response.headers());
        ContextsContainer containerToRead = new ContextsContainerImpl(supplier);
        Assert.assertEquals("Bi-directional context not written in response.", ctx2, containerToRead.getContext(
                ctx2Name));
    }

    private static void readRequestAndAssert(HandlerHolder holder) throws Exception {
        DefaultHttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "");
        holder.addSerializedContext(request, CTX_1_NAME, CTX_1_VAL);
        holder.handler.channelRead(holder.ctx, request);

        ContextsContainer container = ContextAttributeStorageHelper.getContainer(holder.ctx, holder.requestId);

        Assert.assertNotNull("Context container not set after request receive.", container);
        Assert.assertEquals("Context not available in the container.", CTX_1_VAL, container.getContext(CTX_1_NAME));
        Assert.assertEquals("Request Id header not added.", CTX_1_VAL, container.getContext(CTX_1_NAME));
    }
}
