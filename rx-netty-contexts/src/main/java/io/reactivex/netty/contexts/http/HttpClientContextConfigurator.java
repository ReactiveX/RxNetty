package io.reactivex.netty.contexts.http;

import io.netty.channel.ChannelPipeline;
import io.reactivex.netty.contexts.RequestCorrelator;
import io.reactivex.netty.contexts.RequestIdProvider;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;

/**
 * An implementation of {@link PipelineConfigurator} to configure an {@link HttpClient} with the context handling.
 *
 * @author Nitesh Kant
 */
public class HttpClientContextConfigurator<I, O> implements
        PipelineConfigurator<HttpClientResponse<O>, HttpClientRequest<I>> {

    public static final String CTX_HANDLER_NAME = "http-client-context-handler";

    private final RequestCorrelator correlator;
    private final RequestIdProvider requestIdProvider;
    private final PipelineConfigurator<HttpClientResponse<O>, HttpClientRequest<I>> httpConfigurator;

    public HttpClientContextConfigurator(RequestIdProvider requestIdProvider, RequestCorrelator correlator,
                                         PipelineConfigurator<HttpClientResponse<O>, HttpClientRequest<I>> httpConfigurator) {
        this.requestIdProvider = requestIdProvider;
        this.correlator = correlator;
        this.httpConfigurator = httpConfigurator;
    }

    @Override
    public void configureNewPipeline(ChannelPipeline pipeline) {
        httpConfigurator.configureNewPipeline(pipeline);
        /**
         * The reason why this is not paranoid about adding this handler exactly after the client codec is that none
         * of the pipeline configurator really create an Rx request/response. They are always netty's request/response.
         * So, this handler would always get netty's HTTP messages (as it expects).
         */
        pipeline.addLast(CTX_HANDLER_NAME, new HttpClientContextHandler(requestIdProvider, correlator));
    }
}
