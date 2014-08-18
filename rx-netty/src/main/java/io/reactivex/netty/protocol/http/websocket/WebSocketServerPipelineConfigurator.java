package io.reactivex.netty.protocol.http.websocket;

import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.reactivex.netty.metrics.MetricEventsSubject;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.server.ServerMetricsEvent;

/**
 * Initial channel setup contains HTTP handlers together with {@link WebSocketServerHandler}
 * for WebSocket handshake orchestration. Once handshake is done, the channel is dynamically reconfigured.
 *
 * @author Tomasz Bak
 */
public class WebSocketServerPipelineConfigurator<R, W> implements PipelineConfigurator<R, W> {
    private final String webSocketURI;
    private final String subprotocols;
    private final boolean allowExtensions;
    private final int maxFramePayloadLength;
    private final boolean messageAggregator;
    private MetricEventsSubject<ServerMetricsEvent<?>> eventsSubject;

    public WebSocketServerPipelineConfigurator(String webSocketURI, String subprotocols,
                                               boolean allowExtensions, int maxFramePayloadLength,
                                               boolean messageAggregator) {
        this.webSocketURI = webSocketURI;
        this.subprotocols = subprotocols;
        this.allowExtensions = allowExtensions;
        this.maxFramePayloadLength = maxFramePayloadLength;
        this.messageAggregator = messageAggregator;
    }

    @Override
    public void configureNewPipeline(ChannelPipeline pipeline) {
        WebSocketServerHandshakerFactory handshakeHandlerFactory = new WebSocketServerHandshakerFactory(
                webSocketURI, subprotocols, allowExtensions, maxFramePayloadLength);

        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(65536));
        pipeline.addLast(new WebSocketServerHandler(
                handshakeHandlerFactory, maxFramePayloadLength, messageAggregator, eventsSubject));
    }

    void useMetricEventsSubject(MetricEventsSubject<ServerMetricsEvent<?>> eventsSubject) {
        this.eventsSubject = eventsSubject;
    }
}
