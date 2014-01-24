package io.reactivex.netty.pipeline;

import io.netty.channel.ChannelPipeline;

/**
 * A composable implementation of {@link PipelineConfigurator} to compose a pipeline configuration out of multiple
 * {@link PipelineConfigurator} implementations. <br/>
 *
 * @author Nitesh Kant
 */
public class PipelineConfiguratorComposite implements PipelineConfigurator {

    private static final PipelineConfigurator[] EMPTY_CONFIGURATORS = new PipelineConfigurator[0];

    private final PipelineConfigurator[] configurators;

    public PipelineConfiguratorComposite(PipelineConfigurator... configurators) {
        if (null == configurators) {
            configurators = EMPTY_CONFIGURATORS;
        }
        this.configurators = configurators;
    }

    /**
     * Invokes {@link PipelineConfigurator#configureNewPipeline(ChannelPipeline)} on all the
     * underlying {@link PipelineConfigurator} instances in the same order as they were created.
     *
     * @param pipeline The pipeline to configure.
     */
    @Override
    public void configureNewPipeline(ChannelPipeline pipeline) {
        for (PipelineConfigurator configurator : configurators) {
            configurator.configureNewPipeline(pipeline);
        }
    }
}
