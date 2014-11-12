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
package io.reactivex.netty.protocol.text;

import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.string.StringEncoder;
import io.reactivex.netty.pipeline.PipelineConfigurator;

import java.nio.charset.Charset;

/**
 * An implementation of {@link PipelineConfigurator} to have simple text based protocol.
 *
 * @see StringEncoder
 * @see StringLineDecoder
 */
public class SimpleTextProtocolConfigurator implements PipelineConfigurator<String, String> {

    private final Charset inputCharset;

    public SimpleTextProtocolConfigurator() {
        this(Charset.defaultCharset());
    }

    public SimpleTextProtocolConfigurator(Charset inputCharset) {
        this.inputCharset = inputCharset;
    }

    @Override
    public void configureNewPipeline(ChannelPipeline pipeline) {
        pipeline.addLast(new StringEncoder(inputCharset))
                .addLast(new StringLineDecoder());
    }
}
