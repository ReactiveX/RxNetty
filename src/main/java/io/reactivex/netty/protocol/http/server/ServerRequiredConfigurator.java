package io.reactivex.netty.protocol.http.server;

import io.netty.channel.ChannelPipeline;
import io.reactivex.netty.pipeline.PipelineConfigurator;

/**
 *
 * @param <I> The type of the content of request.
 * @param <O> The type of the content of response.
 *
 * @author Nitesh Kant
 */
class ServerRequiredConfigurator<I, O> implements PipelineConfigurator<HttpRequest<I>, HttpResponse<O>> {

    public static final String REQUEST_RESPONSE_CONVERTER_HANDLER_NAME = "request-response-converter";

    @Override
    public void configureNewPipeline(ChannelPipeline pipeline) {
        pipeline.addLast(REQUEST_RESPONSE_CONVERTER_HANDLER_NAME, new ServerRequestResponseConverter());
    }
}
