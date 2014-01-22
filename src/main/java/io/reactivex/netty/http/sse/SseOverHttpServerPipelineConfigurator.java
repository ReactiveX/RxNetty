package io.reactivex.netty.http.sse;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpResponse;
import io.reactivex.netty.http.HttpServerPipelineConfigurator;
import io.reactivex.netty.http.sse.codec.ServerSentEventEncoder;
import io.reactivex.netty.spi.NettyPipelineConfigurator;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;

/**
 * An extension to {@link SSEServerPipelineConfigurator} that enables SSE over HTTP. <br/>
 *
 * @see {@link ServerSentEventEncoder}
 *
 * @author Nitesh Kant
 */
public class SseOverHttpServerPipelineConfigurator extends SSEServerPipelineConfigurator {

    public static final String BYTE_BUF_TO_HTTP_CONTENT_ENCODER_HANDLER_NAME = "bytebuf-http-content-encoder";
    public static final String SSE_RESPONSE_HEADERS_COMPLETER = "sse-response-headers-completer";

    private final NettyPipelineConfigurator pipelineConfigurator;

    public SseOverHttpServerPipelineConfigurator(NettyPipelineConfigurator nettyPipelineConfigurator) {
        pipelineConfigurator = nettyPipelineConfigurator;
    }

    public SseOverHttpServerPipelineConfigurator(HttpServerPipelineConfigurator httpServerPipelineConfigurator) {
        pipelineConfigurator = httpServerPipelineConfigurator;
    }

    @Override
    public void configureNewPipeline(ChannelPipeline pipeline) {
        super.configureNewPipeline(pipeline);
        pipelineConfigurator.configureNewPipeline(pipeline);
        pipeline.addLast(SSE_RESPONSE_HEADERS_COMPLETER, new ChannelOutboundHandlerAdapter() {
            @Override
            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                if (HttpResponse.class.isAssignableFrom(msg.getClass())) {
                    HttpResponse httpResponse = (HttpResponse) msg;
                    String contentTypeHeader = httpResponse.headers().get(CONTENT_TYPE);
                    if (null == contentTypeHeader) {
                        httpResponse.headers().set(CONTENT_TYPE, "text/event-stream");
                    }
                }
                super.write(ctx, msg, promise);
            }
        });
    }
}
