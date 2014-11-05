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

import io.netty.handler.codec.http.QueryStringDecoder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Nitesh Kant
 */
public class UriInfoHolder {

    private final String uri;
    private final String queryString;
    private final QueryStringDecoder decoder;

    public UriInfoHolder(String uri) {
        this.uri = uri;
        int indexOfStartOfQP = uri.indexOf('?');
        if (-1 != indexOfStartOfQP && uri.length() >= indexOfStartOfQP) {
            queryString = uri.substring(indexOfStartOfQP + 1);
        } else {
            queryString = "";
        }
        decoder = new QueryStringDecoder(uri);
    }

    public String getRawUriString() {
        return uri;
    }

    public synchronized String getPath() {
        return decoder.path();
    }

    public String getQueryString() {
        return queryString;
    }

    public synchronized Map<String, List<String>> getQueryParameters() {
        // decoder.parameters() sometimes returns an immutable map
        return new HashMap<String, List<String>>(decoder.parameters());
    }
}
