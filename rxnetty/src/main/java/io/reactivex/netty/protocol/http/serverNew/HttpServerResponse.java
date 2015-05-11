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
package io.reactivex.netty.protocol.http.serverNew;

import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.protocol.http.TrailingHeaders;
import rx.Observable;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * An HTTP server response.
 *
 * <h2>Thread safety</h2>
 *
 * This object is <b>not</b> thread safe and should not be accessed from multiple threads.
 *
 * @param <C> The type of objects written as the content of the response.
 */
public abstract class HttpServerResponse<C> {

    /**
     * Returns the status of this response. If the status is not explicitly set, the default value is
     * {@link HttpResponseStatus#OK}
     *
     * @return The status of this response.
     */
    public abstract HttpResponseStatus getStatus();

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
     * @return A {@link java.util.List} of header values which will be empty if no values are found
     */
    public abstract List<String> getAllHeaderValues(CharSequence name);

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
    public abstract HttpServerResponse<C> addHeader(CharSequence name, Object value);

    /**
     * Adds the passed {@code cookie} to this response.
     *
     * @param cookie Cookie to add.
     *
     * @return {@code this}
     */
    public abstract HttpServerResponse<C> addCookie(Cookie cookie);

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
    public abstract HttpServerResponse<C> addDateHeader(CharSequence name, Date value);

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
    public abstract HttpServerResponse<C> addDateHeader(CharSequence name, Iterable<Date> values);

    /**
     * Adds an HTTP header with the passed {@code name} and {@code values} to this response.
     *
     * @param name Name of the header.
     * @param values Values for the header.
     *
     * @return {@code this}
     */
    public abstract HttpServerResponse<C> addHeader(CharSequence name, Iterable<Object> values);

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
    public abstract HttpServerResponse<C> setDateHeader(CharSequence name, Date value);

    /**
     * Overwrites the current value, if any, of the passed header to the passed value for this response.
     *
     * @param name Name of the header.
     * @param value Value of the header.
     *
     * @return {@code this}
     */
    public abstract HttpServerResponse<C> setHeader(CharSequence name, Object value);

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
    public abstract HttpServerResponse<C> setDateHeader(CharSequence name, Iterable<Date> values);

    /**
     * Overwrites the current value, if any, of the passed header to the passed values for this response.
     *
     * @param name Name of the header.
     * @param values Values of the header.
     *
     * @return {@code this}
     */
    public abstract HttpServerResponse<C> setHeader(CharSequence name, Iterable<Object> values);

    /**
     * Removes the passed header from this response.
     *
     * @param name Name of the header.
     *
     * @return {@code this}
     */
    public abstract HttpServerResponse<C> removeHeader(CharSequence name);

    /**
     * Sets the status for the response.
     *
     * @param status Status to set.
     *
     * @return {@code this}
     */
    public abstract HttpServerResponse<C> setStatus(HttpResponseStatus status);

    /**
     * Sets the HTTP transfer encoding to chunked for this response. This delegates to
     * {@link HttpHeaders#setTransferEncodingChunked(HttpMessage)}
     *
     * @return {@code this}
     */
    public abstract HttpServerResponse<C> setTransferEncodingChunked();

    /**
     * This is a performance optimization to <em>not</em> flush the channel on every response send.
     *
     * <h2>When NOT to use</h2>
     * This can be used
     * only when the processing for a server is not asynchronous, in which case, one would have to flush the responses
     * written explicitly (done on completion of the {@link Observable} written). Something like this:
     *
     <PRE>
     resp.sendHeaders()
         .writeStringAndFlushOnEach(Observable.interval(1, TimeUnit.SECONDS))
                                              .map(aLong -> "Interval =>" + aLong)
                                   )
     </PRE>
     *
     * <h2>When to use</h2>
     *
     * This can be used when the response is written synchronously from a {@link RequestHandler}, something like:
     *
     <PRE>
     response.writeString(Observable.just("Hello world");
     </PRE>
     *
     * When set, this will make the channel to be flushed only when all the requests available on the channel are
     * read. Thus, making it possible to do a gathering write for all pipelined requests on a connection. This reduces
     * the number of system calls and is helpful in "Hello World" benchmarks.
     */
    public abstract HttpServerResponse<C> flushOnlyOnReadComplete();

    /**
     * Writes the headers for the response on the underlying channel.
     *
     * Any modification to the headers after this point, will not be written on the channel and any subsequent calls to
     * this method will result in error.
     *
     * @return A {@link ContentWriter} to optionally write content. If this method has already been
     * called, then the returned {@link ContentWriter} will always throw an error when subscribed.
     */
    public abstract ContentWriter<C> sendHeaders();

    /**
     * A facility to optionally write content to the response.
     *
     * <h2>Thread safety</h2>
     *
     * This object is not thread-safe and can not be accessed from multiple threads.
     */
    public static abstract class ContentWriter<C> extends Observable<Void> {

        protected ContentWriter(OnSubscribe<Void> f) {
            super(f);
        }

        /**
         * On subscription of the returned {@link Observable}, writes the passed message stream on the underneath channel.
         *
         * <h2>Flush</h2>
         *
         * The writes are flushed when the passed stream completes.
         *
         * @param msgs Stream of messages to write.
         *
         * @return {@link Observable} representing the result of this write. Every subscription to this {@link Observable}
         * will replay the write on the channel.
         */
        public abstract ContentWriter<C> write(Observable<C> msgs);

        /**
         * Uses the passed {@link Observable} as the source of content for this request. This method provides a way to
         * write trailing headers.
         *
         * A new instance of {@link TrailingHeaders} will be created using the passed {@code trailerFactory} and the passed
         * {@code trailerMutator} will be invoked for every item emitted from the content source, giving a chance to modify
         * the trailing headers instance.
         *
         * <h2>Multiple invocations</h2>
         *
         * This method can <em>not</em> be invoked multiple times for the same response as on completion of the passed
         * source, it writes the trailing headers and trailing headers can only be written once for an HTTP response.
         * So, any subsequent invocation of this method will always emit an error when subscribed.
         *
         * <h2>Flush</h2>
         *
         * The writes are flushed when the passed stream completes.
         *
         * @param contentSource Content source for the response.
         * @param trailerFactory A factory function to create a new {@link TrailingHeaders} per subscription of the content.
         * @param trailerMutator A function to mutate the trailing header on each item emitted from the content source.
         *
         * @return An new instance of {@link Observable} which can be subscribed to execute the request.
         */
        public abstract <T extends TrailingHeaders> Observable<Void> write(Observable<C> contentSource,
                                                                           Func0<T> trailerFactory,
                                                                           Func2<T, C, T> trailerMutator);

        /**
         * Uses the passed {@link Observable} as the source of content for this request. This method provides a way to
         * write trailing headers.
         *
         * A new instance of {@link TrailingHeaders} will be created using the passed {@code trailerFactory} and the passed
         * {@code trailerMutator} will be invoked for every item emitted from the content source, giving a chance to modify
         * the trailing headers instance.
         *
         * <h2>Multiple invocations</h2>
         *
         * This method can <em>not</em> be invoked multiple times for the same response as on completion of the passed
         * source, it writes the trailing headers and trailing headers can only be written once for an HTTP response.
         * So, any subsequent invocation of this method will always emit an error when subscribed.
         *
         * @param contentSource Content source for the response.
         * @param trailerFactory A factory function to create a new {@link TrailingHeaders} per subscription of the content.
         * @param trailerMutator A function to mutate the trailing header on each item emitted from the content source.
         * @param flushSelector A {@link Func1} which is invoked for every item emitted from {@code msgs}. Channel is
         * flushed, iff this function returns, {@code true}.
         *
         * @return An new instance of {@link Observable} which can be subscribed to execute the request.
         */
        public abstract <T extends TrailingHeaders> Observable<Void> write(Observable<C> contentSource,
                                                                           Func0<T> trailerFactory,
                                                                           Func2<T, C, T> trailerMutator,
                                                                           Func1<C, Boolean> flushSelector);

        /**
         * On subscription of the returned {@link Observable}, writes the passed message stream on the underneath channel
         * and flushes the channel, everytime, {@code flushSelector} returns {@code true} . Any writes issued before
         * subscribing, will also be flushed. However, the returned {@link Observable} will not capture the result of those
         * writes, i.e. if the other writes, fail and this write does not, the returned {@link Observable} will not fail.
         *
         * @param msgs Message stream to write.
         * @param flushSelector A {@link Func1} which is invoked for every item emitted from {@code msgs}.
         * Channel is flushed, iff this function returns, {@code true}.
         *
         * @return An {@link Observable} representing the result of this and all writes done prior to the flush. Every
         * subscription to this {@link Observable} will write the passed messages and flush all pending writes, when the
         * {@code flushSelector} returns {@code true}
         */
        public abstract ContentWriter<C> write(Observable<C> msgs, Func1<C, Boolean> flushSelector);

        /**
         * On subscription of the returned {@link Observable}, writes the passed message stream on the underneath channel
         * and flushes the channel, on every write. Any writes issued before subscribing, will also be flushed. However, the
         * returned {@link Observable} will not capture the result of those writes, i.e. if the other writes, fail and this
         * write does not, the returned {@link Observable} will not fail.
         *
         * @param msgs Message stream to write.
         *
         * @return An {@link Observable} representing the result of this and all writes done prior to the flush. Every
         * subscription to this {@link Observable} will write the passed messages and flush all pending writes, on every
         * write.
         */
        public abstract ContentWriter<C> writeAndFlushOnEach(Observable<C> msgs);

        /**
         * On subscription of the returned {@link Observable}, writes the passed message stream on the underneath channel.
         *
         * <h2>Flush</h2>
         *
         * The writes are flushed when the passed stream completes.
         *
         * @param msgs Stream of messages to write.
         *
         * @return {@link Observable} representing the result of this write. Every subscription to this {@link Observable}
         * will replay the write on the channel.
         */
        public abstract ContentWriter<C> writeString(Observable<String> msgs);

        /**
         * Uses the passed {@link Observable} as the source of content for this request. This method provides a way to
         * write trailing headers.
         *
         * A new instance of {@link TrailingHeaders} will be created using the passed {@code trailerFactory} and the passed
         * {@code trailerMutator} will be invoked for every item emitted from the content source, giving a chance to modify
         * the trailing headers instance.
         *
         * <h2>Multiple invocations</h2>
         *
         * This method can <em>not</em> be invoked multiple times for the same response as on completion of the passed
         * source, it writes the trailing headers and trailing headers can only be written once for an HTTP response.
         * So, any subsequent invocation of this method will always emit an error when subscribed.
         *
         * <h2>Flush</h2>
         *
         * The writes are flushed when the passed stream completes.
         *
         * @param contentSource Content source for the response.
         * @param trailerFactory A factory function to create a new {@link TrailingHeaders} per subscription of the content.
         * @param trailerMutator A function to mutate the trailing header on each item emitted from the content source.
         *
         * @return An new instance of {@link Observable} which can be subscribed to execute the request.
         */
        public abstract <T extends TrailingHeaders> Observable<Void> writeString(Observable<String> contentSource,
                                                                                 Func0<T> trailerFactory,
                                                                                 Func2<T, String, T> trailerMutator);

        /**
         * Uses the passed {@link Observable} as the source of content for this request. This method provides a way to
         * write trailing headers.
         *
         * A new instance of {@link TrailingHeaders} will be created using the passed {@code trailerFactory} and the passed
         * {@code trailerMutator} will be invoked for every item emitted from the content source, giving a chance to modify
         * the trailing headers instance.
         *
         * <h2>Multiple invocations</h2>
         *
         * This method can <em>not</em> be invoked multiple times for the same response as on completion of the passed
         * source, it writes the trailing headers and trailing headers can only be written once for an HTTP response.
         * So, any subsequent invocation of this method will always emit an error when subscribed.
         *
         * @param contentSource Content source for the response.
         * @param trailerFactory A factory function to create a new {@link TrailingHeaders} per subscription of the content.
         * @param trailerMutator A function to mutate the trailing header on each item emitted from the content source.
         * @param flushSelector A {@link Func1} which is invoked for every item emitted from {@code msgs}. Channel is
         * flushed, iff this function returns, {@code true}.
         *
         * @return An new instance of {@link Observable} which can be subscribed to execute the request.
         */
        public abstract <T extends TrailingHeaders> Observable<Void> writeString(Observable<String> contentSource,
                                                                                 Func0<T> trailerFactory,
                                                                                 Func2<T, String, T> trailerMutator,
                                                                                 Func1<String, Boolean> flushSelector);

        /**
         * On subscription of the returned {@link Observable}, writes the passed message stream on the underneath channel
         * and flushes the channel, everytime, {@code flushSelector} returns {@code true} . Any writes issued before
         * subscribing, will also be flushed. However, the returned {@link Observable} will not capture the result of those
         * writes, i.e. if the other writes, fail and this write does not, the returned {@link Observable} will not fail.
         *
         * @param msgs Message stream to write.
         * @param flushSelector A {@link Func1} which is invoked for every item emitted from {@code msgs}.
         * Channel is flushed, iff this function returns, {@code true}.
         *
         * @return An {@link Observable} representing the result of this and all writes done prior to the flush. Every
         * subscription to this {@link Observable} will write the passed messages and flush all pending writes, when the
         * {@code flushSelector} returns {@code true}
         */
        public abstract ContentWriter<C> writeString(Observable<String> msgs, Func1<String, Boolean> flushSelector);

        /**
         * On subscription of the returned {@link Observable}, writes the passed message stream on the underneath channel
         * and flushes the channel, on every write. Any writes issued before subscribing, will also be flushed. However, the
         * returned {@link Observable} will not capture the result of those writes, i.e. if the other writes, fail and this
         * write does not, the returned {@link Observable} will not fail.
         *
         * @param msgs Message stream to write.
         *
         * @return An {@link Observable} representing the result of this and all writes done prior to the flush. Every
         * subscription to this {@link Observable} will write the passed messages and flush all pending writes, on every
         * write.
         */
        public abstract ContentWriter<C> writeStringAndFlushOnEach(Observable<String> msgs);

        /**
         * On subscription of the returned {@link Observable}, writes the passed message stream on the underneath channel.
         *
         * <h2>Flush</h2>
         *
         * The writes are flushed when the passed stream completes.
         *
         * @param msgs Stream of messages to write.
         *
         * @return {@link Observable} representing the result of this write. Every subscription to this {@link Observable}
         * will replay the write on the channel.
         */
        public abstract ContentWriter<C> writeBytes(Observable<byte[]> msgs);

        /**
         * Uses the passed {@link Observable} as the source of content for this request. This method provides a way to
         * write trailing headers.
         *
         * A new instance of {@link TrailingHeaders} will be created using the passed {@code trailerFactory} and the passed
         * {@code trailerMutator} will be invoked for every item emitted from the content source, giving a chance to modify
         * the trailing headers instance.
         *
         * <h2>Multiple invocations</h2>
         *
         * This method can <em>not</em> be invoked multiple times for the same response as on completion of the passed
         * source, it writes the trailing headers and trailing headers can only be written once for an HTTP response.
         * So, any subsequent invocation of this method will always emit an error when subscribed.
         *
         * <h2>Flush</h2>
         *
         * The writes are flushed when the passed stream completes.
         *
         * @param contentSource Content source for the response.
         * @param trailerFactory A factory function to create a new {@link TrailingHeaders} per subscription of the content.
         * @param trailerMutator A function to mutate the trailing header on each item emitted from the content source.
         *
         * @return An new instance of {@link Observable} which can be subscribed to execute the request.
         */
        public abstract <T extends TrailingHeaders> Observable<Void> writeBytes(Observable<byte[]> contentSource,
                                                                                Func0<T> trailerFactory,
                                                                                Func2<T, byte[], T> trailerMutator);

        /**
         * Uses the passed {@link Observable} as the source of content for this request. This method provides a way to
         * write trailing headers.
         *
         * A new instance of {@link TrailingHeaders} will be created using the passed {@code trailerFactory} and the passed
         * {@code trailerMutator} will be invoked for every item emitted from the content source, giving a chance to modify
         * the trailing headers instance.
         *
         * <h2>Multiple invocations</h2>
         *
         * This method can <em>not</em> be invoked multiple times for the same response as on completion of the passed
         * source, it writes the trailing headers and trailing headers can only be written once for an HTTP response.
         * So, any subsequent invocation of this method will always emit an error when subscribed.
         *
         * @param contentSource Content source for the response.
         * @param trailerFactory A factory function to create a new {@link TrailingHeaders} per subscription of the content.
         * @param trailerMutator A function to mutate the trailing header on each item emitted from the content source.
         * @param flushSelector A {@link Func1} which is invoked for every item emitted from {@code msgs}. Channel is
         * flushed, iff this function returns, {@code true}.
         *
         * @return An new instance of {@link Observable} which can be subscribed to execute the request.
         */
        public abstract <T extends TrailingHeaders> Observable<Void> writeBytes(Observable<byte[]> contentSource,
                                                                                Func0<T> trailerFactory,
                                                                                Func2<T, byte[], T> trailerMutator,
                                                                                Func1<byte[], Boolean> flushSelector);

        /**
         * On subscription of the returned {@link Observable}, writes the passed message stream on the underneath channel
         * and flushes the channel, everytime, {@code flushSelector} returns {@code true} . Any writes issued before
         * subscribing, will also be flushed. However, the returned {@link Observable} will not capture the result of those
         * writes, i.e. if the other writes, fail and this write does not, the returned {@link Observable} will not fail.
         *
         * @param msgs Message stream to write.
         * @param flushSelector A {@link Func1} which is invoked for every item emitted from {@code msgs}.
         * Channel is flushed, iff this function returns, {@code true}.
         *
         * @return An {@link Observable} representing the result of this and all writes done prior to the flush. Every
         * subscription to this {@link Observable} will write the passed messages and flush all pending writes, when the
         * {@code flushSelector} returns {@code true}
         */
        public abstract ContentWriter<C> writeBytes(Observable<byte[]> msgs, Func1<byte[], Boolean> flushSelector);

        /**
         * On subscription of the returned {@link Observable}, writes the passed message stream on the underneath channel
         * and flushes the channel, on every write. Any writes issued before subscribing, will also be flushed. However, the
         * returned {@link Observable} will not capture the result of those writes, i.e. if the other writes, fail and this
         * write does not, the returned {@link Observable} will not fail.
         *
         * @param msgs Message stream to write.
         *
         * @return An {@link Observable} representing the result of this and all writes done prior to the flush. Every
         * subscription to this {@link Observable} will write the passed messages and flush all pending writes, on every
         * write.
         */
        public abstract ContentWriter<C> writeBytesAndFlushOnEach(Observable<byte[]> msgs);
    }
}
