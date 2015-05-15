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

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.concurrent.EventExecutorGroup;
import rx.Observable;
import rx.annotations.Experimental;
import rx.functions.Action1;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * An instance of this class can only be obtained from {@link HttpClientRequest} and is an optimization to reduce the
 * creation of intermediate {@link HttpClientRequest} objects while doing multiple mutations on the request.
 *
 * Semantically, the operations here are exactly the same as those in {@link HttpClientRequest}, the updates are applied
 * (by invoking {@link #update()}) as a single batch to create a new {@link HttpClientRequest} instance.
 *
 * @author Nitesh Kant
 */
@Experimental
public abstract class HttpClientRequestUpdater<I, O> implements HttpClientRequestOperations<I, HttpClientRequestUpdater<I, O>> {

    /**
     * Uses the passed {@link rx.Observable} as the content source for the newly created and returned
     * {@link HttpClientRequest}.
     *
     * @param contentSource Content source for the request.
     *
     * @return This updater.
     */
    @Override
    public abstract HttpClientRequestUpdater<I, O> setContentSource(Observable<I> contentSource);

    /**
     * Uses the passed {@code content} as the content for the newly created and returned
     * {@link HttpClientRequest}. This is equivalent to calling
     * {@code
     *      setContentSource(Observable.just(content));
     * }
     *
     * @param content Content for the request.
     *
     * @return This updater.
     */
    @Override
    public abstract HttpClientRequestUpdater<I, O> setContent(I content);

    /**
     * Uses the passed {@code content} as the content for the newly created and returned
     * {@link HttpClientRequest}. This is equivalent to calling
     * {@code
     *      setContent(content.getBytes(Charset.defaultCharset()));
     * }
     *
     * @param content Content for the request.
     *
     * @return This updater.
     */
    @Override
    public abstract HttpClientRequestUpdater<I, O> setStringContent(String content);


    /**
     * Uses the passed {@code content} as the content for the newly created and returned
     * {@link HttpClientRequest}. This is equivalent to calling
     * {@code
     *      setRawContentSource(Observable.just(content), ByteTransformer.DEFAULT_INSTANCE);
     * }
     *
     * @param content Content for the request.
     *
     * @return This updater.
     */
    @Override
    public abstract HttpClientRequestUpdater<I, O> setBytesContent(byte[] content);

    /**
     * Enables read timeout for the response of the newly created and returned request.
     *
     * @param timeOut Read timeout duration.
     * @param timeUnit Read timeout time unit.
     *
     * @return This updater.
     */
    @Override
    public abstract HttpClientRequestUpdater<I, O> readTimeOut(int timeOut, TimeUnit timeUnit);

    /**
     * Enables following HTTP redirects for the newly created and returned request.
     *
     * @param maxRedirects Maximum number of redirects allowed.
     *
     * @return This updater.
     */
    @Override
    public abstract HttpClientRequestUpdater<I, O> followRedirects(int maxRedirects);

    /**
     * Enables/disables following HTTP redirects for the newly created and returned request.
     *
     * @param follow {@code true} for enabling redirects, {@code false} to disable.
     *
     * @return This updater.
     */
    @Override
    public abstract HttpClientRequestUpdater<I, O> followRedirects(boolean follow);

    /**
     * Adds an HTTP header with the passed {@code name} and {@code value} to this request.
     *
     * @param name Name of the header.
     * @param value Value for the header.
     *
     * @return This updater.
     */
    @Override
    public abstract HttpClientRequestUpdater<I, O> addHeader(CharSequence name, Object value);

    /**
     * Adds the passed {@code cookie} to this request.
     *
     * @param cookie Cookie to add.
     *
     * @return This updater.
     */
    @Override
    public abstract HttpClientRequestUpdater<I, O> addCookie(Cookie cookie);

    /**
     * Adds the passed header as a date value to this request. The date is formatted using netty's
     * {@link io.netty.handler.codec.http.HttpHeaders#addDateHeader(io.netty.handler.codec.http.HttpMessage, CharSequence, java.util.Date)} which formats the date as per the
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3.1">HTTP specifications</a> into the format:
     *
     <PRE>"E, dd MMM yyyy HH:mm:ss z"</PRE>
     *
     * @param name Name of the header.
     * @param value Value of the header.
     *
     * @return This updater.
     */
    @Override
    public abstract HttpClientRequestUpdater<I, O> addDateHeader(CharSequence name, Date value);

    /**
     * Adds multiple date values for the passed header name to this request. The date values are formatted using netty's
     * {@link io.netty.handler.codec.http.HttpHeaders#addDateHeader(io.netty.handler.codec.http.HttpMessage, CharSequence, Date)} which formats the date as per the
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3.1">HTTP specifications</a> into the format:
     *
     <PRE>"E, dd MMM yyyy HH:mm:ss z"</PRE>
     *
     * @param name Name of the header.
     * @param values Values for the header.
     *
     * @return This updater.
     */
    @Override
    public abstract HttpClientRequestUpdater<I, O> addDateHeader(CharSequence name, Iterable<Date> values);

    /**
     * Adds an HTTP header with the passed {@code name} and {@code values} to this request.
     *
     * @param name Name of the header.
     * @param values Values for the header.
     *
     * @return This updater.
     */
    @Override
    public abstract HttpClientRequestUpdater<I, O> addHeader(CharSequence name, Iterable<Object> values);

    /**
     * Overwrites the current value, if any, of the passed header to the passed date value for this request.
     * The date is formatted using netty's {@link io.netty.handler.codec.http.HttpHeaders#addDateHeader(io.netty.handler.codec.http.HttpMessage, CharSequence, Date)} which
     * formats the date as per the
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3.1">HTTP specifications</a> into the format:
     *
     <PRE>"E, dd MMM yyyy HH:mm:ss z"</PRE>
     *
     * @param name Name of the header.
     * @param value Value of the header.
     *
     * @return This updater.
     */
    @Override
    public abstract HttpClientRequestUpdater<I, O> setDateHeader(CharSequence name, Date value);

    /**
     * Overwrites the current value, if any, of the passed header to the passed value for this request.
     *
     * @param name Name of the header.
     * @param value Value of the header.
     *
     * @return This updater.
     */
    @Override
    public abstract HttpClientRequestUpdater<I, O> setHeader(CharSequence name, Object value);

    /**
     * Overwrites the current value, if any, of the passed header to the passed date values for this request.
     * The date is formatted using netty's {@link io.netty.handler.codec.http.HttpHeaders#addDateHeader(io.netty.handler.codec.http.HttpMessage, CharSequence, Date)} which
     * formats the date as per the
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3.1">HTTP specifications</a> into the format:
     *
     <PRE>"E, dd MMM yyyy HH:mm:ss z"</PRE>
     *
     * @param name Name of the header.
     * @param values Values of the header.
     *
     * @return This updater.
     */
    @Override
    public abstract HttpClientRequestUpdater<I, O> setDateHeader(CharSequence name, Iterable<Date> values);

    /**
     * Overwrites the current value, if any, of the passed header to the passed values for this request.
     *
     * @param name Name of the header.
     * @param values Values of the header.
     *
     * @return This updater.
     */
    @Override
    public abstract HttpClientRequestUpdater<I, O> setHeader(CharSequence name, Iterable<Object> values);

    /**
     * Sets HTTP Connection header to the appropriate value for HTTP keep-alive.
     * This delegates to {@link io.netty.handler.codec.http.HttpHeaders#setKeepAlive(io.netty.handler.codec.http.HttpMessage, boolean)}
     *
     * @param keepAlive {@code true} to enable keep alive.
     *
     * @return This updater.
     */
    @Override
    public abstract HttpClientRequestUpdater<I, O> setKeepAlive(boolean keepAlive);

    /**
     * Sets the HTTP transfer encoding to chunked for this request.
     * This delegates to {@link io.netty.handler.codec.http.HttpHeaders#setTransferEncodingChunked(io.netty.handler.codec.http.HttpMessage)}
     *
     * @return This updater.
     */
    @Override
    public abstract HttpClientRequestUpdater<I, O> setTransferEncodingChunked();

    /**
     * Adds a {@link io.netty.channel.ChannelHandler} to {@link io.netty.channel.ChannelPipeline} for the connection used by this request. The specified
     * handler is added at the first position of the pipeline as specified by
     * {@link io.netty.channel.ChannelPipeline#addFirst(String, io.netty.channel.ChannelHandler)}
     *
     * @param name Name of the handler.
     * @param handler Handler instance to add.
     *
     * @return This updater.
     */
    public abstract HttpClientRequestUpdater<I, O> addChannelHandlerFirst(String name, ChannelHandler handler);

    /**
     * Adds a {@link ChannelHandler} to {@link io.netty.channel.ChannelPipeline} for the connection used by this request. The specified
     * handler is added at the first position of the pipeline as specified by
     * {@link io.netty.channel.ChannelPipeline#addFirst(io.netty.util.concurrent.EventExecutorGroup, String, ChannelHandler)}
     *
     * @param group   the {@link io.netty.util.concurrent.EventExecutorGroup} which will be used to execute the {@link ChannelHandler}
     *                 methods
     * @param name     the name of the handler to append
     * @param handler  the handler to append
     *
     * @return This updater.
     */
    public abstract <II, OO> HttpClientRequestUpdater<II, OO> addChannelHandlerFirst(EventExecutorGroup group, String name,
                                                                                     ChannelHandler handler);

    /**
     * Adds a {@link ChannelHandler} to {@link io.netty.channel.ChannelPipeline} for the connection used by this request. The specified
     * handler is added at the last position of the pipeline as specified by
     * {@link io.netty.channel.ChannelPipeline#addLast(String, ChannelHandler)}
     *
     * @param name Name of the handler.
     * @param handler Handler instance to add.
     *
     * @return This updater.
     */
    public abstract <II, OO> HttpClientRequestUpdater<II, OO> addChannelHandlerLast(String name, ChannelHandler handler);

    /**
     * Adds a {@link ChannelHandler} to {@link io.netty.channel.ChannelPipeline} for the connection used by this request. The specified
     * handler is added at the last position of the pipeline as specified by
     * {@link io.netty.channel.ChannelPipeline#addLast(EventExecutorGroup, String, ChannelHandler)}
     *
     * @param group   the {@link EventExecutorGroup} which will be used to execute the {@link ChannelHandler}
     *                 methods
     * @param name     the name of the handler to append
     * @param handler  the handler to append
     *
     * @return This updater.
     */
    public abstract <II, OO> HttpClientRequestUpdater<II, OO> addChannelHandlerLast(EventExecutorGroup group,
                                                                                    String name, ChannelHandler handler);

    /**
     * Adds a {@link ChannelHandler} to {@link io.netty.channel.ChannelPipeline} for the connection used by this request. The specified
     * handler is added before an existing handler with the passed {@code baseName} in the pipeline as specified by
     * {@link io.netty.channel.ChannelPipeline#addBefore(String, String, ChannelHandler)}
     *
     * @param baseName  the name of the existing handler
     * @param name Name of the handler.
     * @param handler Handler instance to add.
     *
     * @return This updater.
     */
    public abstract <II, OO> HttpClientRequestUpdater<II, OO> addChannelHandlerBefore(String baseName, String name,
                                                                                      ChannelHandler handler);

    /**
     * Adds a {@link ChannelHandler} to {@link io.netty.channel.ChannelPipeline} for the connection used by this request. The specified
     * handler is added before an existing handler with the passed {@code baseName} in the pipeline as specified by
     * {@link io.netty.channel.ChannelPipeline#addBefore(EventExecutorGroup, String, String, ChannelHandler)}
     *
     * @param group   the {@link EventExecutorGroup} which will be used to execute the {@link ChannelHandler}
     *                 methods
     * @param baseName  the name of the existing handler
     * @param name     the name of the handler to append
     * @param handler  the handler to append
     *
     * @return This updater.
     */
    public abstract <II, OO> HttpClientRequestUpdater<II, OO> addChannelHandlerBefore(EventExecutorGroup group,
                                                                                      String baseName, String name,
                                                                                      ChannelHandler handler);

    /**
     * Adds a {@link ChannelHandler} to {@link io.netty.channel.ChannelPipeline} for the connection used by this request. The specified
     * handler is added after an existing handler with the passed {@code baseName} in the pipeline as specified by
     * {@link io.netty.channel.ChannelPipeline#addAfter(String, String, ChannelHandler)}
     *
     * @param baseName  the name of the existing handler
     * @param name Name of the handler.
     * @param handler Handler instance to add.
     *
     * @return This updater.
     */
    public abstract <II, OO> HttpClientRequestUpdater<II, OO> addChannelHandlerAfter(String baseName, String name,
                                                                                     ChannelHandler handler);

    /**
     * Adds a {@link ChannelHandler} to {@link io.netty.channel.ChannelPipeline} for the connection used by this request. The specified
     * handler is added after an existing handler with the passed {@code baseName} in the pipeline as specified by
     * {@link io.netty.channel.ChannelPipeline#addAfter(EventExecutorGroup, String, String, ChannelHandler)}
     *
     * @param group   the {@link EventExecutorGroup} which will be used to execute the {@link ChannelHandler}
     *                 methods
     * @param baseName  the name of the existing handler
     * @param name     the name of the handler to append
     * @param handler  the handler to append
     *
     * @return This updater.
     */
    public abstract <II, OO> HttpClientRequestUpdater<II, OO>  addChannelHandlerAfter(EventExecutorGroup group,
                                                                                      String baseName, String name,
                                                                                      ChannelHandler handler);

    /**
     * Configures an action to configure the {@link io.netty.channel.ChannelPipeline} for the connection used by this request.
     *
     * @param configurator Action that will be used to configure the pipeline.
     *
     * @return This updater.
     */
    public abstract <II, OO> HttpClientRequestUpdater<II, OO> withPipelineConfigurator(Action1<ChannelPipeline> configurator);

    /**
     * Checks whether a header with the passed name exists for this request.
     *
     * @param name Header name.
     *
     * @return {@code true} if the header exists.
     */
    @Override
    public abstract boolean containsHeader(CharSequence name);

    /**
     * Checks whether a header with the passed name and value exists for this request.
     *
     * @param name Header name.
     * @param value Value to check.
     * @param caseInsensitiveValueMatch If the value has to be matched ignoring case.
     *
     * @return {@code true} if the header with the passed value exists.
     */
    @Override
    public abstract boolean containsHeaderWithValue(CharSequence name, CharSequence value, boolean caseInsensitiveValueMatch);

    /**
     * Fetches the value of a header, if exists, for this request.
     *
     * @param name Name of the header.
     *
     * @return The value of the header, if it exists, {@code null} otherwise. If there are multiple values for this
     * header, the first value is returned.
     */
    @Override
    public abstract String getHeader(CharSequence name);

    /**
     * Fetches all values of a header, if exists, for this request.
     *
     * @param name Name of the header.
     *
     * @return All values of the header, if it exists, {@code null} otherwise.
     */
    @Override
    public abstract List<String> getAllHeaders(CharSequence name);

    /**
     * Applies all changes done via this updater to the original snapshot of state from {@link HttpClientRequest} and
     * creates a new {@link HttpClientRequest} instance.
     *
     * @return New instance of {@link HttpClientRequest} with all changes done via this updater.
     */
    public abstract HttpClientRequestUpdater<I, O> update();

    /**
     * Returns the HTTP version of this request.
     *
     * @return The HTTP version of this request.
     */
    @Override
    public abstract HttpVersion getHttpVersion();

    /**
     * Returns the HTTP method for this request.
     *
     * @return The HTTP method for this request.
     */
    @Override
    public abstract HttpMethod getMethod();

    /**
     * Returns the URI for this request.
     * The returned URI does <em>not</em> contain the scheme, host and port portion of the URI. In case, it is required,
     * {@link #getAbsoluteUri()} must be used.
     *
     * @return The URI for this request.
     */
    @Override
    public abstract String getUri();

    /**
     * Returns the absolute URI for this request including the scheme, host and port portion of the URI.
     *
     * @return The absolute URI for this request.
     */
    @Override
    public abstract String getAbsoluteUri();
}
