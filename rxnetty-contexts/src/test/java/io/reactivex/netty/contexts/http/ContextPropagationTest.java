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
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.contexts.ContextKeySupplier;
import io.reactivex.netty.contexts.ContextsContainer;
import io.reactivex.netty.contexts.ContextsContainerImpl;
import io.reactivex.netty.contexts.MapBackedKeySupplier;
import io.reactivex.netty.contexts.RxContexts;
import io.reactivex.netty.contexts.TestContext;
import io.reactivex.netty.contexts.TestContextSerializer;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.HttpServerBuilder;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;
import rx.functions.Action0;
import rx.functions.Func1;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static io.reactivex.netty.contexts.ThreadLocalRequestCorrelator.getCurrentContextContainer;
import static io.reactivex.netty.contexts.ThreadLocalRequestCorrelator.getCurrentRequestId;

/**
 * @author Nitesh Kant
 */
public class ContextPropagationTest {

    public static final String CTX_3_FOUND_HEADER = "CTX_3_FOUND";

    private HttpServer<ByteBuf, ByteBuf> mockServer;
    private static final String REQUEST_ID_HEADER_NAME = "request_id";
    private static final String CTX_1_NAME = "ctx1";
    private static final String CTX_1_VAL = "ctx1_val";
    private static final String CTX_2_NAME = "ctx2";
    private static final TestContext CTX_2_VAL = new TestContext(CTX_2_NAME);

    @Before
    public void setUp() throws Exception {
        mockServer = RxNetty.newHttpServerBuilder(0, new RequestHandler<ByteBuf, ByteBuf>() {
            @Override
            public Observable<Void> handle(final HttpServerRequest<ByteBuf> request,
                                           HttpServerResponse<ByteBuf> response) {
                final String requestId = request.getHeaders().get(REQUEST_ID_HEADER_NAME);
                if (null == requestId) {
                    System.err.println("Request Id not found.");
                    return Observable.error(new AssertionError("Request Id not found in mock server."));
                }
                response.getHeaders().add(REQUEST_ID_HEADER_NAME, requestId);
                ContextKeySupplier supplier = new ContextKeySupplier() {
                    @Override
                    public String getContextValue(String key) {
                        return request.getHeaders().get(key);
                    }
                };
                ContextsContainer container = new ContextsContainerImpl(supplier);
                try {
                    String ctx1 = container.getContext(CTX_1_NAME);
                    TestContext ctx2 = container.getContext(CTX_2_NAME);
                    if (null != ctx1 && null != ctx2 && ctx1.equals(CTX_1_VAL) && ctx2.equals(CTX_2_VAL)) {
                        return response.writeStringAndFlush("Welcome!");
                    } else {
                        response.setStatus(HttpResponseStatus.BAD_REQUEST);
                        return response.writeStringAndFlush("Contexts not found or have wrong values.");
                    }
                } catch (ContextSerializationException e) {
                    return Observable.error(e);
                }
            }
        }).enableWireLogging(LogLevel.ERROR).build();
        mockServer.start();
    }

    @After
    public void tearDown() throws Exception {
        mockServer.shutdown();
        mockServer.waitTillShutdown(1, TimeUnit.MINUTES);
    }

    @Test
    public void testEndToEnd() throws Exception {
        HttpServer<ByteBuf, ByteBuf> server =
                newTestServerBuilder(new Func1<HttpClient<ByteBuf, ByteBuf>, Observable<HttpClientResponse<ByteBuf>>>() {
                            @Override
                            public Observable<HttpClientResponse<ByteBuf>> call(HttpClient<ByteBuf, ByteBuf> client) {
                                return client.submit(HttpClientRequest.createGet("/"));
                            }
                        }).enableWireLogging(LogLevel.DEBUG).build().start();

        HttpClient<ByteBuf, ByteBuf> testClient = RxNetty.<ByteBuf, ByteBuf>newHttpClientBuilder("localhost", server.getServerPort())
                                                         .enableWireLogging(LogLevel.ERROR).build();

        String reqId = "testE2E";
        sendTestRequest(testClient, reqId);
    }

    @Test(expected = MockBackendRequestFailedException.class)
    public void testWithThreadSwitchNegative() throws Exception {
        HttpServer<ByteBuf, ByteBuf> server =
                newTestServerBuilder(new Func1<HttpClient<ByteBuf, ByteBuf>, Observable<HttpClientResponse<ByteBuf>>>() {
                    @Override
                    public Observable<HttpClientResponse<ByteBuf>> call(final HttpClient<ByteBuf, ByteBuf> client) {
                        return Observable.timer(1, TimeUnit.MILLISECONDS)
                                         .flatMap(new Func1<Long, Observable<HttpClientResponse<ByteBuf>>>() {
                                             @Override
                                             public Observable<HttpClientResponse<ByteBuf>> call(Long aLong) {
                                                 return client.submit(HttpClientRequest.createGet("/"));
                                             }
                                         });
                    }
                }).build().start();

        HttpClient<ByteBuf, ByteBuf> testClient = RxNetty.createHttpClient("localhost", server.getServerPort());

        String reqId = "testWithThreadSwitchNegative";
        sendTestRequest(testClient, reqId);
    }

    @Test
    public void testWithThreadSwitch() throws Exception {
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        HttpServer<ByteBuf, ByteBuf> server =
                newTestServerBuilder(new Func1<HttpClient<ByteBuf, ByteBuf>, Observable<HttpClientResponse<ByteBuf>>>() {
                    @Override
                    public Observable<HttpClientResponse<ByteBuf>> call(final HttpClient<ByteBuf, ByteBuf> client) {
                        Callable<HttpClientResponse<ByteBuf>> ctxAware =
                                RxContexts.DEFAULT_CORRELATOR.makeClosure(new Callable<HttpClientResponse<ByteBuf>>() {
                                    @Override
                                    public HttpClientResponse<ByteBuf> call() throws Exception {
                                        return client.submit(HttpClientRequest.createGet("/")).toBlocking()
                                                     .last();
                                    }
                                });
                        Future<HttpClientResponse<ByteBuf>> submit = executor.submit(ctxAware);
                        return Observable.from(submit);
                    }
                }).build().start();

        HttpClient<ByteBuf, ByteBuf> testClient = RxNetty.createHttpClient("localhost", server.getServerPort());

        String reqId = "testWithThreadSwitch";
        sendTestRequest(testClient, reqId);
    }

    @Test
    public void testWithPooledConnections() throws Exception {
        HttpClient<ByteBuf, ByteBuf> testClient =
                RxContexts.<ByteBuf, ByteBuf>newHttpClientBuilder("localhost", mockServer.getServerPort(),
                                                                  REQUEST_ID_HEADER_NAME,
                                                                  RxContexts.DEFAULT_CORRELATOR)
                          .withMaxConnections(1).enableWireLogging(LogLevel.ERROR)
                          .withIdleConnectionsTimeoutMillis(100000).build();
        ContextsContainer container = new ContextsContainerImpl(new MapBackedKeySupplier());
        container.addContext(CTX_1_NAME, CTX_1_VAL);
        container.addContext(CTX_2_NAME, CTX_2_VAL, new TestContextSerializer());

        String reqId = "testWithPooledConnections";
        RxContexts.DEFAULT_CORRELATOR.onNewServerRequest(reqId, container);

        invokeMockServer(testClient, reqId, false);

        invokeMockServer(testClient, reqId, true);
    }

    @Test(expected = MockBackendRequestFailedException.class)
    public void testNoStateLeakOnThreadReuse() throws Exception {
        HttpClient<ByteBuf, ByteBuf> testClient =
                RxContexts.<ByteBuf, ByteBuf>newHttpClientBuilder("localhost", mockServer.getServerPort(),
                                                                  REQUEST_ID_HEADER_NAME,
                                                                  RxContexts.DEFAULT_CORRELATOR)
                          .withMaxConnections(1).enableWireLogging(LogLevel.ERROR)
                          .withIdleConnectionsTimeoutMillis(100000).build();

        ContextsContainer container = new ContextsContainerImpl(new MapBackedKeySupplier());
        container.addContext(CTX_1_NAME, CTX_1_VAL);
        container.addContext(CTX_2_NAME, CTX_2_VAL, new TestContextSerializer());

        String reqId = "testNoStateLeakOnThreadReuse";
        RxContexts.DEFAULT_CORRELATOR.onNewServerRequest(reqId, container);

        try {
            invokeMockServer(testClient, reqId, true);
        } catch (MockBackendRequestFailedException e) {
            throw new AssertionError("First request to mock backend failed. Error: " + e.getMessage());
        }

        invokeMockServer(testClient, reqId, false);
    }

    private HttpServerBuilder<ByteBuf, ByteBuf> newTestServerBuilder(final Func1<HttpClient<ByteBuf, ByteBuf>,
            Observable<HttpClientResponse<ByteBuf>>> clientInvoker) {
        return RxContexts.newHttpServerBuilder(0, new RequestHandler<ByteBuf, ByteBuf>() {
            @Override
            public Observable<Void> handle(HttpServerRequest<ByteBuf> request,
                                           final HttpServerResponse<ByteBuf> serverResponse) {
                String reqId = getCurrentRequestId();
                if (null == reqId) {
                    return Observable.error(new AssertionError("Request Id not found at server."));
                }
                ContextsContainer container = getCurrentContextContainer();
                if (null == container) {
                    return Observable.error(new AssertionError("Context container not found by server."));
                }
                container.addContext(CTX_1_NAME, CTX_1_VAL);
                container.addContext(CTX_2_NAME, CTX_2_VAL, new TestContextSerializer());

                HttpClient<ByteBuf, ByteBuf> client =
                        RxContexts.<ByteBuf, ByteBuf>newHttpClientBuilder("localhost", mockServer.getServerPort(),
                                                                          REQUEST_ID_HEADER_NAME,
                                                                          RxContexts.DEFAULT_CORRELATOR)
                                  .withMaxConnections(1).enableWireLogging(LogLevel.DEBUG)
                                  .build();

                return clientInvoker.call(client).flatMap(
                        new Func1<HttpClientResponse<ByteBuf>, Observable<Void>>() {
                            @Override
                            public Observable<Void> call(HttpClientResponse<ByteBuf> response) {
                                serverResponse.setStatus(response.getStatus());
                                return serverResponse.close(true);
                            }
                        });
            }
        }, REQUEST_ID_HEADER_NAME, RxContexts.DEFAULT_CORRELATOR);
    }

    private static void invokeMockServer(HttpClient<ByteBuf, ByteBuf> testClient, final String requestId,
                                         boolean finishServerProcessing)
            throws MockBackendRequestFailedException, InterruptedException {
        try {
            sendTestRequest(testClient, requestId);
        } finally {
            if (finishServerProcessing) {
                RxContexts.DEFAULT_CORRELATOR.onServerProcessingEnd(requestId);
                System.err.println("Sent server processing end callback to correlator.");
                RxContexts.DEFAULT_CORRELATOR.dumpThreadState(System.err);
            }
        }

        if (finishServerProcessing) {
            Assert.assertNull("Current request id not cleared from thread.", getCurrentRequestId());
            Assert.assertNull("Current context not cleared from thread.", getCurrentContextContainer());
        }
    }
    private static void sendTestRequest(HttpClient<ByteBuf, ByteBuf> testClient, final String requestId)
            throws MockBackendRequestFailedException, InterruptedException {
        System.err.println("Sending test request to mock server, with request id: " + requestId);
        RxContexts.DEFAULT_CORRELATOR.dumpThreadState(System.err);
        final CountDownLatch finishLatch = new CountDownLatch(1);
        final List<HttpClientResponse<ByteBuf>> responseHolder = new ArrayList<HttpClientResponse<ByteBuf>>();
        testClient.submit(HttpClientRequest.createGet("").withHeader(REQUEST_ID_HEADER_NAME, requestId))
                  .flatMap(new Func1<HttpClientResponse<ByteBuf>, Observable<ByteBuf>>() {
                      @Override
                      public Observable<ByteBuf> call(HttpClientResponse<ByteBuf> response) {
                          responseHolder.add(response);
                          return response.getContent();
                      }
                  })
                  .ignoreElements()
                  .finallyDo(new Action0() {
                      @Override
                      public void call() {
                          finishLatch.countDown();
                      }
                  })
                  .subscribe();

        finishLatch.await(1, TimeUnit.MINUTES);

        if (responseHolder.isEmpty()) {
            throw new AssertionError("Response not received.");
        }

        System.err.println("Received response from mock server, with request id: " + requestId
                           + ", status: " + responseHolder.get(0).getStatus());

        HttpClientResponse<ByteBuf> response = responseHolder.get(0);

        if (response.getStatus().code() != HttpResponseStatus.OK.code()) {
            throw new MockBackendRequestFailedException("Test request failed. Status: " + response.getStatus().code());
        }

        String requestIdGot = response.getHeaders().get(REQUEST_ID_HEADER_NAME);

        if (!requestId.equals(requestId)) {
            throw new MockBackendRequestFailedException("Request Id not sent from mock server. Expected: "
                                                        + requestId + ", got: " + requestIdGot);
        }
    }

    private static class MockBackendRequestFailedException extends Exception {

        private static final long serialVersionUID = 5033661188956567940L;

        private MockBackendRequestFailedException(String message) {
            super(message);
        }
    }
}
