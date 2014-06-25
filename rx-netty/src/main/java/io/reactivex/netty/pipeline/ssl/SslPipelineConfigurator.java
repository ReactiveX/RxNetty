package io.reactivex.netty.pipeline.ssl;

import io.netty.channel.ChannelPipeline;
import io.netty.handler.ssl.SslHandler;
import io.reactivex.netty.pipeline.PipelineConfigurator;

/**
 * @author Tomasz Bak
 */
public class SslPipelineConfigurator<I, O> implements PipelineConfigurator<I, O> {

    private final SSLEngineFactory sslEngineFactory;

    public SslPipelineConfigurator(SSLEngineFactory sslEngineFactory) {
        this.sslEngineFactory = sslEngineFactory;
    }

    @Override
    public void configureNewPipeline(ChannelPipeline pipeline) {
        pipeline.addFirst(new SslHandler(sslEngineFactory.createSSLEngine(pipeline.channel().alloc())));
    }
}
