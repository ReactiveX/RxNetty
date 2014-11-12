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
package io.reactivex.netty.protocol.http;

import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.reactivex.netty.pipeline.PipelineConfigurator;

/**
 * An implementation of {@link PipelineConfigurator} that can be applied with an implementation of
 * {@link AbstractHttpConfigurator} so that instead of multiple events per Http request/response, they are aggregated
 * as a single request/response. <p/>
 *
 * @see HttpObjectAggregator
 *
 * @author Nitesh Kant
 */
public class HttpObjectAggregationConfigurator<R extends FullHttpMessage, W> implements PipelineConfigurator<R, W> {

    public static final String AGGREGATOR_HANDLER_NAME = "http-aggregator";

    public static final int DEFAULT_CHUNK_SIZE = 1048576; // 1 MB

    private final int maxChunkSize;

    public HttpObjectAggregationConfigurator() {
        this(DEFAULT_CHUNK_SIZE);
    }

    public HttpObjectAggregationConfigurator(int maxChunkSize) {
        this.maxChunkSize = maxChunkSize;
    }

    @Override
    public void configureNewPipeline(ChannelPipeline pipeline) {
        pipeline.addLast(AGGREGATOR_HANDLER_NAME, new HttpObjectAggregator(maxChunkSize));
    }
}
