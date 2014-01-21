package io.reactivex.netty.spi;

import io.netty.channel.ChannelPipeline;

import java.util.Map;

/**
 * A composable implementation of {@link NettyPipelineConfigurator} to compose a pipeline configuration out of multiple
 * {@link NettyPipelineConfigurator} implementations. <br/>
 *
 * @author Nitesh Kant
 */
public class PipelineConfiguratorComposite implements NettyPipelineConfigurator {

    private static final NettyPipelineConfigurator[] EMPTY_CONFIGURATORS = new NettyPipelineConfigurator[0];

    private final NettyPipelineConfigurator[] configurators;

    public PipelineConfiguratorComposite(NettyPipelineConfigurator... configurators) {
        if (null == configurators) {
            configurators = EMPTY_CONFIGURATORS;
        }
        this.configurators = configurators;
    }

    /**
     * Invokes {@link NettyPipelineConfigurator#configureNewPipeline(ChannelPipeline)} on all the
     * underlying {@link NettyPipelineConfigurator} instances in the same order as they were created.
     *
     * @param pipeline The pipeline to configure.
     */
    @Override
    public void configureNewPipeline(ChannelPipeline pipeline) {
        for (NettyPipelineConfigurator configurator : configurators) {
            configurator.configureNewPipeline(pipeline);
        }
    }
}
