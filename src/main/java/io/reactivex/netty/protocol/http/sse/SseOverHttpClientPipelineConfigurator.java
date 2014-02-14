package io.reactivex.netty.protocol.http.sse;

import io.netty.channel.ChannelPipeline;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.protocol.http.client.HttpClientPipelineConfigurator;
import io.reactivex.netty.protocol.http.client.HttpRequest;
import io.reactivex.netty.protocol.http.client.HttpResponse;
import io.reactivex.netty.protocol.text.sse.SSEClientPipelineConfigurator;
import io.reactivex.netty.protocol.text.sse.SSEEvent;
import io.reactivex.netty.protocol.text.sse.ServerSentEventDecoder;

/**
 * An extension to {@link SSEClientPipelineConfigurator} that enables SSE over HTTP. <br/>
 *
 * @see {@link SSEInboundHandler}
 * @see {@link ServerSentEventDecoder}
 *
 * @author Nitesh Kant
 */
public class SseOverHttpClientPipelineConfigurator<I> implements PipelineConfigurator<HttpResponse<SSEEvent>, HttpRequest<I>> {

    private final HttpClientPipelineConfigurator<I, ?> httpClientPipelineConfigurator;

    public SseOverHttpClientPipelineConfigurator() {
        this(new HttpClientPipelineConfigurator<I, Object>());
    }

    public SseOverHttpClientPipelineConfigurator(HttpClientPipelineConfigurator<I, ?> httpClientPipelineConfigurator) {
        this.httpClientPipelineConfigurator = httpClientPipelineConfigurator;
    }

    @Override
    public void configureNewPipeline(ChannelPipeline pipeline) {
        httpClientPipelineConfigurator.configureNewPipeline(pipeline);
        if (null != pipeline.get(HttpClientPipelineConfigurator.REQUEST_RESPONSE_CONVERTER_HANDLER_NAME)) {
            pipeline.addBefore(HttpClientPipelineConfigurator.REQUEST_RESPONSE_CONVERTER_HANDLER_NAME,
                               SSEInboundHandler.NAME, SSEClientPipelineConfigurator.SSE_INBOUND_HANDLER);
        } else {
            // Assuming that the underlying HTTP configurator knows what its doing. It will mostly fail though.
            pipeline.addLast(SSEInboundHandler.NAME, SSEClientPipelineConfigurator.SSE_INBOUND_HANDLER);
        }
    }
}
