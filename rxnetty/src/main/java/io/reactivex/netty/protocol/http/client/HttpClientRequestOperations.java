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
 */
package io.reactivex.netty.protocol.http.client;

import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A package private interface to share the mutable operation declarations on {@link HttpClientRequest} between
 * {@link HttpClientRequest} and {@link HttpClientRequestUpdater}
 *
 * @author Nitesh Kant
 */
interface HttpClientRequestOperations<Input, Owner> {

    Owner setContent(Input content);

    Owner setStringContent(String content);

    Owner setBytesContent(byte[] content);

    Owner readTimeOut(int timeOut, TimeUnit timeUnit);

    Owner followRedirects(int maxRedirects);

    Owner followRedirects(boolean follow);

    Owner addCookie(Cookie cookie);

    Owner addDateHeader(CharSequence name, Date value);

    Owner addHeader(CharSequence name, Object value);

    Owner addDateHeader(CharSequence name, Iterable<Date> values);

    Owner addHeader(CharSequence name, Iterable<Object> values);

    Owner setDateHeader(CharSequence name, Date value);

    Owner setHeader(CharSequence name, Object value);

    Owner setDateHeader(CharSequence name, Iterable<Date> values);

    Owner setHeader(CharSequence name, Iterable<Object> values);

    Owner setKeepAlive(boolean keepAlive);

    Owner setTransferEncodingChunked();

    boolean containsHeader(CharSequence name);

    boolean containsHeaderWithValue(CharSequence name, CharSequence value, boolean caseInsensitiveValueMatch);

    String getHeader(CharSequence name);

    List<String> getAllHeaders(CharSequence name);

    HttpVersion getHttpVersion();

    HttpMethod getMethod();

    String getUri();

    String getAbsoluteUri();
}
