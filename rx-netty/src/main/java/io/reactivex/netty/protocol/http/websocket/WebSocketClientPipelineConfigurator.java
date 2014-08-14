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

package io.reactivex.netty.protocol.http.websocket;

import java.net.URI;

import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.metrics.MetricEventsSubject;
import io.reactivex.netty.pipeline.PipelineConfigurator;

/**
 * Initial channel setup contains HTTP handlers together with {@link WebSocketClientHandler}
 * for WebSocket handshake orchestration. Once handshake is done, the channel is dynamically reconfigured.
 *
 * @author Tomasz Bak
 */
public class WebSocketClientPipelineConfigurator<R, W> implements PipelineConfigurator<R, W> {

    private final URI webSocketURI;
    private final WebSocketVersion webSocketVersion;
    private final String subprotocol;
    private final boolean allowExtensions;
    private final int maxFramePayloadLength;
    private final boolean messageAggregation;
    private final MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject;

    public WebSocketClientPipelineConfigurator(URI webSocketURI, WebSocketVersion webSocketVersion,
                                               String subprotocol, boolean allowExtensions,
                                               int maxFramePayloadLength, boolean messageAggregation,
                                               MetricEventsSubject<ClientMetricsEvent<?>> eventsSubject) {
        this.webSocketURI = webSocketURI;
        this.webSocketVersion = webSocketVersion;
        this.subprotocol = subprotocol;
        this.allowExtensions = allowExtensions;
        this.maxFramePayloadLength = maxFramePayloadLength;
        this.messageAggregation = messageAggregation;
        this.eventsSubject = eventsSubject;
    }

    @Override
    public void configureNewPipeline(ChannelPipeline pipeline) {
        WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory.newHandshaker(
                webSocketURI,
                webSocketVersion,
                subprotocol,
                allowExtensions,
                new DefaultHttpHeaders(),
                maxFramePayloadLength);
        WebSocketClientHandler handler = new WebSocketClientHandler(handshaker, maxFramePayloadLength, messageAggregation, eventsSubject);
        pipeline.addLast(new HttpClientCodec());
        pipeline.addLast(new HttpObjectAggregator(8192));
        pipeline.addLast(handler);
    }
}
