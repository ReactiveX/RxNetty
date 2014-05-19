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

import io.netty.handler.codec.http.HttpHeaders;
import io.reactivex.netty.contexts.ContextKeySupplier;

/**
 * An implementation of {@link ContextKeySupplier} that reads context keys from HTTP headers.
 *
 * @author Nitesh Kant
 */
public class HttpContextKeySupplier implements ContextKeySupplier {

    private final HttpHeaders headers;

    public HttpContextKeySupplier(HttpHeaders headers) {
        this.headers = headers;
    }

    @Override
    public String getContextValue(String key) {
        return headers.get(key);
    }
}
