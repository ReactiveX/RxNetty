package io.reactivex.netty.http.sse;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;
import io.reactivex.netty.http.sse.codec.SSEEvent;
import io.reactivex.netty.http.sse.codec.ServerSentEventDecoder;
import io.reactivex.netty.spi.NettyPipelineConfigurator;

/**
 * An implementation of {@link NettyPipelineConfigurator} that will setup Netty's pipeline for a client recieving
 * Server Sent Events. <br/>
 * This will convert {@link ByteBuf} objects to {@link SSEEvent}. So, if the client is an HTTP client, then you would
 * have to use {@link SseOverHttpClientPipelineConfigurator} instead.
 *
 * @see {@link ServerSentEventDecoder}
 *
 * @author Nitesh Kant
 */
public class SSEClientPipelineConfigurator implements NettyPipelineConfigurator {

    public static final String SSE_DECODER_HANDLER_NAME = "sse-decoder";

    @Override
    public void configureNewPipeline(ChannelPipeline pipeline) {
        pipeline.addLast(SSE_DECODER_HANDLER_NAME, new ServerSentEventDecoder());
    }
}
