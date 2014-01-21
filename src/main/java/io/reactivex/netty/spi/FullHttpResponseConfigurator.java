package io.reactivex.netty.spi;

import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObjectAggregator;

/**
 * An implementation of {@link NettyPipelineConfigurator} that can be applied with an implementation of
 * {@link HttpPipelineConfigurator} so that instead of multiple events per Http request/response, they are aggregated
 * as a single request/response. <p/>
 *
 * @see {@link HttpObjectAggregator}
 *
 * @author Nitesh Kant
 */
public class FullHttpResponseConfigurator implements NettyPipelineConfigurator {

    public static final String AGGREGATOR_HANDLER_NAME = "http-aggregator";

    private final int maxChunkSize;
    private final HttpPipelineConfigurator httpPipelineConfigurator;

    public FullHttpResponseConfigurator(int maxChunkSize, HttpPipelineConfigurator httpConfigurator) {
        if (null == httpConfigurator) {
            throw new IllegalArgumentException("Http configurator can not be null.");
        }
        this.maxChunkSize = maxChunkSize;
        httpPipelineConfigurator = httpConfigurator;
    }

    @Override
    public void configureNewPipeline(ChannelPipeline pipeline) {
        httpPipelineConfigurator.configureNewPipeline(pipeline);
        pipeline.addLast(AGGREGATOR_HANDLER_NAME, new HttpObjectAggregator(maxChunkSize));
    }
}
