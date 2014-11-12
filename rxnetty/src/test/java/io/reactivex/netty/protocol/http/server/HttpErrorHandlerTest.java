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
package io.reactivex.netty.protocol.http.server;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.server.RxServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import rx.Observable;

/**
 * @author Nitesh Kant
 */
public class HttpErrorHandlerTest {

    public static final String X_TEST_RESP_GEN_CALLED_HEADER_NAME = "X-TEST-RESP-GEN-CALLED";

    private RxServer<HttpServerRequest<ByteBuf>, HttpServerResponse<ByteBuf>> server;

    @After
    public void tearDown() throws Exception {
        if (null != server) {
            server.shutdown();
        }
    }

    @Test
    public void testErrorGenerator() throws Exception {
        server = RxNetty.createHttpServer(0, new RequestHandler<ByteBuf, ByteBuf>() {
            @Override
            public Observable<Void> handle(
                    HttpServerRequest<ByteBuf> request,
                    HttpServerResponse<ByteBuf> response) {
                return Observable
                        .error(new IllegalStateException(
                                "I always throw an error."));
            }
        }).withErrorResponseGenerator(new ErrorResponseGenerator<ByteBuf>() {
            @Override
            public void updateResponse(
                    HttpServerResponse<ByteBuf> response,
                    Throwable error) {
                response.setStatus(
                        HttpResponseStatus.INTERNAL_SERVER_ERROR);
                response.getHeaders().add(
                        X_TEST_RESP_GEN_CALLED_HEADER_NAME,
                        "true");
            }
        }).start();

        int port = server.getServerPort();

        HttpClientRequest<ByteBuf> request =
                HttpClientRequest.createGet("/");

        HttpClientResponse<ByteBuf> response =
                RxNetty.createHttpClient("localhost", port).submit(request).toBlocking().last();

        Assert.assertEquals("Unexpected response status", HttpResponseStatus.INTERNAL_SERVER_ERROR.code(),
                            response.getStatus().code());
        Assert.assertTrue("Error response generator not called.", response.getHeaders().contains(
                X_TEST_RESP_GEN_CALLED_HEADER_NAME));
    }

    @Test
    public void testErrorGeneratorThrowException() throws Exception {
        server = RxNetty.createHttpServer(0, new RequestHandler<ByteBuf, ByteBuf>() {
            @Override
            public Observable<Void> handle(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response) {
                throw new IllegalStateException("I always throw an error.");
            }
        }).withErrorResponseGenerator(new ErrorResponseGenerator<ByteBuf>() {
            @Override
            public void updateResponse(HttpServerResponse<ByteBuf> response, Throwable error) {
                response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
                response.getHeaders().add(X_TEST_RESP_GEN_CALLED_HEADER_NAME, "true");
            }
        }).start();

        int port = server.getServerPort();

        HttpClientRequest<ByteBuf> request =
                HttpClientRequest.createGet("/");

        HttpClientResponse<ByteBuf> response =
                RxNetty.createHttpClient("localhost", port).submit(request).toBlocking().last();

        Assert.assertEquals("Unexpected response status", HttpResponseStatus.INTERNAL_SERVER_ERROR.code(),
                            response.getStatus().code());
        Assert.assertTrue("Error response generator not called.", response.getHeaders().contains(
                X_TEST_RESP_GEN_CALLED_HEADER_NAME));
    }
}
