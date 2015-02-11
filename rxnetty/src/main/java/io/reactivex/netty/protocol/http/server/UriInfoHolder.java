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

        // java.net.URI doesn't support a relaxed mode and fails for many URIs that get used
        // in practice
        int indexOfStartOfQP = uri.indexOf('?');
        if (-1 != indexOfStartOfQP && uri.length() >= indexOfStartOfQP) {
            queryString = uri.substring(indexOfStartOfQP + 1);
        } else {
            queryString = "";
        }

        decoder = new QueryStringDecoder(getPath(uri));
    }

    // If it is a relative URI then just pass it to the decoder. Otherwise we need to remove
    // everything before the path. This method assumes the first '/' after the scheme is the
    // start of the path.
    private String getPath(String uri) {
        int offset = 0;
        if (uri.startsWith("http://")) {
            offset = "http://".length();
        } else if (uri.startsWith("https://")) {
            offset = "https://".length();
        }

        if (offset == 0) {
            return uri;
        } else {
            int firstSlash = uri.indexOf("/", offset);
            return (firstSlash != -1) ? uri.substring(firstSlash) : uri;
        }
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
        return decoder.parameters();
    }
}
