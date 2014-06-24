/*
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.reactivex.netty.pipeline;

import io.netty.channel.ChannelPipeline;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A composable implementation of {@link PipelineConfigurator} to compose a pipeline configuration out of multiple
 * {@link PipelineConfigurator} implementations. <br/>
 *
 * @author Nitesh Kant
 */
public class PipelineConfiguratorComposite<I, O> implements PipelineConfigurator<I, O> {

    @SuppressWarnings("rawtypes")
    private static final PipelineConfigurator[] EMPTY_CONFIGURATORS = new PipelineConfigurator[0];

    @SuppressWarnings("rawtypes")
    private final PipelineConfigurator[] configurators;

    public PipelineConfiguratorComposite(@SuppressWarnings("rawtypes")PipelineConfigurator... configurators) {
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
        for (@SuppressWarnings("rawtypes") PipelineConfigurator configurator : configurators) {
            if (null != configurator) {
                configurator.configureNewPipeline(pipeline);
            }
        }
    }

    /**
     * Returns an unmodifiable list of the constituent configurators in this composite.
     *
     * @return An unmodifiable list of the constituent configurators in this composite.
     */
    @SuppressWarnings("rawtypes")
    public List<PipelineConfigurator> getConstituentConfigurators() {
        return Collections.unmodifiableList(Arrays.asList(configurators));
    }
}
