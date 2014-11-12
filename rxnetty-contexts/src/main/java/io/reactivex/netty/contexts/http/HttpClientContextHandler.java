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

import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.reactivex.netty.contexts.AbstractClientContextHandler;
import io.reactivex.netty.contexts.ContextKeySupplier;
import io.reactivex.netty.contexts.RequestCorrelator;
import io.reactivex.netty.contexts.RequestIdProvider;

public class HttpClientContextHandler extends AbstractClientContextHandler<HttpResponse, HttpRequest> {

    private String currentlyProcessingRequestId; // Updated only on read to account for pipelining.

    public HttpClientContextHandler(RequestIdProvider requestIdProvider, RequestCorrelator correlator) {
        super(correlator, requestIdProvider);
        if (null == requestIdProvider) {
            throw new IllegalArgumentException("Request Id Provider can not be null.");
        }
        if (null == correlator) {
            throw new IllegalArgumentException("Client context provider can not be null.");
        }
    }

    @Override
    protected boolean isAcceptableToRead(Object msg) {
        return msg instanceof HttpResponse;
    }

    @Override
    protected boolean isAcceptableToWrite(Object msg) {
        return msg instanceof HttpRequest;
    }

    @Override
    protected void addKey(HttpRequest msg, String key, String value) {
        msg.headers().add(key, value);
    }

    @Override
    protected ContextKeySupplier newKeySupplierForWrite(HttpRequest msg) {
        return new HttpContextKeySupplier(msg.headers());
    }

    @Override
    protected ContextKeySupplier newKeySupplierForRead(HttpResponse msg) {
        return new HttpContextKeySupplier(msg.headers());
    }

    @Override
    protected void newRequestIdRead(String requestId) {
        currentlyProcessingRequestId = requestId;
    }

    @Override
    protected String getCurrentlyProcessingRequestId() {
        return currentlyProcessingRequestId;
    }

    @Override
    protected boolean isLastResponseFragmentToRead(Object response) {
        return response instanceof LastHttpContent;
    }
}
