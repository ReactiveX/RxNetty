package io.reactivex.netty.contexts.http;

import io.netty.channel.ChannelPipeline;
import io.reactivex.netty.contexts.RequestCorrelator;
import io.reactivex.netty.contexts.RequestIdProvider;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;

/**
 * An implementation of {@link PipelineConfigurator} to configure an {@link HttpServer} with the context handling.
 *
 * @author Nitesh Kant
 */
public class HttpServerContextConfigurator<I, O> implements
        PipelineConfigurator<HttpServerRequest<I>, HttpServerResponse<O>> {

    public static final String CTX_HANDLER_NAME = "http-server-context-handler";

    private final RequestCorrelator correlator;
    private final RequestIdProvider requestIdProvider;
    private final PipelineConfigurator<HttpServerRequest<I>, HttpServerResponse<O>> httpConfigurator;

    public HttpServerContextConfigurator(RequestIdProvider requestIdProvider, RequestCorrelator correlator,
                                         PipelineConfigurator<HttpServerRequest<I>, HttpServerResponse<O>> httpConfigurator) {
        this.requestIdProvider = requestIdProvider;
        this.correlator = correlator;
        this.httpConfigurator = httpConfigurator;
    }

    @Override
    public void configureNewPipeline(ChannelPipeline pipeline) {
        httpConfigurator.configureNewPipeline(pipeline);
        pipeline.addLast(CTX_HANDLER_NAME, new HttpServerContextHandler(requestIdProvider, correlator));
    }
}
