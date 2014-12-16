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
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.EventExecutorGroup;
import io.reactivex.netty.client.PoolLimitDeterminationStrategy;
import io.reactivex.netty.pipeline.ssl.SSLEngineFactory;
import rx.functions.Action1;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * An HTTP client for executing HTTP requests.
 *
 * <h2>Immutability</h2>
 * An instance of this client is immutable and all mutations produce a new client instance. For this reason it is
 * recommended that the mutations are done during client creation and not during request processing to avoid repeated
 * object creation overhead.
 *
 * @param <I> The type of the content of request.
 * @param <O> The type of the content of response.
 */
public abstract class HttpClient<I, O> {

    /**
     * Creates a GET request for the passed URI.
     *
     * @param uri The URI for the request. The URI can be relative or absolute. If the URI is relative
     *            (missing host and port information), the target host and port are inferred from the {@link HttpClient}
     *            that created the request. If the URI is absolute, the host and port are used from the URI.
     *
     * @return New {@link HttpClientRequest}.
     */
    public abstract HttpClientRequest<I, O> createGet(String uri);

    /**
     * Creates a POST request for the passed URI.
     *
     * @param uri The URI for the request. The URI can be relative or absolute. If the URI is relative
     *            (missing host and port information), the target host and port are inferred from the {@link HttpClient}
     *            that created the request. If the URI is absolute, the host and port are used from the URI.
     *
     * @return New {@link HttpClientRequest}.
     */
    public abstract HttpClientRequest<I, O> createPost(String uri);

    /**
     * Creates a PUT request for the passed URI.
     *
     * @param uri The URI for the request. The URI can be relative or absolute. If the URI is relative
     *            (missing host and port information), the target host and port are inferred from the {@link HttpClient}
     *            that created the request. If the URI is absolute, the host and port are used from the URI.
     *
     * @return New {@link HttpClientRequest}.
     */
    public abstract HttpClientRequest<I, O> createPut(String uri);

    /**
     * Creates a DELETE request for the passed URI.
     *
     * @param uri The URI for the request. The URI can be relative or absolute. If the URI is relative
     *            (missing host and port information), the target host and port are inferred from the {@link HttpClient}
     *            that created the request. If the URI is absolute, the host and port are used from the URI.
     *
     * @return New {@link HttpClientRequest}.
     */
    public abstract HttpClientRequest<I, O> createDelete(String uri);

    /**
     * Creates a HEAD request for the passed URI.
     *
     * @param uri The URI for the request. The URI can be relative or absolute. If the URI is relative
     *            (missing host and port information), the target host and port are inferred from the {@link HttpClient}
     *            that created the request. If the URI is absolute, the host and port are used from the URI.
     *
     * @return New {@link HttpClientRequest}.
     */
    public abstract HttpClientRequest<I, O> createHead(String uri);

    /**
     * Creates an OPTIONS request for the passed URI.
     *
     * @param uri The URI for the request. The URI can be relative or absolute. If the URI is relative
     *            (missing host and port information), the target host and port are inferred from the {@link HttpClient}
     *            that created the request. If the URI is absolute, the host and port are used from the URI.
     *
     * @return New {@link HttpClientRequest}.
     */
    public abstract HttpClientRequest<I, O> createOptions(String uri);

    /**
     * Creates a PATCH request for the passed URI.
     *
     * @param uri The URI for the request. The URI can be relative or absolute. If the URI is relative
     *            (missing host and port information), the target host and port are inferred from the {@link HttpClient}
     *            that created the request. If the URI is absolute, the host and port are used from the URI.
     *
     * @return New {@link HttpClientRequest}.
     */
    public abstract HttpClientRequest<I, O> createPatch(String uri);

    /**
     * Creates a TRACE request for the passed URI.
     *
     * @param uri The URI for the request. The URI can be relative or absolute. If the URI is relative
     *            (missing host and port information), the target host and port are inferred from the {@link HttpClient}
     *            that created the request. If the URI is absolute, the host and port are used from the URI.
     *
     * @return New {@link HttpClientRequest}.
     */
    public abstract HttpClientRequest<I, O> createTrace(String uri);

    /**
     * Creates a CONNECT request for the passed URI.
     *
     * @param uri The URI for the request. The URI can be relative or absolute. If the URI is relative
     *            (missing host and port information), the target host and port are inferred from the {@link HttpClient}
     *            that created the request. If the URI is absolute, the host and port are used from the URI.
     *
     * @return New {@link HttpClientRequest}.
     */
    public abstract HttpClientRequest<I, O> createConnect(String uri);

    /**
     * Creates a new client instances, inheriting all configurations from this client and adding the passed read timeout
     * for all requests created by the newly created client instance.
     *
     * @param timeOut Read timeout duration.
     * @param timeUnit Timeunit for the timeout.
     *
     * @return A new {@link HttpClient} instance.
     */
    public abstract HttpClient<I, O> readTimeOut(int timeOut, TimeUnit timeUnit);

    /**
     * Creates a new client instances, inheriting all configurations from this client and following the passed number of
     * max HTTP redirects.
     *
     * @param maxRedirects Maximum number of redirects to follow for any request.
     *
     * @return A new {@link HttpClient} instance.
     */
    public abstract HttpClient<I, O> followRedirects(int maxRedirects);

    /**
     * Creates a new client instances, inheriting all configurations from this client and enabling/disabling redirects
     * for all requests created by the newly created client instance.
     *
     * @param follow {@code true} to follow redirects. {@code false} to disable any redirects.
     *
     * @return A new {@link HttpClient} instance.
     */
    public abstract HttpClient<I, O> followRedirects(boolean follow);

    /**
     * Creates a new client instances, inheriting all configurations from this client and adding a
     * {@link ChannelOption} for the connections created by the newly created client instance.
     *
     * @param option Option to add.
     * @param value Value for the option.
     *
     * @return A new {@link HttpClient} instance.
     */
    public abstract <T> HttpClient<I, O> channelOption(ChannelOption<T> option, T value);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for all connections created by this client. The specified
     * handler is added at the first position of the pipeline as specified by
     * {@link ChannelPipeline#addFirst(String, ChannelHandler)}
     *
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be more
     * convenient.</em>
     *
     * @param name Name of the handler.
     * @param handler Handler instance to add.
     *
     * @return A new {@link HttpClient} instance.
     */
    public abstract <II, OO> HttpClient<II, OO> addChannelHandlerFirst(String name, ChannelHandler handler);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for all connections created by this client. The specified
     * handler is added at the first position of the pipeline as specified by
     * {@link ChannelPipeline#addFirst(EventExecutorGroup, String, ChannelHandler)}
     *
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be more
     * convenient.</em>
     *
     * @param group   the {@link EventExecutorGroup} which will be used to execute the {@link ChannelHandler}
     *                 methods
     * @param name     the name of the handler to append
     * @param handler  the handler to append
     *
     * @return A new {@link HttpClient} instance.
     */
    public abstract <II, OO> HttpClient<II, OO> addChannelHandlerFirst(EventExecutorGroup group, String name,
                                                                       ChannelHandler handler);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for all connections created by this client. The specified
     * handler is added at the last position of the pipeline as specified by
     * {@link ChannelPipeline#addLast(String, ChannelHandler)}
     *
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be more
     * convenient.</em>
     *
     * @param name Name of the handler.
     * @param handler Handler instance to add.
     *
     * @return A new {@link HttpClient} instance.
     */
    public abstract <II, OO> HttpClient<II, OO>  addChannelHandlerLast(String name, ChannelHandler handler);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for all connections created by this client. The specified
     * handler is added at the last position of the pipeline as specified by
     * {@link ChannelPipeline#addLast(EventExecutorGroup, String, ChannelHandler)}
     *
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be more
     * convenient.</em>
     *
     * @param group   the {@link EventExecutorGroup} which will be used to execute the {@link ChannelHandler}
     *                 methods
     * @param name     the name of the handler to append
     * @param handler  the handler to append
     *
     * @return A new {@link HttpClient} instance.
     */
    public abstract <II, OO> HttpClient<II, OO> addChannelHandlerLast(EventExecutorGroup group, String name,
                                                                      ChannelHandler handler);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for all connections created by this client. The specified
     * handler is added before an existing handler with the passed {@code baseName} in the pipeline as specified by
     * {@link ChannelPipeline#addBefore(String, String, ChannelHandler)}
     *
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be more
     * convenient.</em>
     *
     * @param baseName  the name of the existing handler
     * @param name Name of the handler.
     * @param handler Handler instance to add.
     *
     * @return A new {@link HttpClient} instance.
     */
    public abstract <II, OO> HttpClient<II, OO> addChannelHandlerBefore(String baseName, String name,
                                                                        ChannelHandler handler);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for all connections created by this client. The specified
     * handler is added before an existing handler with the passed {@code baseName} in the pipeline as specified by
     * {@link ChannelPipeline#addBefore(EventExecutorGroup, String, String, ChannelHandler)}
     *
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be more
     * convenient.</em>
     *
     * @param group   the {@link EventExecutorGroup} which will be used to execute the {@link ChannelHandler}
     *                 methods
     * @param baseName  the name of the existing handler
     * @param name     the name of the handler to append
     * @param handler  the handler to append
     *
     * @return A new {@link HttpClient} instance.
     */
    public abstract <II, OO> HttpClient<II, OO> addChannelHandlerBefore(EventExecutorGroup group, String baseName,
                                                                        String name, ChannelHandler handler);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for all connections created by this client. The specified
     * handler is added after an existing handler with the passed {@code baseName} in the pipeline as specified by
     * {@link ChannelPipeline#addAfter(String, String, ChannelHandler)}
     *
     * @param baseName  the name of the existing handler
     * @param name Name of the handler.
     * @param handler Handler instance to add.
     *
     * @return A new {@link HttpClient} instance.
     */
    public abstract <II, OO> HttpClient<II, OO>  addChannelHandlerAfter(String baseName, String name,
                                                                               ChannelHandler handler);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for all connections created by this client. The specified
     * handler is added after an existing handler with the passed {@code baseName} in the pipeline as specified by
     * {@link ChannelPipeline#addAfter(EventExecutorGroup, String, String, ChannelHandler)}
     *
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be more
     * convenient.</em>
     *
     * @param group   the {@link EventExecutorGroup} which will be used to execute the {@link ChannelHandler}
     *                 methods
     * @param baseName  the name of the existing handler
     * @param name     the name of the handler to append
     * @param handler  the handler to append
     *
     * @return A new {@link HttpClient} instance.
     */
    public abstract <II, OO> HttpClient<II, OO> addChannelHandlerAfter(EventExecutorGroup group, String baseName,
                                                                       String name, ChannelHandler handler);

    /**
     * Removes the {@link ChannelHandler} with the passed {@code name} from the {@link ChannelPipeline} for all
     * connections created by this client.
     *
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be more
     * convenient.</em>
     *
     * @param name Name of the handler.
     *
     * @return A new {@link HttpClient} instance.
     */
    public abstract <II, OO> HttpClient<II, OO> removeHandler(String name);

    /**
     * Creates a new client instances, inheriting all configurations from this client and using the passed
     * action to configure all the connections created by the newly created client instance.
     *
     * @param pipelineConfigurator Action to configure {@link ChannelPipeline}.
     *
     * @return A new {@link HttpClient} instance.
     */
    public abstract <II, OO> HttpClient<II, OO> pipelineConfigurator(Action1<ChannelPipeline> pipelineConfigurator);

    /**
     * Creates a new client instances, inheriting all configurations from this client and using the passed
     * eventLoopGroup for all the connections created by the newly created client instance.
     *
     * @param eventLoopGroup {@link EventLoopGroup} to use.
     *
     * @return A new {@link HttpClient} instance.
     */
    public abstract HttpClient<I, O> eventLoop(EventLoopGroup eventLoopGroup);

    /**
     * Creates a new client instances, inheriting all configurations from this client and using the passed
     * {@code maxConnections} as the maximum number of concurrent connections created by the newly created client instance.
     *
     * @param maxConnections Maximum number of concurrent connections to be created by this client.
     *
     * @return A new {@link HttpClient} instance.
     */
    public abstract HttpClient<I, O> maxConnections(int maxConnections);

    /**
     * Creates a new client instances, inheriting all configurations from this client and using the passed
     * {@code idleConnectionsTimeoutMillis} as the time elapsed before an idle connections will be closed by the newly
     * created client instance.
     *
     * @param idleConnectionsTimeoutMillis Time elapsed before an idle connections will be closed by the newly
     * created client instance
     *
     * @return A new {@link HttpClient} instance.
     */
    public abstract HttpClient<I, O> withIdleConnectionsTimeoutMillis(long idleConnectionsTimeoutMillis);

    /**
     * Creates a new client instances, inheriting all configurations from this client and using the passed
     * {@code limitDeterminationStrategy} as the strategy to control the maximum concurrent connections created by the
     * newly created client instance.
     *
     * @param limitDeterminationStrategy Strategy to control the maximum concurrent connections created by the
     * newly created client instance.
     *
     * @return A new {@link HttpClient} instance.
     */
    public abstract HttpClient<I, O> withConnectionPoolLimitStrategy(PoolLimitDeterminationStrategy limitDeterminationStrategy);

    /**
     * Creates a new client instances, inheriting all configurations from this client and using the passed
     * {@code poolIdleCleanupScheduler} for detecting and cleaning idle connections by the newly created client instance.
     *
     * @param poolIdleCleanupScheduler Scheduled to schedule idle connections cleanup.
     *
     * @return A new {@link HttpClient} instance.
     */
    public abstract HttpClient<I, O> withPoolIdleCleanupScheduler(ScheduledExecutorService poolIdleCleanupScheduler);

    /**
     * Creates a new client instances, inheriting all configurations from this client and disabling idle connection
     * cleanup for the newly created client instance.
     *
     * @return A new {@link HttpClient} instance.
     */
    public abstract HttpClient<I, O> withNoIdleConnectionCleanup();

    /**
     * Creates a new client instances, inheriting all configurations from this client and disabling connection
     * pooling for the newly created client instance.
     *
     * @return A new {@link HttpClient} instance.
     */
    public abstract HttpClient<I, O> withNoConnectionPooling();

    /**
     * Creates a new client instances, inheriting all configurations from this client and enabling wire logging at the
     * passed level for the newly created client instance.
     *
     * @param wireLogginLevel Logging level at which the wire logs will be logged. The wire logging will only be done if
     *                        logging is enabled at this level for {@link LoggingHandler}
     *
     * @return A new {@link HttpClient} instance.
     */
    public abstract HttpClient<I, O> enableWireLogging(LogLevel wireLogginLevel);

    /**
     * Creates a new client instances, inheriting all configurations from this client and using the passed
     * {@code sslEngineFactory} for all secured connections created by the newly created client instance.
     *
     * @param sslEngineFactory {@link SSLEngineFactory} for all secured connections created by the newly created client
     *                                                 instance.
     *
     * @return A new {@link HttpClient} instance.
     */
    public abstract HttpClient<I, O> withSslEngineFactory(SSLEngineFactory sslEngineFactory);
}
