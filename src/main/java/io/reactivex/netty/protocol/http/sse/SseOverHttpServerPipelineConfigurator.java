package io.reactivex.netty.protocol.http.sse;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.protocol.http.HttpServerPipelineConfigurator;
import io.reactivex.netty.protocol.text.sse.SSEServerPipelineConfigurator;
import io.reactivex.netty.protocol.text.sse.ServerSentEventEncoder;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;

/**
 * An extension to {@link SSEServerPipelineConfigurator} that enables SSE over HTTP. <br/>
 *
 * @see {@link ServerSentEventEncoder}
 *
 * @author Nitesh Kant
 */
public class SseOverHttpServerPipelineConfigurator<I extends HttpObject> extends
        SSEServerPipelineConfigurator<I, Object> {

    public static final String BYTE_BUF_TO_HTTP_CONTENT_ENCODER_HANDLER_NAME = "bytebuf-http-content-encoder";
    public static final String SSE_RESPONSE_HEADERS_COMPLETER = "sse-response-headers-completer";

    private final PipelineConfigurator<I, ?> pipelineConfigurator;

    public SseOverHttpServerPipelineConfigurator(PipelineConfigurator<I, ?> httpServerPipelineConfigurator) {
        pipelineConfigurator = httpServerPipelineConfigurator;
    }

    public SseOverHttpServerPipelineConfigurator(HttpServerPipelineConfigurator<I, ?> httpServerPipelineConfigurator) {
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
