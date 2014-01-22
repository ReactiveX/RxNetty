package io.reactivex.netty.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.reactivex.netty.spi.NettyPipelineConfigurator;

/**
 * An implementation of {@link NettyPipelineConfigurator} that can be applied with an implementation of
 * {@link HttpPipelineConfigurator} so that instead of multiple events per Http request/response, they are aggregated
 * as a single request/response. <p/>
 *
 * @see {@link HttpObjectAggregator}
 *
 * @author Nitesh Kant
 */
public class HttpObjectAggregationConfigurator implements NettyPipelineConfigurator {

    public static final String AGGREGATOR_HANDLER_NAME = "http-aggregator";

    public static final int DEFAULT_CHUNK_SIZE = 1048576; // 1 MB

    private final int maxChunkSize;
    private final HttpPipelineConfigurator httpPipelineConfigurator;

    public HttpObjectAggregationConfigurator(HttpPipelineConfigurator httpConfigurator) {
        this(DEFAULT_CHUNK_SIZE, httpConfigurator);
    }

    public HttpObjectAggregationConfigurator(int maxChunkSize, HttpPipelineConfigurator httpConfigurator) {
        if (null == httpConfigurator) {
            throw new IllegalArgumentException("Http configurator can not be null.");
        }
        this.maxChunkSize = maxChunkSize;
        httpPipelineConfigurator = httpConfigurator;
    }

    @Override
    public void configureNewPipeline(ChannelPipeline pipeline) {
        httpPipelineConfigurator.configureNewPipeline(pipeline);
        ChannelHandlerContext completerCtx = pipeline.context(HttpServerPipelineConfigurator.FullHttpResponseCompleter.class);
        if (completerCtx != null) {
            pipeline.addBefore(completerCtx.name(), AGGREGATOR_HANDLER_NAME,
                               new HttpObjectAggregator(maxChunkSize));
        } else {
            pipeline.addLast(AGGREGATOR_HANDLER_NAME, new HttpObjectAggregator(maxChunkSize));
        }
    }
}
