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

package io.reactivex.netty.protocol.http.clientNew;

import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import rx.Observable;
import rx.Subscriber;

import java.text.ParseException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * HTTP response for {@link HttpClient}
 *
 * <h2>Thread safety</h2>
 *
 * This object is not thread-safe and must not be used by multiple threads.
 *
 * <h2>Mutability</h2>
 *
 * Headers and trailing headers can be mutated for this response.
 */
public abstract class HttpClientResponse<T> {

    /**
     * Returns the HTTP version for this response.
     *
     * @return The HTTP version for this response.
     */
    public abstract HttpVersion getHttpVersion();

    /**
     * Returns the HTTP status for this response.
     *
     * @return The HTTP status for this response.
     */
    public abstract HttpResponseStatus getStatus();

    /**
     * Returns an immutable map of cookie names and cookies contained in this response.
     *
     * @return An immutable map of cookie names and cookies contained in this response.
     */
    public abstract Map<String, Set<Cookie>> getCookies();

    /**
     * Checks if there is a header with the passed name in this response.
     *
     * @param name Name of the header.
     *
     * @return {@code true} if there is a header with the passed name in this response.
     */
    public abstract boolean containsHeader(CharSequence name);

    /**
     * Checks if there is a header with the passed name and value in this response.
     *
     * @param name Name of the header.
     * @param value Value of the header.
     * @param ignoreCaseValue {@code true} then the value comparision is done ignoring case.
     *
     * @return {@code true} if there is a header with the passed name and value in this response.
     */
    public abstract boolean containsHeader(CharSequence name, CharSequence value, boolean ignoreCaseValue);

    /**
     * Returns an iterator over the header entries. Multiple values for the same header appear as separate entries in
     * the returned iterator.
     *
     * @return An iterator over the header entries
     */
    public abstract Iterator<Entry<String, String>> headerIterator();

    /**
     * Returns the value of a header with the specified name.  If there are more than one values for the specified name,
     * the first value is returned.
     *
     * @param name The name of the header to search
     * @return The first header value or {@code null} if there is no such header
     */
    public abstract String getHeader(CharSequence name);

    /**
     * Returns the value of a header with the specified name.  If there are more than one values for the specified name,
     * the first value is returned.
     *
     * @param name The name of the header to search
     * @param defaultValue Default if the header does not exist.
     *
     * @return The first header value or {@code defaultValue} if there is no such header
     */
    public abstract String getHeader(CharSequence name, String defaultValue);

    /**
     * Returns the values of headers with the specified name
     *
     * @param name The name of the headers to search
     *
     * @return A {@link List} of header values which will be empty if no values are found
     */
    public abstract List<String> getAllHeaderValues(CharSequence name);

    /**
     * Returns the length of the content.
     *
     * @return the content length
     *
     * @throws NumberFormatException if the message does not have the {@code "Content-Length"} header or its value is
     * not a number.
     */
    public abstract long getContentLength();

    /**
     * Returns the length of the content.
     *
     * @param defaultValue Default value if the message does not have a {@code "Content-Length"} header or its value is
     * not a number
     *
     * @return the content length or {@code defaultValue} if this message does not have the {@code "Content-Length"}
     * header or its value is not a number
     */
    public abstract long getContentLength(long defaultValue);

    /**
     * Returns the date header value with the specified header name.  If there are more than one header value for the
     * specified header name, the first value is returned.
     * The value is parsed as per the
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3.1">HTTP specifications</a> using the format:
     * <PRE>"E, dd MMM yyyy HH:mm:ss z"</PRE>
     *
     * @param name The name of the header to search
     *
     * @return the header value
     *
     * @throws ParseException if there is no such header or the header value is not a formatted date
     */
    public abstract Date getDateHeader(CharSequence name) throws ParseException;

    /**
     * Returns the date header value with the specified header name.  If there are more than one header value for the
     * specified header name, the first value is returned.
     * The value is parsed as per the
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3.1">HTTP specifications</a> using the format:
     * <PRE>"E, dd MMM yyyy HH:mm:ss z"</PRE>
     *
     * @param name The name of the header to search
     * @param defaultValue Default value if there is no header with this name.
     *
     * @return the header value or {@code defaultValue} if there is no header with this name.
     */
    public abstract Date getDateHeader(CharSequence name, Date defaultValue);

    /**
     * Returns the value of the {@code "Host"} header.
     */
    public abstract String getHostHeader();

    /**
     * Returns the value of the {@code "Host"} header.
     *
     * @param defaultValue Default if the header does not exist.
     *
     * @return The value of the {@code "Host"} header or {@code defaultValue} if there is no such header.
     */
    public abstract String getHost(String defaultValue);

    /**
     * Returns the integer header value with the specified header name.  If there are more than one header value for
     * the specified header name, the first value is returned.
     *
     * @param name The name of the header to search
     *
     * @return the header value
     *
     * @throws NumberFormatException if there is no such header or the header value is not a number
     */
    public abstract int getIntHeader(CharSequence name);

    /**
     * Returns the integer header value with the specified header name.  If there are more than one header value for
     * the specified header name, the first value is returned.
     *
     * @param name The name of the header to search
     * @param defaultValue Default if the header does not exist.
     *
     * @return the header value or the {@code defaultValue} if there is no such header or the header value is not a
     * number
     */
    public abstract int getIntHeader(CharSequence name, int defaultValue);

    /**
     * Returns {@code true} if and only if this response has the content-length header set.
     */
    public abstract boolean isContentLengthSet();

    /**
     * Returns {@code true} if and only if the connection can remain open and thus 'kept alive'.  This methods respects
     * the value of the {@code "Connection"} header first and then the return value of
     * {@link HttpVersion#isKeepAliveDefault()}.
     */
    public abstract boolean isKeepAlive();

    /**
     * Checks to see if the transfer encoding of this response is chunked
     *
     * @return True if transfer encoding is chunked, otherwise false
     */
    public abstract boolean isTransferEncodingChunked();

    /**
     * Returns a new {@link Set} that contains the names of all headers in this response.  Note that modifying the
     * returned {@link Set} will not affect the state of this response.
     */
    public abstract Set<String> getHeaderNames();

    /**
     * Adds an HTTP header with the passed {@code name} and {@code value} to this response.
     *
     * @param name Name of the header.
     * @param value Value for the header.
     *
     * @return {@code this}
     */
    public abstract HttpClientResponse<T> addHeader(CharSequence name, Object value);

    /**
     * Adds the passed {@code cookie} to this response.
     *
     * @param cookie Cookie to add.
     *
     * @return {@code this}
     */
    public abstract HttpClientResponse<T> addCookie(Cookie cookie);

    /**
     * Adds the passed header as a date value to this response. The date is formatted using netty's
     * {@link HttpHeaders#addDateHeader(HttpMessage, CharSequence, Date)} which formats the date as per the
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3.1">HTTP specifications</a> into the format:
     * <PRE>"E, dd MMM yyyy HH:mm:ss z"</PRE>
     *
     * @param name Name of the header.
     * @param value Value of the header.
     *
     * @return {@code this}
     */
    public abstract HttpClientResponse<T> addDateHeader(CharSequence name, Date value);

    /**
     * Adds multiple date values for the passed header name to this response. The date values are formatted using netty's
     * {@link HttpHeaders#addDateHeader(HttpMessage, CharSequence, Date)} which formats the date as per the
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3.1">HTTP specifications</a> into the format:
     *
     * <PRE>"E, dd MMM yyyy HH:mm:ss z"</PRE>
     *
     * @param name Name of the header.
     * @param values Values for the header.
     *
     * @return {@code this}
     */
    public abstract HttpClientResponse<T> addDateHeader(CharSequence name, Iterable<Date> values);

    /**
     * Adds an HTTP header with the passed {@code name} and {@code values} to this response.
     *
     * @param name Name of the header.
     * @param values Values for the header.
     *
     * @return {@code this}
     */
    public abstract HttpClientResponse<T> addHeader(CharSequence name, Iterable<Object> values);

    /**
     * Overwrites the current value, if any, of the passed header to the passed date value for this response. The date is
     * formatted using netty's {@link HttpHeaders#addDateHeader(HttpMessage, CharSequence, Date)} which formats the date
     * as per the <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3.1">HTTP specifications</a> into
     * the format:
     * <p/>
     * <PRE>"E, dd MMM yyyy HH:mm:ss z"</PRE>
     *
     * @param name Name of the header.
     * @param value Value of the header.
     *
     * @return {@code this}
     */
    public abstract HttpClientResponse<T> setDateHeader(CharSequence name, Date value);

    /**
     * Overwrites the current value, if any, of the passed header to the passed value for this response.
     *
     * @param name Name of the header.
     * @param value Value of the header.
     *
     * @return {@code this}
     */
    public abstract HttpClientResponse<T> setHeader(CharSequence name, Object value);

    /**
     * Overwrites the current value, if any, of the passed header to the passed date values for this response. The date
     * is formatted using netty's {@link HttpHeaders#addDateHeader(HttpMessage, CharSequence, Date)} which formats the
     * date as per the <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3.1">HTTP specifications</a>
     * into the format:
     * <p/>
     * <PRE>"E, dd MMM yyyy HH:mm:ss z"</PRE>
     *
     * @param name Name of the header.
     * @param values Values of the header.
     *
     * @return {@code this}
     */
    public abstract HttpClientResponse<T> setDateHeader(CharSequence name, Iterable<Date> values);

    /**
     * Overwrites the current value, if any, of the passed header to the passed values for this response.
     *
     * @param name Name of the header.
     * @param values Values of the header.
     *
     * @return {@code this}
     */
    public abstract HttpClientResponse<T> setHeader(CharSequence name, Iterable<Object> values);

    /**
     * Removes the passed header from this response.
     *
     * @param name Name of the header.
     *
     * @return {@code this}
     */
    public abstract HttpClientResponse<T> removeHeader(CharSequence name);

    /**
     * Returns the content as a stream. There can only be one {@link Subscriber} to the returned {@link Observable}, any
     * subsequent subscriptions will get an error.
     *
     * @return Stream of content.
     */
    public abstract Observable<T> getContent();

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getHttpVersion().text())
               .append(' ')
               .append(getStatus().code())
               .append(' ')
               .append(getStatus().reasonPhrase())
               .append('\n');

        Iterator<Entry<String, String>> headers = headerIterator();
        while (headers.hasNext()) {
            Entry<String, String> next = headers.next();
            builder.append(next.getKey())
                   .append(": ")
                   .append(next.getValue());
        }
        builder.append('\n');

        return builder.toString();
    }
}
