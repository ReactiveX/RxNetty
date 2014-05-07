package io.reactivex.netty.contexts;

import io.reactivex.netty.contexts.http.HttpClientContextConfigurator;
import io.reactivex.netty.contexts.http.HttpServerContextConfigurator;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.text.sse.ServerSentEvent;

/**
 * A factory class for different {@link PipelineConfigurator} for the context module.
 *
 * @author Nitesh Kant
 */
public final class ContextPipelineConfigurators {

    private ContextPipelineConfigurators() {
    }


    public static <I, O> PipelineConfigurator<HttpServerRequest<I>, HttpServerResponse<O>>
    httpServerConfigurator(RequestIdProvider requestIdProvider, RequestCorrelator correlator,
                           PipelineConfigurator<HttpServerRequest<I>, HttpServerResponse<O>> httpConfigurator) {
        return new HttpServerContextConfigurator<I, O>(requestIdProvider, correlator, httpConfigurator);
    }

    public static <I, O> PipelineConfigurator<HttpClientResponse<O>, HttpClientRequest<I>>
    httpClientConfigurator(RequestIdProvider requestIdProvider, RequestCorrelator correlator,
                           PipelineConfigurator<HttpClientResponse<O>, HttpClientRequest<I>> httpConfigurator) {
        return new HttpClientContextConfigurator<I, O>(requestIdProvider, correlator, httpConfigurator);
    }

    public static <I, O> PipelineConfigurator<HttpServerRequest<I>, HttpServerResponse<O>>
    httpServerConfigurator(RequestIdProvider requestIdProvider, RequestCorrelator correlator) {
        return httpServerConfigurator(requestIdProvider, correlator, PipelineConfigurators.<I, O>httpServerConfigurator());
    }

    public static <I, O> PipelineConfigurator<HttpClientResponse<O>, HttpClientRequest<I>>
    httpClientConfigurator(RequestIdProvider requestIdProvider, RequestCorrelator correlator) {
        return httpClientConfigurator(requestIdProvider, correlator, PipelineConfigurators.<I, O>httpClientConfigurator());
    }

    public static <I> PipelineConfigurator<HttpClientResponse<ServerSentEvent>, HttpClientRequest<I>>
    sseClientConfigurator(RequestIdProvider requestIdProvider, RequestCorrelator correlator) {
        return new HttpClientContextConfigurator<I, ServerSentEvent>(requestIdProvider, correlator,
                                                                     PipelineConfigurators.<I>sseClientConfigurator());
    }

    public static <I> PipelineConfigurator<HttpServerRequest<I>, HttpServerResponse<ServerSentEvent>>
    sseServerConfigurator(RequestIdProvider requestIdProvider, RequestCorrelator correlator) {
        return new HttpServerContextConfigurator<I, ServerSentEvent>(requestIdProvider, correlator,
                                                                     PipelineConfigurators.<I>sseServerConfigurator());
    }
}
