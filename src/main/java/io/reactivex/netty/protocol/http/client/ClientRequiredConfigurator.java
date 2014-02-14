package io.reactivex.netty.protocol.http.client;

import io.netty.channel.ChannelPipeline;
import io.reactivex.netty.pipeline.PipelineConfigurator;

/**
 *
 * @param <I> The type of the content of request.
 * @param <O> The type of the content of response.
 *
 * @author Nitesh Kant
 */
class ClientRequiredConfigurator<I, O> implements PipelineConfigurator<HttpResponse<O>, HttpRequest<I>> {

    @Override
    public void configureNewPipeline(ChannelPipeline pipeline) {
        ClientRequestResponseConverter converter = pipeline.get(ClientRequestResponseConverter.class);
        if (null == converter) {
            pipeline.addLast(HttpClientPipelineConfigurator.REQUEST_RESPONSE_CONVERTER_HANDLER_NAME,
                             new ClientRequestResponseConverter());
        }
    }
}
