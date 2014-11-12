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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.nio.charset.Charset;

/**
 * An implementation of {@link PipelineConfigurator} to configure {@link StringEncoder} and {@link StringDecoder} to
 * convert incoming {@link ByteBuf} into {@link String} and outgoing {@link String} as {@link ByteBuf}
 *
 * @see StringEncoder
 * @see StringDecoder
 *
 * @author Nitesh Kant
 */
public class StringMessageConfigurator implements PipelineConfigurator<String, String> {

    private final Charset inputCharset;
    private final Charset outputCharset;

    public StringMessageConfigurator() {
        this(Charset.defaultCharset(), Charset.defaultCharset());
    }

    public StringMessageConfigurator(Charset inputCharset, Charset outputCharset) {
        this.inputCharset = inputCharset;
        this.outputCharset = outputCharset;
    }

    @Override
    public void configureNewPipeline(ChannelPipeline pipeline) {
        pipeline.addLast(new StringDecoder(outputCharset))
                .addLast(new StringEncoder(inputCharset));
    }
}
