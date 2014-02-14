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
import io.reactivex.netty.client.RxClientImpl;
import io.reactivex.netty.server.RxServer;

/**
 * A contract for configuring Netty's channel pipeline according to the required protocol.
 *
 * @param <R> The input object type for the pipeline i.e. the object type which is read from the pipeline.
 *           This type is not enforced by the pipeline configurator per se, but they are just to make the creator of
 *           {@link RxServer} and {@link RxClientImpl} type aware.
 *
 * @param <W> The output object type for the pipeline i.e. the object type which is written to the pipeline.
 *           This type is not enforced by the pipeline configurator per se, but they are just to make the creator of
 *           {@link RxServer} and {@link RxClientImpl} type aware.
 */
public interface PipelineConfigurator<R, W> {

    /**
     * A callback to configure the passed {@code pipeline}. This will be invoked everytime a new netty pipeline is
     * created, which is whenever a new channel is established.
     *
     * @param pipeline The pipeline to configure.
     */
    void configureNewPipeline(ChannelPipeline pipeline);

}
