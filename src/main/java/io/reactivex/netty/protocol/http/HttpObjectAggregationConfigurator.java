package io.reactivex.netty.protocol.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.reactivex.netty.pipeline.PipelineConfigurator;

/**
 * An implementation of {@link PipelineConfigurator} that can be applied with an implementation of
 * {@link HttpPipelineConfigurator} so that instead of multiple events per Http request/response, they are aggregated
 * as a single request/response. <p/>
 *
 * @see {@link HttpObjectAggregator}
 *
 * @author Nitesh Kant
 */
public class HttpObjectAggregationConfigurator<R extends FullHttpMessage, W> implements PipelineConfigurator<R, W> {

    public static final String AGGREGATOR_HANDLER_NAME = "http-aggregator";
    public static final String READ_TIMEOUT_REMOVING_HANDLER_NAME = "http-readtimout-removing-handler";

    public static final int DEFAULT_CHUNK_SIZE = 1048576; // 1 MB

    private final int maxChunkSize;
    private final HttpPipelineConfigurator<? extends HttpObject, W> httpPipelineConfigurator;

    public <I extends HttpObject> HttpObjectAggregationConfigurator(HttpPipelineConfigurator<I, W> httpConfigurator) {
        this(DEFAULT_CHUNK_SIZE, httpConfigurator);
    }

    public <I extends HttpObject> HttpObjectAggregationConfigurator(int maxChunkSize,
                                                                    HttpPipelineConfigurator<I, W> httpConfigurator) {
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
            if (HttpClientPipelineConfigurator.class.isAssignableFrom(httpPipelineConfigurator.getClass())) {
                //pipeline.addLast(READ_TIMEOUT_REMOVING_HANDLER_NAME)
            }
        }
    }
}
