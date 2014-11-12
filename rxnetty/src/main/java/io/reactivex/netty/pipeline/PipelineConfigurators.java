/*
 * Copyright 2014 Netflix, Inc.
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
package io.reactivex.netty.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.client.ClientRequiredConfigurator;
import io.reactivex.netty.client.RxClient;
import io.reactivex.netty.metrics.MetricEventsSubject;
import io.reactivex.netty.pipeline.ssl.SSLEngineFactory;
import io.reactivex.netty.pipeline.ssl.SslPipelineConfigurator;
import io.reactivex.netty.protocol.http.HttpObjectAggregationConfigurator;
import io.reactivex.netty.protocol.http.client.HttpClientPipelineConfigurator;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.http.server.HttpServerPipelineConfigurator;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.sse.ServerSentEvent;
import io.reactivex.netty.protocol.http.sse.SseClientPipelineConfigurator;
import io.reactivex.netty.protocol.http.sse.SseOverHttpClientPipelineConfigurator;
import io.reactivex.netty.protocol.http.sse.SseOverHttpServerPipelineConfigurator;
import io.reactivex.netty.protocol.http.sse.SseServerPipelineConfigurator;
import io.reactivex.netty.protocol.text.SimpleTextProtocolConfigurator;

import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

/**
 * Utility class that provides a variety of {@link PipelineConfigurator} implementations
 */
public final class PipelineConfigurators {

    private static final PipelineConfigurator<ByteBuf,ByteBuf> EMPTY_CONFIGURATOR =
            new PipelineConfigurator<ByteBuf, ByteBuf>() {
                @Override
                public void configureNewPipeline(ChannelPipeline pipeline) {
                    // Do Nothing
                }
            };

    private PipelineConfigurators() {
    }

    public static PipelineConfigurator<byte[], byte[]> byteArrayConfigurator() {
        return new ByteArrayPipelineConfigurator();
    }

    public static PipelineConfigurator<String, String> textOnlyConfigurator() {
        return new StringMessageConfigurator();
    }

    public static PipelineConfigurator<String, String> textOnlyConfigurator(Charset inputCharset, Charset outputCharset) {
        return new StringMessageConfigurator(inputCharset, outputCharset);
    }

    public static PipelineConfigurator<String, String> stringMessageConfigurator() {
        return new SimpleTextProtocolConfigurator();
    }

    public static <I, O> PipelineConfigurator<HttpServerRequest<I>, HttpServerResponse<O>> httpServerConfigurator() {
        return new PipelineConfiguratorComposite<HttpServerRequest<I>,
                HttpServerResponse<O>>(new HttpServerPipelineConfigurator<I, O>(),
                                                                         new HttpObjectAggregationConfigurator());
    }

    public static <I, O> PipelineConfigurator<HttpClientResponse<O>, HttpClientRequest<I>> httpClientConfigurator() {
        return new PipelineConfiguratorComposite<HttpClientResponse<O>, HttpClientRequest<I>>(new HttpClientPipelineConfigurator<I, O>(),
                                                                                  new HttpObjectAggregationConfigurator());
    }

    public static <I> PipelineConfigurator<HttpClientResponse<ServerSentEvent>, HttpClientRequest<I>> clientSseConfigurator() {
        return new SseClientPipelineConfigurator<I>();
    }

    public static <I> PipelineConfigurator<HttpServerRequest<I>, HttpServerResponse<ServerSentEvent>> serveSseConfigurator() {
        return new SseServerPipelineConfigurator<I>();
    }

    /**
     * @deprecated Use {@link #clientSseConfigurator()} instead.
     */
    @Deprecated
    public static <I> PipelineConfigurator<HttpClientResponse<io.reactivex.netty.protocol.text.sse.ServerSentEvent>, HttpClientRequest<I>> sseClientConfigurator() {
        return new SseOverHttpClientPipelineConfigurator<I>();
    }

    /**
     * @deprecated Use {@link #serveSseConfigurator()} instead.
     */
    @Deprecated
    public static <I> PipelineConfigurator<HttpServerRequest<I>, HttpServerResponse<io.reactivex.netty.protocol.text.sse.ServerSentEvent>> sseServerConfigurator() {
        return new SseOverHttpServerPipelineConfigurator<I>();
    }

    public static <I, O> PipelineConfigurator<I, O> sslConfigurator(SSLEngineFactory sslEngineFactory) {
        return new SslPipelineConfigurator<I, O>(sslEngineFactory);
    }

    /**
     * Enables wire level logs (all events received by netty) to be logged at the passed {@code logLevel}. This adds
     * a {@link PipelineConfigurator} as returned by {@link #wireLoggingConfigurator(LogLevel)} to the returned
     * configurator. <br/>
     *
     * Since, in most of the production systems, the logging level is set to {@link LogLevel#WARN} or
     * {@link LogLevel#ERROR}, if this wire level logging is required for all requests (not at all recommended as this
     * logging is very verbose), the passed level must be {@link LogLevel#WARN} or {@link LogLevel#ERROR} respectively. <br/>
     *
     * It is recommended to set this level to {@link LogLevel#DEBUG} and then dynamically enabled disable this log level
     * whenever required. <br/>
     *
     * @param logLevel Log level at which the wire level logs will be logged.
     *
     * @return This builder.
     *
     * @see LoggingHandler
     */
    public static <I, O> PipelineConfigurator<I, O> appendLoggingConfigurator(PipelineConfigurator<I, O> existing,
                                                                              LogLevel logLevel) {
        if (null == existing) {
            return wireLoggingConfigurator(logLevel);
        }
        return new PipelineConfiguratorComposite<I, O>(existing, wireLoggingConfigurator(logLevel));
    }

    /**
     * Creates a new {@link PipelineConfiguratorComposite} for both the passed configurators. <br/>
     * The only real convenience is that the {@code existing} configurator can be null.
     *
     * @param existing Existing configurator. Can be {@code null}
     * @param additional Additional configurator. Can not be {@code null}
     *
     * @return Composite configurator.
     * @throws NullPointerException If the passed existing configurator is null.
     */
    public static <I, O> PipelineConfigurator<I, O> composeConfigurators(PipelineConfigurator<I, O> existing,
                                                                         PipelineConfigurator<I, O> additional) {
        if(null == additional) {
            throw new NullPointerException("Additional configurator can not be null.");
        }
        if (null == existing) {
            return additional;
        }
        return new PipelineConfiguratorComposite<I, O>(existing, additional);

    }


    /**
     * Enables wire level logs (all events received by netty) to be logged at the passed {@code wireLogginLevel}. <br/>
     *
     * Since, in most of the production systems, the logging level is set to {@link LogLevel#WARN} or
     * {@link LogLevel#ERROR}, if this wire level logging is required for all requests (not at all recommended as this
     * logging is very verbose), the passed level must be {@link LogLevel#WARN} or {@link LogLevel#ERROR} respectively. <br/>
     *
     * It is recommended to set this level to {@link LogLevel#DEBUG} and then dynamically enabled disable this log level
     * whenever required. <br/>
     *
     * @param wireLogginLevel Log level at which the wire level logs will be logged.
     *
     * @return This builder.
     *
     * @see LoggingHandler
     */
    public static <I, O> PipelineConfigurator<I, O> wireLoggingConfigurator(final LogLevel wireLogginLevel) {
        return new PipelineConfigurator<I, O>() {
            @Override
            public void configureNewPipeline(ChannelPipeline pipeline) {
                pipeline.addFirst(new LoggingHandler(wireLogginLevel));
            }
        };
    }

    public static <I, O> PipelineConfigurator<I, O> createClientConfigurator(
            PipelineConfigurator<I, O> pipelineConfigurator, RxClient.ClientConfig clientConfig) {
        return createClientConfigurator(pipelineConfigurator, clientConfig, new MetricEventsSubject<ClientMetricsEvent<?>>());
    }

    public static <I, O> PipelineConfigurator<I, O> createClientConfigurator(PipelineConfigurator<I, O> pipelineConfigurator,
                                                                             RxClient.ClientConfig clientConfig,
                                                                             MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject) {

        PipelineConfigurator<I, O> clientRequiredConfigurator;

        if (clientConfig.isReadTimeoutSet()) {
            ReadTimeoutPipelineConfigurator readTimeoutConfigurator =
                    new ReadTimeoutPipelineConfigurator(clientConfig.getReadTimeoutInMillis(), TimeUnit.MILLISECONDS);
            clientRequiredConfigurator =
                    new PipelineConfiguratorComposite<I, O>(new ClientRequiredConfigurator<I, O>(eventsSubject),
                                                            readTimeoutConfigurator);
        } else {
            clientRequiredConfigurator = new ClientRequiredConfigurator<I, O>(eventsSubject);
        }

        if (null != pipelineConfigurator) {
            pipelineConfigurator = new PipelineConfiguratorComposite<I, O>(pipelineConfigurator,
                                                                           clientRequiredConfigurator);
        } else {
            pipelineConfigurator = clientRequiredConfigurator;
        }
        return pipelineConfigurator;

    }

    public static PipelineConfigurator<ByteBuf, ByteBuf> empty() {
        return EMPTY_CONFIGURATOR;
    }
}
