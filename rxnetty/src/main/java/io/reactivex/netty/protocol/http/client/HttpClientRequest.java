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

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.EventExecutorGroup;
import io.reactivex.netty.protocol.http.TrailingHeaders;
import io.reactivex.netty.protocol.http.ws.client.WebSocketRequest;
import rx.Observable;
import rx.annotations.Experimental;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * An HTTP request. An instance of a request can only be created from an associated {@link HttpClient} and can be
 * modified after creation.
 *
 * <h2>Request URIs</h2>
 *
 * While creating a request, the user should provide a URI to be used for the request. The URI can be relative or
 * absolute. If the URI is relative (missing host and port information), the target host and port are inferred from the
 * {@link HttpClient} that created the request. If the URI is absolute, the host and port are used from the URI.
 *
 * <h2>Mutations</h2>
 *
 * All mutations to this request creates a brand new instance.

 * <h2>Trailing headers</h2>
 *
 * One can write HTTP trailing headers by using
 *
 * <h2> Executing request</h2>
 *
 * The request is executed every time {@link HttpClientRequest}, or {@link Observable} returned by
 * {@code write*Content} is subscribed and is the only way of executing the request.
 *
 * @param <I> The type of objects read from the request content.
 * @param <O> The type of objects read from the response content.
 */
public abstract class HttpClientRequest<I, O> extends Observable<HttpClientResponse<O>> {

    protected HttpClientRequest(OnSubscribe<HttpClientResponse<O>> onSubscribe) {
        super(onSubscribe);
    }

    /**
     * Uses the passed {@link Observable} as the source of content for this request.
     *
     * @param contentSource Content source for the request.
     *
     * @return An new instance of {@link Observable} which can be subscribed to execute the request.
     */
    public abstract Observable<HttpClientResponse<O>> writeContent(Observable<I> contentSource);

    /**
     * Uses the passed {@link Observable} as the source of content for this request. Every item is written and flushed
     * immediately.
     *
     * @param contentSource Content source for the request.
     *
     * @return An new instance of {@link Observable} which can be subscribed to execute the request.
     */
    public abstract Observable<HttpClientResponse<O>> writeContentAndFlushOnEach(Observable<I> contentSource);

    /**
     * Uses the passed {@link Observable} as the source of content for this request.
     *
     * @param contentSource Content source for the request.
     * @param flushSelector A {@link Func1} which is invoked for every item emitted from {@code msgs}. All pending
     * writes are flushed, iff this function returns, {@code true}.
     *
     * @return An new instance of {@link Observable} which can be subscribed to execute the request.
     */
    public abstract Observable<HttpClientResponse<O>> writeContent(Observable<I> contentSource,
                                                                   Func1<I, Boolean> flushSelector);

    /**
     * Uses the passed {@link Observable} as the source of content for this request. This method provides a way to
     * write trailing headers.
     *
     * A new instance of {@link TrailingHeaders} will be created using the passed {@code trailerFactory} and the passed
     * {@code trailerMutator} will be invoked for every item emitted from the content source, giving a chance to modify
     * the trailing headers instance.
     *
     * @param contentSource Content source for the request.
     * @param trailerFactory A factory function to create a new {@link TrailingHeaders} per subscription of the content.
     * @param trailerMutator A function to mutate the trailing header on each item emitted from the content source.
     *
     * @return An new instance of {@link Observable} which can be subscribed to execute the request.
     */
    @Experimental
    public abstract <T extends TrailingHeaders> Observable<HttpClientResponse<O>> writeContent(Observable<I> contentSource,
                                                                                               Func0<T> trailerFactory,
                                                                                               Func2<T, I, T> trailerMutator);

    /**
     * Uses the passed {@link Observable} as the source of content for this request. This method provides a way to
     * write trailing headers.
     *
     * A new instance of {@link TrailingHeaders} will be created using the passed {@code trailerFactory} and the passed
     * {@code trailerMutator} will be invoked for every item emitted from the content source, giving a chance to modify
     * the trailing headers instance.
     *
     * @param contentSource Content source for the request.
     * @param trailerFactory A factory function to create a new {@link TrailingHeaders} per subscription of the content.
     * @param trailerMutator A function to mutate the trailing header on each item emitted from the content source.
     * @param flushSelector A {@link Func1} which is invoked for every item emitted from {@code msgs}. All pending
     * writes are flushed, iff this function returns, {@code true}.
     *
     * @return An new instance of {@link Observable} which can be subscribed to execute the request.
     */
    @Experimental
    public abstract <T extends TrailingHeaders> Observable<HttpClientResponse<O>> writeContent(Observable<I> contentSource,
                                                                                               Func0<T> trailerFactory,
                                                                                               Func2<T, I, T> trailerMutator,
                                                                                               Func1<I, Boolean> flushSelector);

    /**
     * Uses the passed {@link Observable} as the source of content for this request.
     *
     * @param contentSource Content source for the request.
     *
     * @return An new instance of {@link Observable} which can be subscribed to execute the request.
     */
    public abstract Observable<HttpClientResponse<O>> writeStringContent(Observable<String> contentSource);

    /**
     * Uses the passed {@link Observable} as the source of content for this request.
     *
     * @param contentSource Content source for the request.
     * @param flushSelector A {@link Func1} which is invoked for every item emitted from {@code msgs}. All pending
     * writes are flushed, iff this function returns, {@code true}.
     *
     * @return An new instance of {@link Observable} which can be subscribed to execute the request.
     */
    public abstract Observable<HttpClientResponse<O>> writeStringContent(Observable<String> contentSource,
                                                                         Func1<String, Boolean> flushSelector);

    /**
     * Uses the passed {@link Observable} as the source of content for this request. This method provides a way to
     * write trailing headers.
     *
     * A new instance of {@link TrailingHeaders} will be created using the passed {@code trailerFactory} and the passed
     * {@code trailerMutator} will be invoked for every item emitted from the content source, giving a chance to modify
     * the trailing headers instance.
     *
     * @param contentSource Content source for the request.
     * @param trailerFactory A factory function to create a new {@link TrailingHeaders} per subscription of the content.
     * @param trailerMutator A function to mutate the trailing header on each item emitted from the content source.
     *
     * @return An new instance of {@link Observable} which can be subscribed to execute the request.
     */
    @Experimental
    public abstract <T extends TrailingHeaders> Observable<HttpClientResponse<O>> writeStringContent(Observable<String> contentSource,
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
     * @param contentSource Content source for the request.
     * @param trailerFactory A factory function to create a new {@link TrailingHeaders} per subscription of the content.
     * @param trailerMutator A function to mutate the trailing header on each item emitted from the content source.
     * @param flushSelector A {@link Func1} which is invoked for every item emitted from {@code msgs}. All pending
     * writes are flushed, iff this function returns, {@code true}.
     *
     * @return An new instance of {@link Observable} which can be subscribed to execute the request.
     */
    @Experimental
    public abstract <T extends TrailingHeaders> Observable<HttpClientResponse<O>> writeStringContent(Observable<String> contentSource,
                                                                                                     Func0<T> trailerFactory,
                                                                                                     Func2<T, String, T> trailerMutator,
                                                                                                     Func1<String, Boolean> flushSelector);

    /**
     * Uses the passed {@link Observable} as the source of content for this request.
     *
     * @param contentSource Content source for the request.
     *
     * @return An new instance of {@link Observable} which can be subscribed to execute the request.
     */
    public abstract Observable<HttpClientResponse<O>> writeBytesContent(Observable<byte[]> contentSource);

    /**
     * Uses the passed {@link Observable} as the source of content for this request.
     *
     * @param contentSource Content source for the request.
     * @param flushSelector A {@link Func1} which is invoked for every item emitted from {@code msgs}. All pending
     * writes are flushed, iff this function returns, {@code true}.
     *
     * @return An new instance of {@link Observable} which can be subscribed to execute the request.
     */
    public abstract Observable<HttpClientResponse<O>> writeBytesContent(Observable<byte[]> contentSource,
                                                                        Func1<byte[], Boolean> flushSelector);

    /**
     * Uses the passed {@link Observable} as the source of content for this request. This method provides a way to
     * write trailing headers.
     *
     * A new instance of {@link TrailingHeaders} will be created using the passed {@code trailerFactory} and the passed
     * {@code trailerMutator} will be invoked for every item emitted from the content source, giving a chance to modify
     * the trailing headers instance.
     *
     * @param contentSource Content source for the request.
     * @param trailerFactory A factory function to create a new {@link TrailingHeaders} per subscription of the content.
     * @param trailerMutator A function to mutate the trailing header on each item emitted from the content source.
     *
     * @return An new instance of {@link Observable} which can be subscribed to execute the request.
     */
    @Experimental
    public abstract <T extends TrailingHeaders> Observable<HttpClientResponse<O>> writeBytesContent(Observable<byte[]> contentSource,
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
     * @param contentSource Content source for the request.
     * @param trailerFactory A factory function to create a new {@link TrailingHeaders} per subscription of the content.
     * @param trailerMutator A function to mutate the trailing header on each item emitted from the content source.
     * @param flushSelector A {@link Func1} which is invoked for every item emitted from {@code msgs}. All pending
     * writes are flushed, iff this function returns, {@code true}.
     *
     * @return An new instance of {@link Observable} which can be subscribed to execute the request.
     */
    @Experimental
    public abstract <T extends TrailingHeaders> Observable<HttpClientResponse<O>> writeBytesContent(Observable<byte[]> contentSource,
                                                                                                    Func0<T> trailerFactory,
                                                                                                    Func2<T, byte[], T> trailerMutator,
                                                                                                    Func1<byte[], Boolean> flushSelector);

    /**
     * Enables read timeout for the response of the newly created and returned request.
     *
     * @param timeOut Read timeout duration.
     * @param timeUnit Read timeout time unit.
     *
     * @return A new instance of the {@link HttpClientRequest} sharing all existing state from this request.
     */
    public abstract HttpClientRequest<I, O> readTimeOut(int timeOut, TimeUnit timeUnit);

    /**
     * Enables following HTTP redirects for the newly created and returned request.
     *
     * @param maxRedirects Maximum number of redirects allowed.
     *
     * @return A new instance of the {@link HttpClientRequest} sharing all existing state from this request.
     */
    public abstract HttpClientRequest<I, O> followRedirects(int maxRedirects);

    /**
     * Enables/disables following HTTP redirects for the newly created and returned request.
     *
     * @param follow {@code true} for enabling redirects, {@code false} to disable.
     *
     * @return A new instance of the {@link HttpClientRequest} sharing all existing state from this request.
     */
    public abstract HttpClientRequest<I, O> followRedirects(boolean follow);

    /**
     * Updates the HTTP method of the request and creates a new {@link HttpClientRequest} instance.
     *
     * @param method New HTTP method to use.
     *
     * @return A new instance of the {@link HttpClientRequest} sharing all existing state from this request.
     */
    public abstract HttpClientRequest<I, O> setMethod(HttpMethod method);

    /**
     * Updates the URI of the request and creates a new {@link HttpClientRequest} instance.
     *
     * @param newUri New URI to use.
     *
     * @return A new instance of the {@link HttpClientRequest} sharing all existing state from this request.
     */
    public abstract HttpClientRequest<I, O> setUri(String newUri);

    /**
     * Adds an HTTP header with the passed {@code name} and {@code value} to this request.
     *
     * @param name Name of the header.
     * @param value Value for the header.
     *
     * @return A new instance of the {@link HttpClientRequest} sharing all existing state from this request.
     */
    public abstract HttpClientRequest<I, O> addHeader(CharSequence name, Object value);

    /**
     * Adds the passed {@code cookie} to this request.
     *
     * @param cookie Cookie to add.
     *
     * @return A new instance of the {@link HttpClientRequest} sharing all existing state from this request.
     */
    public abstract HttpClientRequest<I, O> addCookie(Cookie cookie);

    /**
     * Adds the passed header as a date value to this request. The date is formatted using netty's {@link
     * HttpHeaders#addDateHeader(HttpMessage, CharSequence, Date)} which formats the date as per the <a
     * href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3.1">HTTP specifications</a> into the format:
     * <p/>
     * <PRE>"E, dd MMM yyyy HH:mm:ss z"</PRE>
     *
     * @param name Name of the header.
     * @param value Value of the header.
     *
     * @return A new instance of the {@link HttpClientRequest} sharing all existing state from this request.
     */
    public abstract HttpClientRequest<I, O> addDateHeader(CharSequence name, Date value);

    /**
     * Adds multiple date values for the passed header name to this request. The date values are formatted using netty's
     * {@link HttpHeaders#addDateHeader(HttpMessage, CharSequence, Date)} which formats the date as per the <a
     * href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3.1">HTTP specifications</a> into the format:
     * <p/>
     * <PRE>"E, dd MMM yyyy HH:mm:ss z"</PRE>
     *
     * @param name Name of the header.
     * @param values Values for the header.
     *
     * @return A new instance of the {@link HttpClientRequest} sharing all existing state from this request.
     */
    public abstract HttpClientRequest<I, O> addDateHeader(CharSequence name, Iterable<Date> values);

    /**
     * Adds an HTTP header with the passed {@code name} and {@code values} to this request.
     *
     * @param name Name of the header.
     * @param values Values for the header.
     *
     * @return A new instance of the {@link HttpClientRequest} sharing all existing state from this request.
     */
    public abstract HttpClientRequest<I, O> addHeaderValues(CharSequence name, Iterable<Object> values);

    /**
     * Overwrites the current value, if any, of the passed header to the passed date value for this request. The date is
     * formatted using netty's {@link HttpHeaders#addDateHeader(HttpMessage, CharSequence, Date)} which formats the date
     * as per the <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3.1">HTTP specifications</a> into
     * the format:
     * <p/>
     * <PRE>"E, dd MMM yyyy HH:mm:ss z"</PRE>
     *
     * @param name Name of the header.
     * @param value Value of the header.
     *
     * @return A new instance of the {@link HttpClientRequest} sharing all existing state from this request.
     */
    public abstract HttpClientRequest<I, O> setDateHeader(CharSequence name, Date value);

    /**
     * Overwrites the current value, if any, of the passed header to the passed value for this request.
     *
     * @param name Name of the header.
     * @param value Value of the header.
     *
     * @return A new instance of the {@link HttpClientRequest} sharing all existing state from this request.
     */
    public abstract HttpClientRequest<I, O> setHeader(CharSequence name, Object value);

    /**
     * Overwrites the current value, if any, of the passed header to the passed date values for this request. The date
     * is formatted using netty's {@link HttpHeaders#addDateHeader(HttpMessage, CharSequence, Date)} which formats the
     * date as per the <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3.1">HTTP specifications</a>
     * into the format:
     * <p/>
     * <PRE>"E, dd MMM yyyy HH:mm:ss z"</PRE>
     *
     * @param name Name of the header.
     * @param values Values of the header.
     *
     * @return A new instance of the {@link HttpClientRequest} sharing all existing state from this request.
     */
    public abstract HttpClientRequest<I, O> setDateHeader(CharSequence name, Iterable<Date> values);

    /**
     * Overwrites the current value, if any, of the passed header to the passed values for this request.
     *
     * @param name Name of the header.
     * @param values Values of the header.
     *
     * @return A new instance of the {@link HttpClientRequest} sharing all existing state from this request.
     */
    public abstract HttpClientRequest<I, O> setHeaderValues(CharSequence name, Iterable<Object> values);

    /**
     * Removes the passed header from this request.
     *
     * @param name Name of the header.
     *
     * @return A new instance of the {@link HttpClientRequest} sharing all existing state from this request.
     */
    public abstract HttpClientRequest<I, O> removeHeader(CharSequence name);

    /**
     * Sets HTTP Connection header to the appropriate value for HTTP keep-alive. This delegates to {@link
     * HttpHeaders#setKeepAlive(HttpMessage, boolean)}
     *
     * @param keepAlive {@code true} to enable keep alive.
     *
     * @return A new instance of the {@link HttpClientRequest} sharing all existing state from this request.
     */
    public abstract HttpClientRequest<I, O> setKeepAlive(boolean keepAlive);

    /**
     * Sets the HTTP transfer encoding to chunked for this request. This delegates to {@link
     * HttpHeaders#setTransferEncodingChunked(HttpMessage)}
     *
     * @return A new instance of the {@link HttpClientRequest} sharing all existing state from this request.
     */
    public abstract HttpClientRequest<I, O> setTransferEncodingChunked();

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for all connections created by this request. The
     * specified handler is added at the first position of the pipeline as specified by {@link
     * ChannelPipeline#addFirst(String, ChannelHandler)}
     * <p/>
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be
     * more convenient.</em>
     *
     * @param name Name of the handler.
     * @param handlerFactory Factory to create handler instance to add.
     *
     * @return A new {@link HttpClient} instance.
     */
    public abstract <II, OO> HttpClientRequest<II, OO> addChannelHandlerFirst(String name,
                                                                              Func0<ChannelHandler> handlerFactory);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for all connections created by this request. The
     * specified handler is added at the first position of the pipeline as specified by {@link
     * ChannelPipeline#addFirst(EventExecutorGroup, String, ChannelHandler)}
     * <p/>
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be
     * more convenient.</em>
     *
     * @param group the {@link EventExecutorGroup} which will be used to execute the {@link ChannelHandler} methods
     * @param name the name of the handler to append
     * @param handlerFactory Factory to create handler instance to add.
     *
     * @return A new {@link HttpClientRequest} instance.
     */
    public abstract <II, OO> HttpClientRequest<II, OO> addChannelHandlerFirst(EventExecutorGroup group, String name,
                                                                              Func0<ChannelHandler> handlerFactory);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for all connections created by this request. The
     * specified handler is added at the last position of the pipeline as specified by {@link
     * ChannelPipeline#addLast(String, ChannelHandler)}
     * <p/>
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be
     * more convenient.</em>
     *
     * @param name Name of the handler.
     * @param handlerFactory Factory to create handler instance to add.
     *
     * @return A new {@link HttpClientRequest} instance.
     */
    public abstract <II, OO> HttpClientRequest<II, OO> addChannelHandlerLast(String name,
                                                                             Func0<ChannelHandler> handlerFactory);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for all connections created by this request. The
     * specified handler is added at the last position of the pipeline as specified by {@link
     * ChannelPipeline#addLast(EventExecutorGroup, String, ChannelHandler)}
     * <p/>
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be
     * more convenient.</em>
     *
     * @param group the {@link EventExecutorGroup} which will be used to execute the {@link ChannelHandler} methods
     * @param name the name of the handler to append
     * @param handlerFactory Factory to create handler instance to add.
     *
     * @return A new {@link HttpClientRequest} instance.
     */
    public abstract <II, OO> HttpClientRequest<II, OO> addChannelHandlerLast(EventExecutorGroup group, String name,
                                                                             Func0<ChannelHandler> handlerFactory);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for all connections created by this request. The
     * specified handler is added before an existing handler with the passed {@code baseName} in the pipeline as
     * specified by {@link ChannelPipeline#addBefore(String, String, ChannelHandler)}
     * <p/>
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be
     * more convenient.</em>
     *
     * @param baseName the name of the existing handler
     * @param name Name of the handler.
     * @param handlerFactory Factory to create handler instance to add.
     *
     * @return A new {@link HttpClientRequest} instance.
     */
    public abstract <II, OO> HttpClientRequest<II, OO> addChannelHandlerBefore(String baseName, String name,
                                                                               Func0<ChannelHandler> handlerFactory);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for all connections created by this request. The
     * specified handler is added before an existing handler with the passed {@code baseName} in the pipeline as
     * specified by {@link ChannelPipeline#addBefore(EventExecutorGroup, String, String, ChannelHandler)}
     * <p/>
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be
     * more convenient.</em>
     *
     * @param group the {@link EventExecutorGroup} which will be used to execute the {@link ChannelHandler} methods
     * @param baseName the name of the existing handler
     * @param name the name of the handler to append
     * @param handlerFactory Factory to create handler instance to add.
     *
     * @return A new {@link HttpClientRequest} instance.
     */
    public abstract <II, OO> HttpClientRequest<II, OO> addChannelHandlerBefore(EventExecutorGroup group,
                                                                               String baseName,
                                                                               String name,
                                                                               Func0<ChannelHandler> handlerFactory);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for all connections created by this request. The
     * specified handler is added after an existing handler with the passed {@code baseName} in the pipeline as
     * specified by {@link ChannelPipeline#addAfter(String, String, ChannelHandler)}
     *
     * @param baseName the name of the existing handler
     * @param name Name of the handler.
     * @param handlerFactory Factory to create handler instance to add.
     *
     * @return A new {@link HttpClientRequest} instance.
     */
    public abstract <II, OO> HttpClientRequest<II, OO> addChannelHandlerAfter(String baseName, String name,
                                                                              Func0<ChannelHandler> handlerFactory);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for all connections created by this request. The
     * specified handler is added after an existing handler with the passed {@code baseName} in the pipeline as
     * specified by {@link ChannelPipeline#addAfter(EventExecutorGroup, String, String, ChannelHandler)}
     * <p/>
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be
     * more convenient.</em>
     *
     * @param group the {@link io.netty.util.concurrent.EventExecutorGroup} which will be used to execute the {@link
     * io.netty.channel.ChannelHandler} methods
     * @param baseName the name of the existing handler
     * @param name the name of the handler to append
     * @param handlerFactory Factory to create handler instance to add.
     *
     * @return A new {@link HttpClientRequest} instance.
     */
    public abstract <II, OO> HttpClientRequest<II, OO> addChannelHandlerAfter(EventExecutorGroup group, String baseName,
                                                                              String name,
                                                                              Func0<ChannelHandler> handlerFactory);

    /**
     * Creates a new request instance, inheriting all configurations from this request and using the passed action to
     * configure all the connections created by the newly created request instance.
     *
     * @param configurator Action to configure {@link ChannelPipeline}.
     *
     * @return A new {@link HttpClientRequest} instance.
     */
    public abstract <II, OO> HttpClientRequest<II, OO> pipelineConfigurator(Action1<ChannelPipeline> configurator);

    /**
     * Creates a new request instance, inheriting all configurations from this request and enabling wire logging at the
     * passed level for the newly created request instance.
     *
     * @param wireLoggingLevel Logging level at which the wire logs will be logged. The wire logging will only be done if
     *                        logging is enabled at this level for {@link LoggingHandler}
     *
     * @return A new {@link HttpClientRequest} instance.
     */
    public abstract HttpClientRequest<I, O> enableWireLogging(LogLevel wireLoggingLevel);

    /**
     * Creates a new {@link WebSocketRequest}, inheriting all configurations from this request, that will request an
     * upgrade to websockets from the server.
     *
     * @return A new {@link WebSocketRequest}.
     */
    public abstract WebSocketRequest<O> requestWebSocketUpgrade();

    /**
     * Checks whether a header with the passed name exists for this request.
     *
     * @param name Header name.
     *
     * @return {@code true} if the header exists.
     */
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
    public abstract boolean containsHeaderWithValue(CharSequence name, CharSequence value,
                                                    boolean caseInsensitiveValueMatch);

    /**
     * Fetches the value of a header, if exists, for this request.
     *
     * @param name Name of the header.
     *
     * @return The value of the header, if it exists, {@code null} otherwise. If there are multiple values for this
     * header, the first value is returned.
     */
    public abstract String getHeader(CharSequence name);

    /**
     * Fetches all values of a header, if exists, for this request.
     *
     * @param name Name of the header.
     *
     * @return All values of the header, if it exists, {@code null} otherwise.
     */
    public abstract List<String> getAllHeaders(CharSequence name);

    /**
     * Returns an iterator over the header entries. Multiple values for the same header appear as separate entries in
     * the returned iterator.
     *
     * @return An iterator over the header entries
     */
    public abstract Iterator<Entry<String, String>> headerIterator();

    /**
     * Returns a new {@link Set} that contains the names of all headers in this request.  Note that modifying the
     * returned {@link Set} will not affect the state of this response.
     */
    public abstract Set<String> getHeaderNames();

    /**
     * Returns the HTTP version of this request.
     *
     * @return The HTTP version of this request.
     */
    public abstract HttpVersion getHttpVersion();

    /**
     * Returns the HTTP method for this request.
     *
     * @return The HTTP method for this request.
     */
    public abstract HttpMethod getMethod();

    /**
     * Returns the URI for this request. The returned URI does <em>not</em> contain the scheme, host and port portion of
     * the URI.
     *
     * @return The URI for this request.
     */
    public abstract String getUri();

}

