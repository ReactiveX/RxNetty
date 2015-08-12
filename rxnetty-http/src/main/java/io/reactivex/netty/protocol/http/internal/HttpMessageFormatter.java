/*
 * Copyright 2015 Netflix, Inc.
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
 *
 */
package io.reactivex.netty.protocol.http.internal;

import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.util.Iterator;
import java.util.Map.Entry;

public final class HttpMessageFormatter {

    private HttpMessageFormatter() {
    }

    public static String formatRequest(HttpVersion version,HttpMethod method, String uri,
                                        Iterator<Entry<String, String>> headers) {
        StringBuilder builder = new StringBuilder();
        builder.append(method)
               .append(' ')
               .append(uri)
               .append(' ')
               .append(version.text())
               .append('\n');

        printHeaders(headers, builder);

        return builder.toString();
    }

    public static String formatResponse(HttpVersion version, HttpResponseStatus status,
                                        Iterator<Entry<String, String>> headers) {
        StringBuilder builder = new StringBuilder();
        builder.append(version.text())
               .append(' ')
               .append(status.code())
               .append(' ')
               .append(status.reasonPhrase())
               .append('\n');

        printHeaders(headers, builder);

        return builder.toString();
    }

    private static void printHeaders(Iterator<Entry<String, String>> headers, StringBuilder builder) {
        while (headers.hasNext()) {
            Entry<String, String> next = headers.next();
            builder.append(next.getKey())
                   .append(": ")
                   .append(next.getValue())
                   .append('\n');
        }

        builder.append('\n');
    }
}
