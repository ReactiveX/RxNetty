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
package io.reactivex.netty.protocol.http;

import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.LastHttpContent;

import java.util.List;

/**
 * A mutable stateful entity containing trailing headers.
 *
 * <b>This class is not thread safe</b>
 */
public class TrailingHeaders {

    private final LastHttpContent lastHttpContent;

    public TrailingHeaders() {
        lastHttpContent = new DefaultLastHttpContent();
    }

    public TrailingHeaders(LastHttpContent lastHttpContent) {
        this.lastHttpContent = lastHttpContent;
    }

    /**
     * Adds an HTTP trailing header with the passed {@code name} and {@code value} to this request.
     *
     * @param name Name of the header.
     * @param value Value for the header.
     *
     * @return {@code this}.
     */
    public TrailingHeaders addHeader(CharSequence name, Object value) {
        lastHttpContent.trailingHeaders().add(name, value);
        return this;
    }

    /**
     * Adds an HTTP trailing header with the passed {@code name} and {@code values} to this request.
     *
     * @param name Name of the header.
     * @param values Values for the header.
     *
     * @return {@code this}.
     */
    public TrailingHeaders addHeader(CharSequence name, Iterable<Object> values) {
        lastHttpContent.trailingHeaders().add(name, values);
        return this;
    }

    /**
     * Overwrites the current value, if any, of the passed trailing header to the passed value for this request.
     *
     * @param name Name of the header.
     * @param value Value of the header.
     *
     * @return {@code this}.
     */
    public TrailingHeaders setHeader(CharSequence name, Object value) {
        lastHttpContent.trailingHeaders().set(name, value);
        return this;
    }

    /**
     * Overwrites the current value, if any, of the passed trailing header to the passed values for this request.
     *
     * @param name Name of the header.
     * @param values Values of the header.
     *
     * @return {@code this}.
     */
    public TrailingHeaders setHeader(CharSequence name, Iterable<Object> values) {
        lastHttpContent.trailingHeaders().set(name, values);
        return this;
    }

    /**
     * Returns the value of a header with the specified name.  If there are more than one values for the specified name,
     * the first value is returned.
     *
     * @param name The name of the header to search
     *
     * @return The first header value or {@code null} if there is no such header
     */
    public String getHeader(CharSequence name) {
        return lastHttpContent.trailingHeaders().get(name);
    }

    /**
     * Returns the values of headers with the specified name
     *
     * @param name The name of the headers to search
     *
     * @return A {@link List} of header values which will be empty if no values are found
     */
    public List<String> getAllHeaderValues(CharSequence name) {
        return lastHttpContent.trailingHeaders().getAll(name);
    }

}
