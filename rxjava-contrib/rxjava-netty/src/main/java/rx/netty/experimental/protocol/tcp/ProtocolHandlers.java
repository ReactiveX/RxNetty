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
package rx.netty.experimental.protocol.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

/**
 * Utility class that provides a variety of {@link ProtocolHandler} implementations
 */
public class ProtocolHandlers {
    /**
     *  @return An {@link ProtocolHandler} implementation that does nothing on inbound data but encode outbound data
     *  as strings. This allows {@line ByteBuf} from a {@link ChannelPipeline} to get passed down to th pipeline.
     */
    public static ProtocolHandler<ByteBuf, String> commandOnlyHandler() {
        return CommandOnlyHandler.INSTANCE;
    }

    /**
     * @return An {@link ProtocolHandler} implementation that encode and decode outgoing and incoming data to strings
     */
    public static ProtocolHandler<String, String> stringCodec() {
        return new StringCodec();
    }

    public static ProtocolHandler<FullHttpResponse, Void> fullHttpContentHandler(){
        return new ProtocolHandler<FullHttpResponse, Void>() {

            @Override
            public void configure(ChannelPipeline pipeline) {
                pipeline
                    .addLast(new HttpRequestDecoder())
                    .addLast(new HttpObjectAggregator(Integer.MAX_VALUE));
            }

        };
    }


    public static ProtocolHandler<String, String> stringLineCodec() {
        return new StringLineCodec();
    }

    /**
     * The identity protocol handler. It does nothing, allowing any given
     * {@link io.netty.buffer.ByteBuf} object to reach {@link rx.netty.experimental.impl.HandlerObserver}
     * directly.
     */
    public static class CommandOnlyHandler implements ProtocolHandler<ByteBuf, String> {

        private CommandOnlyHandler(){}

        @Override
        public void configure(ChannelPipeline pipeline) {
            pipeline.addLast(new StringEncoder());
        }

        public static final CommandOnlyHandler INSTANCE = new CommandOnlyHandler();
    }

    public static class StringCodec implements ProtocolHandler<String, String> {

        @Override
        public void configure(ChannelPipeline pipeline) {
            pipeline
                .addLast(new StringDecoder())
                .addLast(new StringEncoder());
        }
    }
}
