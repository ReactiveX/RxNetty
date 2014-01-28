package io.reactivex.netty.protocol.text.sse;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.protocol.http.sse.SSEInboundHandler;
import io.reactivex.netty.protocol.http.sse.SseOverHttpClientPipelineConfigurator;

/**
 * An implementation of {@link PipelineConfigurator} that will setup Netty's pipeline for a client recieving
 * Server Sent Events. <br/>
 * This will convert {@link ByteBuf} objects to {@link SSEEvent}. So, if the client is an HTTP client, then you would
 * have to use {@link SseOverHttpClientPipelineConfigurator} instead.
 *
 * @param <W> The request type for the client pipeline.
 *
 * @see {@link ServerSentEventDecoder}
 *
 * @author Nitesh Kant
 */
public class SSEClientPipelineConfigurator<W> implements PipelineConfigurator<SSEEvent, W> {

    public static final SSEInboundHandler SSE_INBOUND_HANDLER = new SSEInboundHandler();

    @Override
    public void configureNewPipeline(ChannelPipeline pipeline) {
        pipeline.addLast(SSEInboundHandler.NAME, SSE_INBOUND_HANDLER);
    }
}
