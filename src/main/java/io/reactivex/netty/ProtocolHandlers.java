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
package io.reactivex.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.reactivex.netty.codec.StringLineCodec;
import io.reactivex.netty.spi.NettyPipelineConfigurator;

import java.nio.charset.Charset;

/**
 * Utility class that provides a variety of {@link NettyPipelineConfigurator} implementations
 */
public final class ProtocolHandlers {

    private ProtocolHandlers() {
    }

    /**
     * @return An {@link NettyPipelineConfigurator} implementation that does nothing on inbound data but encode outbound data
     *         as strings. This allows {@link ByteBuf} from a {@link ChannelPipeline} to get passed down to th pipeline.
     */
    public static NettyPipelineConfigurator commandOnlyHandler() {
        return CommandOnlyHandler.INSTANCE;
    }

    /**
     * @return An {@link NettyPipelineConfigurator} implementation that does nothing on inbound data but encode outbound data
     *         as strings. This allows {@link ByteBuf} from a {@link ChannelPipeline} to get passed down to th pipeline.
     */
    public static NettyPipelineConfigurator commandOnlyHandler(Charset charset) {
        return new CommandOnlyHandler(charset);
    }

    /**
     * @return An {@link NettyPipelineConfigurator} implementation that encode and decode outgoing and incoming data
     * to/from strings.
     */
    public static NettyPipelineConfigurator stringCodec() {
        return new StringCodec();
    }

    /**
     * @return An {@link NettyPipelineConfigurator} implementation that encode and decode outgoing and incoming data
     * to/from strings.
     */
    public static NettyPipelineConfigurator stringCodec(Charset inputCharset, Charset outputCharset) {
        return new StringCodec(inputCharset, outputCharset);
    }

    public static NettyPipelineConfigurator stringLineCodec() {
        return new StringLineCodec();
    }

    /**
     * The identity protocol handler. It does nothing, allowing any given {@link ByteBuf} object to reach {@link ConnectionLifecycleHandler} directly.
     */
    public static class CommandOnlyHandler implements NettyPipelineConfigurator {

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

    public static class StringCodec implements NettyPipelineConfigurator {

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
