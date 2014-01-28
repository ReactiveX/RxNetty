package io.reactivex.netty.protocol.http.sse;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.reactivex.netty.protocol.http.HttpClientPipelineConfigurator;
import io.reactivex.netty.protocol.text.sse.SSEClientPipelineConfigurator;
import io.reactivex.netty.protocol.text.sse.ServerSentEventDecoder;

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
public class SseOverHttpClientPipelineConfigurator<W extends HttpRequest> extends SSEClientPipelineConfigurator<W> {

    private final HttpClientPipelineConfigurator<W, ?> httpClientPipelineConfigurator;

    public <O extends HttpObject> SseOverHttpClientPipelineConfigurator(HttpClientPipelineConfigurator<W, O> httpClientPipelineConfigurator) {
        this.httpClientPipelineConfigurator = httpClientPipelineConfigurator;
    }

    @Override
    public void configureNewPipeline(ChannelPipeline pipeline) {
        httpClientPipelineConfigurator.configureNewPipeline(pipeline);
        super.configureNewPipeline(pipeline);
    }
}
