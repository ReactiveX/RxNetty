package io.reactivex.netty.protocol.http.sse;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;
import io.reactivex.netty.protocol.http.HttpClientPipelineConfigurator;
import io.reactivex.netty.protocol.http.sse.codec.ServerSentEventDecoder;

/**
 * An extension to {@link SSEClientPipelineConfigurator} that enables SSE over HTTP. <br/>
 * This just adds {@link SSEInboundHandler} in the pipeline before {@link ServerSentEventDecoder} which always passes
 * a {@link ByteBuf} to the decoder.
 *
 * @see {@link SSEInboundHandler}
 * @see {@link ServerSentEventDecoder}
 *
 * @author Nitesh Kant
 */
public class SseOverHttpClientPipelineConfigurator extends SSEServerPipelineConfigurator {

    public static final SSEInboundHandler SSE_INBOUND_HANDLER = new SSEInboundHandler();
    private final HttpClientPipelineConfigurator httpClientPipelineConfigurator;

    public SseOverHttpClientPipelineConfigurator(HttpClientPipelineConfigurator httpClientPipelineConfigurator) {
        this.httpClientPipelineConfigurator = httpClientPipelineConfigurator;
    }

    @Override
    public void configureNewPipeline(ChannelPipeline pipeline) {
        httpClientPipelineConfigurator.configureNewPipeline(pipeline);
        pipeline.addLast(SSEInboundHandler.NAME, SSE_INBOUND_HANDLER);
        super.configureNewPipeline(pipeline);
    }
}
