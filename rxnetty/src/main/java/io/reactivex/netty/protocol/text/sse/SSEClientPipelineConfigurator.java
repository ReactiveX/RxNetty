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
package io.reactivex.netty.protocol.text.sse;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.protocol.http.sse.SSEInboundHandler;
import io.reactivex.netty.protocol.http.sse.SseOverHttpClientPipelineConfigurator;

/**
 * An implementation of {@link PipelineConfigurator} that will setup Netty's pipeline for a client recieving
 * Server Sent Events. <br/>
 * This will convert {@link ByteBuf} objects to {@link ServerSentEvent}. So, if the client is an HTTP client, then you would
 * have to use {@link SseOverHttpClientPipelineConfigurator} instead.
 *
 * @param <W> The request type for the client pipeline.
 *
 * @see ServerSentEventDecoder
 *
 * @author Nitesh Kant
 *
 * @deprecated Since SSE is always over HTTP, using the same protocol name isn't correct.
 */
@Deprecated
public class SSEClientPipelineConfigurator<W> implements PipelineConfigurator<ServerSentEvent, W> {

    public static final SSEInboundHandler SSE_INBOUND_HANDLER = new SSEInboundHandler();

    @Override
    public void configureNewPipeline(ChannelPipeline pipeline) {
        pipeline.addLast(SSEInboundHandler.NAME, SSE_INBOUND_HANDLER);
    }
}
