/**
 * Copyright 2013 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
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
import io.reactivex.netty.protocol.text.SimpleTextProtocolConfigurator;

import java.nio.charset.Charset;

/**
 * Utility class that provides a variety of {@link PipelineConfigurator} implementations
 */
public final class PipelineConfigurators {

    private PipelineConfigurators() {
    }

    public static PipelineConfigurator<ByteBuf, String> commandOnlyHandler() {
        return CommandOnlyHandler.INSTANCE;
    }

    public static PipelineConfigurator<ByteBuf, String> commandOnlyHandler(Charset charset) {
        return new CommandOnlyHandler(charset);
    }

    public static PipelineConfigurator<String, String> stringCodec() {
        return new StringCodec();
    }

    public static PipelineConfigurator<String, String> stringCodec(Charset inputCharset, Charset outputCharset) {
        return new StringCodec(inputCharset, outputCharset);
    }

    public static PipelineConfigurator<String, String> stringLineCodec() {
        return new SimpleTextProtocolConfigurator();
    }

    public static class CommandOnlyHandler implements PipelineConfigurator<ByteBuf, String> {

        public static final CommandOnlyHandler INSTANCE = new CommandOnlyHandler();
        private final Charset dataCharset;

        private CommandOnlyHandler() {
            this(Charset.defaultCharset());
        }

        public CommandOnlyHandler(Charset dataCharset) {
            this.dataCharset = dataCharset;
        }

        @Override
        public void configureNewPipeline(ChannelPipeline pipeline) {
            pipeline.addLast(new StringEncoder(dataCharset));
        }
    }

    public static class StringCodec implements PipelineConfigurator<String, String> {

        private final Charset inputCharset;
        private final Charset outputCharset;

        public StringCodec() {
            this(Charset.defaultCharset(), Charset.defaultCharset());
        }

        public StringCodec(Charset inputCharset, Charset outputCharset) {
            this.inputCharset = inputCharset;
            this.outputCharset = outputCharset;
        }

        @Override
        public void configureNewPipeline(ChannelPipeline pipeline) {
            pipeline.addLast(new StringDecoder(outputCharset))
                    .addLast(new StringEncoder(inputCharset));
        }
    }
}
