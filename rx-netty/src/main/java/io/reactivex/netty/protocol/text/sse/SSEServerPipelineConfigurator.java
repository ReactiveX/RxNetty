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
import io.reactivex.netty.protocol.http.sse.SseOverHttpServerPipelineConfigurator;

/**
 * An implementation of {@link PipelineConfigurator} that will setup Netty's pipeline for a server sending
 * Server Sent Events. <br/>
 * This will convert {@link ServerSentEvent} objects to {@link ByteBuf}. So, if the server is an HTTP server, then you would
 * have to use {@link SseOverHttpServerPipelineConfigurator} instead.
 *
 * @see ServerSentEventEncoder
 *
 * @author Nitesh Kant
 *
 * @deprecated Since SSE is always over HTTP, using the same protocol name isn't correct.
 */
@Deprecated
public class SSEServerPipelineConfigurator<R, W> implements PipelineConfigurator<R, W> {

    public static final String SSE_ENCODER_HANDLER_NAME = "sse-encoder";

    public static final ServerSentEventEncoder SERVER_SENT_EVENT_ENCODER = new ServerSentEventEncoder(); // contains no state.

    @Override
    public void configureNewPipeline(ChannelPipeline pipeline) {
        pipeline.addLast(SSE_ENCODER_HANDLER_NAME, SERVER_SENT_EVENT_ENCODER);
    }
}
