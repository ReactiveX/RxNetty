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

package io.reactivex.netty.protocol.tcp.server;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.channel.AbstractDelegatingConnection;
import io.reactivex.netty.channel.Connection;
import rx.annotations.Beta;
import rx.functions.Func1;

/**
 * A utility to create an interceptor chain to be used with a {@link TcpServer} to modify behavior of connections
 * accepted by that server.
 *
 * <h2>What are interceptors?</h2>
 *
 * Interceptors can be used to achieve use-cases that involve instrumentation related to any behavior of the connection,
 * they can even be used to short-circuit the rest of the chain or to provide canned responses. In order to achieve such
 * widely different use-cases, an interceptor is modelled as a simple function that takes one {@link ConnectionHandler}
 * and returns another {@link ConnectionHandler} instance. With this low level abstraction, any use-case pertaining to
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
      Interceptor    Interceptor    Interceptor    Interceptor     Connection
           1              2              3              4           Handler
 </PRE>
 *
 * An interceptor chain always starts with an interceptor and ends with a {@link ConnectionHandler} and any number of
 * other interceptors can exist between the start and end. <p>
 *
 * A chain can be created by using the various {@code start*()} methods available in this class, eg:
 * {@link #start(Interceptor)}, {@link #startWithTransform(TransformingInterceptor)},
 * {@link #startWithReadTransform(TransformingInterceptor)}, {@link #startWithWriteTransform(TransformingInterceptor)}.<p>
 *
 * After starting a chain, any number of other interceptors can be added by using the various {@code next*()} methods
 * available in this class, eg: {@link #next(Interceptor)}, {@link #nextWithTransform(TransformingInterceptor)},
 * {@link #nextWithReadTransform(TransformingInterceptor)} and {@link #nextWithWriteTransform(TransformingInterceptor)}<p>
 *
 * After adding the required interceptors, by providing a {@link ConnectionHandler} via the
 * {@link #end(ConnectionHandler)} method, the chain can be ended and the returned {@link ConnectionHandler} can be used
 * with any {@link TcpServer}<p>
 *
 * So, a typical interaction with this class would look like: <p>
 *
 * {@code
 *    TcpServer.newServer().start(TcpServerInterceptorChain.start(first).next(second).next(third).end(handler))
 * }
 *
 * <h2>Simple Interceptor</h2>
 *
 * For interceptors that do not change the types of objects read or written to the underlying connection, the interface
 * {@link TcpServerInterceptorChain.Interceptor} defines the interceptor contract.
 *
 * <h2>Modifying the type of data read/written to the {@link Connection}</h2>
 *
 * Sometimes, it is required to change the type of objects read or written to a {@link Connection} instance handled by
 * a {@link TcpServer}. For such cases, the interface {@link TcpServerInterceptorChain.TransformingInterceptor} defines
 * the interceptor contract. Since, this included 4 generic arguments to the interceptor, this is not the base type for
 * all interceptors and should be used only when the types of the {@link Connection} are actually to be changed.
 *
 * <h2>Execution order</h2>
 *
 <PRE>
      -------        -------         -------        -------        -------        -------        -------
     |       |      |       |       |       |      |       |      |       |      |       |      |       |
     |       | ---> |       | --->  |       | ---> |       | ---> |       | ---> |       | ---> |       |
     |       |      |       |       |       |      |       |      |       |      |       |      |       |
      -------        -------         -------        -------        -------        -------        -------
       Tcp         Connection      Interceptor    Interceptor    Interceptor    Interceptor     Connection
      Server         Handler           1              2              3              4             Handler
                   (Internal)                                                                     (User)
 </PRE>
 *
 * The above diagram depicts the execution order of interceptors. The first connection handler (internal) is created by
 * this class and as is returned by {@link #end(ConnectionHandler)} method by providing a {@link ConnectionHandler} that
 * does the actual processing of the connection. {@link TcpServer} with which this interceptor chain is used, will
 * invoke the internal connection handler provided by this class. <p>
 *
 * The interceptors are invoked in the order that they are added to this chain.
 *
 * @param <R> The type of objects read from a connection to {@link TcpServer} with which this interceptor chain will be
 * used.
 * @param <W> The type of objects written to a connection to {@link TcpServer} with which this interceptor chain will be
 * used.
 * @param <RR> The type of objects read from a connection to {@link TcpServer} after applying this interceptor chain.
 * @param <WW> The type of objects written to a connection to {@link TcpServer} after applying this interceptor chain.
 *
 * @see AbstractDelegatingConnection
 */
@Beta
public final class TcpServerInterceptorChain<R, W, RR, WW> {

    private final TransformingInterceptor<R, W, RR, WW> interceptor;

    private TcpServerInterceptorChain(TransformingInterceptor<R, W, RR, WW> interceptor) {
        this.interceptor = interceptor;
    }

    /**
     * Add the next interceptor to this chain.
     *
     * @param next Next interceptor to add.
     *
     * @return A new interceptor chain with the interceptors current existing and the passed interceptor added to the
     * end.
     */
    public TcpServerInterceptorChain<R, W, RR, WW> next(final Interceptor<RR, WW> next) {
        return new TcpServerInterceptorChain<>(new TransformingInterceptor<R, W, RR, WW>() {
            @Override
            public ConnectionHandler<R, W> call(ConnectionHandler<RR, WW> handler) {
                return interceptor.call(next.call(handler));
            }
        });
    }

    /**
     * Add the next interceptor to this chain, which changes the type of objects read from the connections processed by
     * the associated {@link TcpServer}.
     *
     * @param next Next interceptor to add.
     *
     * @return A new interceptor chain with the interceptors current existing and the passed interceptor added to the
     * end.
     */
    public <RRR> TcpServerInterceptorChain<R, W, RRR, WW> nextWithReadTransform(final TransformingInterceptor<RR, WW, RRR, WW> next) {
        return new TcpServerInterceptorChain<>(new TransformingInterceptor<R, W, RRR, WW>() {
            @Override
            public ConnectionHandler<R, W> call(ConnectionHandler<RRR, WW> handler) {
                return interceptor.call(next.call(handler));
            }
        });
    }

    /**
     * Add the next interceptor to this chain, which changes the type of objects written to the connections processed by
     * the associated {@link TcpServer}.
     *
     * @param next Next interceptor to add.
     *
     * @return A new interceptor chain with the interceptors current existing and the passed interceptor added to the
     * end.
     */
    public <WWW> TcpServerInterceptorChain<R, W, RR, WWW> nextWithWriteTransform(final TransformingInterceptor<RR, WW, RR, WWW> next) {
        return new TcpServerInterceptorChain<>(new TransformingInterceptor<R, W, RR, WWW>() {
            @Override
            public ConnectionHandler<R, W> call(ConnectionHandler<RR, WWW> handler) {
                return interceptor.call(next.call(handler));
            }
        });
    }

    /**
     * Add the next interceptor to this chain, which changes the type of objects read and written from/to the
     * connections processed by the associated {@link TcpServer}.
     *
     * @param next Next interceptor to add.
     *
     * @return A new interceptor chain with the interceptors current existing and the passed interceptor added to the
     * end.
     */
    public <RRR, WWW> TcpServerInterceptorChain<R, W, RRR, WWW> nextWithTransform(final TransformingInterceptor<RR, WW, RRR, WWW> next) {
        return new TcpServerInterceptorChain<>(new TransformingInterceptor<R, W, RRR, WWW>() {
            @Override
            public ConnectionHandler<R, W> call(ConnectionHandler<RRR, WWW> handler) {
                return interceptor.call(next.call(handler));
            }
        });
    }

    /**
     * Terminates this chain with the passed {@link ConnectionHandler} and returns a {@link ConnectionHandler} to be
     * used by a {@link TcpServer}
     *
     * @param handler Connection handler to use.
     *
     * @return A connection handler that wires the interceptor chain, to be used with {@link TcpServer} instead of
     * directly using the passed {@code handler}
     */
    public ConnectionHandler<R, W> end(ConnectionHandler<RR, WW> handler) {
        return interceptor.call(handler);
    }

    /**
     * One of the methods to start creating the interceptor chain. The other start methods can be used for starting with
     * interceptors that modify the type of Objects read/written from/to the connections processed by the associated
     * {@link TcpServer}.
     *
     * @param start The starting interceptor for this chain.
     *
     * @param <R> The type of objects read from a connection to {@link TcpServer} with which this interceptor chain will
     * be used.
     * @param <W> The type of objects written to a connection to {@link TcpServer} with which this interceptor chain
     * will be used.
     *
     * @return A new interceptor chain.
     */
    public static <R, W> TcpServerInterceptorChain<R, W, R, W> start(final Interceptor<R, W> start) {
        return new TcpServerInterceptorChain<>(new TransformingInterceptor<R, W, R, W>() {
            @Override
            public ConnectionHandler<R, W> call(ConnectionHandler<R, W> handler) {
                return start.call(handler);
            }
        });
    }

    /**
     * One of the methods to start creating the interceptor chain. The other start methods can be used for starting with
     * interceptors that modify the type of Objects read/written from/to the connections processed by the associated
     * {@link TcpServer}.
     *
     * @param start The starting interceptor for this chain.
     *
     * @return A new interceptor chain.
     */
    public static TcpServerInterceptorChain<ByteBuf, ByteBuf, ByteBuf, ByteBuf> startRaw(final Interceptor<ByteBuf, ByteBuf> start) {
        return new TcpServerInterceptorChain<>(new TransformingInterceptor<ByteBuf, ByteBuf, ByteBuf, ByteBuf>() {
            @Override
            public ConnectionHandler<ByteBuf, ByteBuf> call(ConnectionHandler<ByteBuf, ByteBuf> handler) {
                return start.call(handler);
            }
        });
    }

    /**
     * One of the methods to start creating the interceptor chain for converting the type of objects read from the
     * connections processed by the associated {@link TcpServer}. For converting the type of objects written, use
     * {@link #startWithWriteTransform(TransformingInterceptor)} and for converting both read and write types, use
     * {@link #startWithTransform(TransformingInterceptor)}
     *
     * @param start The starting interceptor for this chain.
     *
     * @param <R> The type of objects read from a connection to {@link TcpServer} before applying this interceptor
     * chain.
     * @param <W> The type of objects written to a connection to {@link TcpServer} with which this interceptor chain
     * will be used.
     * @param <RR> The type of objects read from a connection to {@link TcpServer} after applying this interceptor
     * chain.
     *
     * @return A new interceptor chain.
     */
    public static <R, W, RR> TcpServerInterceptorChain<R, W, RR, W> startWithReadTransform(TransformingInterceptor<R, W, RR, W> start) {
        return new TcpServerInterceptorChain<>(start);
    }

    /**
     * One of the methods to start creating the interceptor chain for converting the type of objects written to the
     * connections processed by the associated {@link TcpServer}. For converting the type of objects read, use
     * {@link #startWithReadTransform(TransformingInterceptor)} and for converting both read and write types, use
     * {@link #startWithTransform(TransformingInterceptor)}
     *
     * @param start The starting interceptor for this chain.
     *
     * @param <R> The type of objects read from a connection to {@link TcpServer} with which this interceptor chain
     * will be used.
     * @param <W> The type of objects written to a connection to {@link TcpServer} before applying this interceptor
     * chain.
     * @param <WW> The type of objects written to a connection to {@link TcpServer} after applying this interceptor
     * chain.
     *
     * @return A new interceptor chain.
     */
    public static <R, W, WW> TcpServerInterceptorChain<R, W, R, WW> startWithWriteTransform(TransformingInterceptor<R, W, R, WW> start) {
        return new TcpServerInterceptorChain<>(start);
    }

    /**
     * One of the methods to start creating the interceptor chain for converting the type of objects read and written
     * from/to the connections processed by the associated {@link TcpServer}. For converting the type of objects read,
     * use {@link #startWithReadTransform(TransformingInterceptor)} and for converting type of objects written, use
     * {@link #startWithWriteTransform(TransformingInterceptor)}
     *
     * @param start The starting interceptor for this chain.
     *
     * @param <R> The type of objects read from a connection to {@link TcpServer} before applying this interceptor
     * chain.
     * @param <W> The type of objects written to a connection to {@link TcpServer} with which this interceptor chain
     * will be used.
     * @param <RR> The type of objects read from a connection to {@link TcpServer} after applying this interceptor
     * chain.
     * @param <WW> The type of objects written to a connection to {@link TcpServer} after applying this interceptor
     * chain.
     *
     * @return A new interceptor chain.
     */
    public static <R, W, RR, WW> TcpServerInterceptorChain<R, W, RR, WW> startWithTransform(TransformingInterceptor<R, W, RR, WW> start) {
        return new TcpServerInterceptorChain<>(start);
    }

    /**
     * An interceptor that preserves the type of objects read and written to the connection.
     *
     * @param <R> Type of objects read from the connection handled by this interceptor.
     * @param <W> Type of objects written to the connection handled by this interceptor.
     */
    public interface Interceptor<R, W> extends Func1<ConnectionHandler<R, W>, ConnectionHandler<R, W>> {

    }

    /**
     * An interceptor that changes the type of objects read and written to the connection.
     *
     * @param <R> Type of objects read from the connection before applying this interceptor.
     * @param <W> Type of objects written to the connection before applying this interceptor.
     * @param <RR> Type of objects read from the connection after applying this interceptor.
     * @param <WW> Type of objects written to the connection after applying this interceptor.
     */
    public interface TransformingInterceptor<R, W, RR, WW>
            extends Func1<ConnectionHandler<RR, WW>, ConnectionHandler<R, W>> {

    }
}
