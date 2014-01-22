package io.reactivex.netty.http.sse;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;
import io.reactivex.netty.http.sse.codec.SSEEvent;
import io.reactivex.netty.http.sse.codec.ServerSentEventEncoder;
import io.reactivex.netty.spi.NettyPipelineConfigurator;

/**
 * An implementation of {@link NettyPipelineConfigurator} that will setup Netty's pipeline for a server sending
 * Server Sent Events. <br/>
 * This will convert {@link SSEEvent} objects to {@link ByteBuf}. So, if the server is an HTTP server, then you would
 * have to use {@link SseOverHttpServerPipelineConfigurator} instead.
 *
 * @see {@link ServerSentEventEncoder}
 *
 * @author Nitesh Kant
 */
public class SSEServerPipelineConfigurator implements NettyPipelineConfigurator {

    public static final String SSE_ENCODER_HANDLER_NAME = "sse-encoder";

    public static final ServerSentEventEncoder SERVER_SENT_EVENT_ENCODER = new ServerSentEventEncoder(); // contains no state.

    @Override
    public void configureNewPipeline(ChannelPipeline pipeline) {
        pipeline.addLast(SSE_ENCODER_HANDLER_NAME, SERVER_SENT_EVENT_ENCODER);
    }
}
