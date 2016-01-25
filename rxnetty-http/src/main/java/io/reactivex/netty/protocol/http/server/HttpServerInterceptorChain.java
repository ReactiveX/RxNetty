/*
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.netty.protocol.http.server;

import io.netty.buffer.ByteBuf;
import rx.annotations.Beta;

/**
 * A utility to create an interceptor chain to be used with a {@link HttpServer} to modify behavior of requests
 * processed by that server.
 *
 * <h2>What are interceptors?</h2>
 *
 * Interceptors can be used to achieve use-cases that involve instrumentation related to any behavior of the requests,
 * they can even be used to short-circuit the rest of the chain or to provide canned responses. In order to achieve such
 * widely different use-cases, an interceptor is modelled as a simple function that takes one {@link RequestHandler}
 * and returns another {@link RequestHandler} instance. With this low level abstraction, any use-case pertaining to
 * connection instrumentation can be achieved.
 *
 * <h2>Interceptor chain anatomy</h2>
 *
 <PRE>
        -------        -------        -------        -------        -------
       |       |      |       |      |       |      |       |      |       |
       |       | ---> |       | ---> |       | ---> |       | ---> |       |
       |       |      |       |      |       |      |       |      |       |
        -------        -------        -------        -------        -------
      Interceptor    Interceptor    Interceptor    Interceptor      Request
           1              2              3              4           Handler
 </PRE>
 *
 * An interceptor chain always starts with an interceptor and ends with a {@link RequestHandler} and any number of
 * other interceptors can exist between the start and end. <p>
 *
 * A chain can be created by using the various {@code start*()} methods available in this class, eg:
 * {@link #start()}, {@link #startRaw()}.<p>
 *
 * After starting a chain, any number of other interceptors can be added by using the various {@code next*()} methods
 * available in this class, eg: {@link #next(Interceptor)}, {@link #nextWithTransform(TransformingInterceptor)},
 * {@link #nextWithRequestTransform(TransformingInterceptor)} and
 * {@link #nextWithResponseTransform(TransformingInterceptor)}<p>
 *
 * After adding the required interceptors, by providing a {@link RequestHandler} via the
 * {@link #end(RequestHandler)} method, the chain can be ended and the returned {@link RequestHandler} can be used
 * with any {@link HttpServer}<p>
 *
 * So, a typical interaction with this class would look like: <p>
 *
 * {@code
 *    HttpServer.newServer().start(HttpServerInterceptorChain.start(first).next(second).next(third).end(handler))
 * }
 *
 * <h2>Simple Interceptor</h2>
 *
 * For interceptors that do not change the types of objects read or written to the underlying request/response, the
 * interface {@link HttpServerInterceptorChain.Interceptor} defines the interceptor contract.
 *
 * <h2>Modifying the type of data read/written to the request/response</h2>
 *
 * Sometimes, it is required to change the type of objects read from a request or written to a response handled by
 * a {@link HttpServer}. For such cases, the interface {@link HttpServerInterceptorChain.TransformingInterceptor}
 * defines the interceptor contract. Since, this included 4 generic arguments to the interceptor, this is not the base
 * type for all interceptors and should be used only when the types of the request/response are actually to be
 * changed.
 *
 * <h2>Execution order</h2>
 *
 <PRE>
      -------        -------         -------        -------        -------        -------        -------
     |       |      |       |       |       |      |       |      |       |      |       |      |       |
     |       | ---> |       | --->  |       | ---> |       | ---> |       | ---> |       | ---> |       |
     |       |      |       |       |       |      |       |      |       |      |       |      |       |
      -------        -------         -------        -------        -------        -------        -------
       Http          Request      Interceptor    Interceptor    Interceptor    Interceptor        Request
      Server         Handler           1              2              3              4             Handler
                   (Internal)                                                                     (User)
 </PRE>
 *
 * The above diagram depicts the execution order of interceptors. The first request handler (internal) is created by
 * this class and as is returned by {@link #end(RequestHandler)} method by providing a {@link RequestHandler} that
 * does the actual processing of the connection. {@link HttpServer} with which this interceptor chain is used, will
 * invoke the internal request handler provided by this class. <p>
 *
 * The interceptors are invoked in the order that they are added to this chain.
 *
 * @param <I> The type of objects received as content from a request to this server.
 * @param <O> The type of objects written as content from a response from this server.
 * @param <II> The type of objects received as content from a request to this server after applying these interceptors.
 * @param <OO> The type of objects written as content from a response from this server after applying these
 * interceptors.
 */
@Beta
public final class HttpServerInterceptorChain<I, O, II, OO> {

    private final TransformingInterceptor<I, O, II, OO> interceptor;

    private HttpServerInterceptorChain(TransformingInterceptor<I, O, II, OO> interceptor) {
        this.interceptor = interceptor;
    }

    /**
     * Add the next interceptor to this chain.
     *
     * @param next Next interceptor to add.
     *
     * @return A new interceptor chain with the interceptors currently existing and the passed interceptor added to the
     * end.
     */
    public HttpServerInterceptorChain<I, O, II, OO> next(final Interceptor<II, OO> next) {
        return new HttpServerInterceptorChain<>(new TransformingInterceptor<I, O, II, OO>() {
            @Override
            public RequestHandler<I, O> intercept(RequestHandler<II, OO> handler) {
                return interceptor.intercept(next.intercept(handler));
            }
        });
    }

    /**
     * Add the next interceptor to this chain, which changes the type of objects read from the request accepted by
     * the associated {@link HttpServer}.
     *
     * @param next Next interceptor to add.
     *
     * @return A new interceptor chain with the interceptors currently existing and the passed interceptor added to the
     * end.
     */
    public <III> HttpServerInterceptorChain<I, O, III, OO> nextWithRequestTransform(final TransformingInterceptor<II, OO, III, OO> next) {
        return new HttpServerInterceptorChain<>(new TransformingInterceptor<I, O, III, OO>() {
            @Override
            public RequestHandler<I, O> intercept(RequestHandler<III, OO> handler) {
                return interceptor.intercept(next.intercept(handler));
            }
        });
    }

    /**
     * Add the next interceptor to this chain, which changes the type of objects written to the response sent by
     * the associated {@link HttpServer}.
     *
     * @param next Next interceptor to add.
     *
     * @return A new interceptor chain with the interceptors current existing and the passed interceptor added to the
     * end.
     */
    public <OOO> HttpServerInterceptorChain<I, O, II, OOO> nextWithResponseTransform(final TransformingInterceptor<II, OO, II, OOO> next) {
        return new HttpServerInterceptorChain<>(new TransformingInterceptor<I, O, II, OOO>() {
            @Override
            public RequestHandler<I, O> intercept(RequestHandler<II, OOO> handler) {
                return interceptor.intercept(next.intercept(handler));
            }
        });
    }

    /**
     * Add the next interceptor to this chain, which changes the type of objects read read from the request and written
     * to the response sent by the associated {@link HttpServer}.
     *
     * @param next Next interceptor to add.
     *
     * @return A new interceptor chain with the interceptors current existing and the passed interceptor added to the
     * end.
     */
    public <RRR, WWW> HttpServerInterceptorChain<I, O, RRR, WWW> nextWithTransform(final TransformingInterceptor<II, OO, RRR, WWW> next) {
        return new HttpServerInterceptorChain<>(new TransformingInterceptor<I, O, RRR, WWW>() {
            @Override
            public RequestHandler<I, O> intercept(RequestHandler<RRR, WWW> handler) {
                return interceptor.intercept(next.intercept(handler));
            }
        });
    }

    /**
     * Terminates this chain with the passed {@link RequestHandler} and returns a {@link RequestHandler} to be
     * used by a {@link HttpServer}
     *
     * @param handler Request handler to use.
     *
     * @return A request handler that wires the interceptor chain, to be used with {@link HttpServer} instead of
     * directly using the passed {@code handler}
     */
    public RequestHandler<I, O> end(RequestHandler<II, OO> handler) {
        return interceptor.intercept(handler);
    }

    /**
     * Starts a new interceptor chain.
     *
     * @return A new interceptor chain.
     */
    public static <R, W> HttpServerInterceptorChain<R, W, R, W> start() {
        return new HttpServerInterceptorChain<>(new TransformingInterceptor<R, W, R, W>() {
            @Override
            public RequestHandler<R, W> intercept(RequestHandler<R, W> handler) {
                return handler;
            }
        });
    }

    /**
     * Starts a new interceptor chain with {@link ByteBuf} read and written from request and to responses.
     *
     * @return A new interceptor chain.
     */
    public static HttpServerInterceptorChain<ByteBuf, ByteBuf, ByteBuf, ByteBuf> startRaw() {
        return new HttpServerInterceptorChain<>(new TransformingInterceptor<ByteBuf, ByteBuf, ByteBuf, ByteBuf>() {
            @Override
            public RequestHandler<ByteBuf, ByteBuf> intercept(RequestHandler<ByteBuf, ByteBuf> handler) {
                return handler;
            }
        });
    }

    /**
     * An interceptor that preserves the type of content of request and response.
     *
     * @param <I> The type of objects received as content from a request to this server.
     * @param <O> The type of objects written as content from a response from this server.
     */
    public interface Interceptor<I, O> {

        /**
         * Intercepts and optionally changes the passed {@code RequestHandler}.
         *
         * @param handler Handler to intercept.
         *
         * @return Handler to use after this transformation.
         */
        RequestHandler<I, O> intercept(RequestHandler<I, O> handler);

    }

    /**
     * An interceptor that changes the type of content of request and response.
     *
     * @param <I> The type of objects received as content from a request to this server.
     * @param <O> The type of objects written as content from a response from this server.
     * @param <II> The type of objects received as content from a request to this server after applying this
     * interceptor.
     * @param <OO> The type of objects written as content from a response from this server after applying this
     * interceptor.
     */
    public interface TransformingInterceptor<I, O, II, OO> {

        /**
         * Intercepts and changes the passed {@code RequestHandler}.
         *
         * @param handler Handler to intercept.
         *
         * @return Handler to use after this transformation.
         */
        RequestHandler<I, O> intercept(RequestHandler<II, OO> handler);

    }
}
