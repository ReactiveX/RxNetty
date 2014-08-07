package io.reactivex.netty.protocol.http.websocket;

import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.reactivex.netty.channel.ConnectionHandler;
import io.reactivex.netty.metrics.MetricEventsListener;
import io.reactivex.netty.metrics.MetricEventsListenerFactory;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.server.ConnectionBasedServerBuilder;
import io.reactivex.netty.server.RxServer;
import io.reactivex.netty.server.ServerMetricsEvent;

/**
 * @author Tomasz Bak
 */
public class WebSocketServerBuilder<T extends WebSocketFrame> extends ConnectionBasedServerBuilder<T, T, WebSocketServerBuilder<T>> {
    private String webSocketURI;
    private String subprotocols;
    private boolean allowExtensions;
    private int maxFramePayloadLength = 65536;
    private boolean messageAggregator;

    public WebSocketServerBuilder(int port, ConnectionHandler<T, T> connectionHandler) {
        super(port, connectionHandler);
    }

    @Override
    protected RxServer<T, T> createServer() {
        PipelineConfigurator<T, T> webSocketPipeline = new WebSocketServerPipelineConfigurator<T, T>(webSocketURI,
                subprotocols, allowExtensions, maxFramePayloadLength, messageAggregator);
        if (getPipelineConfigurator() != null) {
            appendPipelineConfigurator(webSocketPipeline);
        } else {
            pipelineConfigurator(webSocketPipeline);
        }
        return new WebSocketServer<T>(serverBootstrap, port, pipelineConfigurator, connectionHandler, eventExecutorGroup);
    }

    public WebSocketServerBuilder<T> withWebSocketURI(String uri) {
        webSocketURI = uri;
        return this;
    }

    public WebSocketServerBuilder<T> withSubprotocol(String subprotocols) {
        this.subprotocols = subprotocols;
        return this;
    }

    public WebSocketServerBuilder<T> withAllowExtensions(boolean allowExtensions) {
        this.allowExtensions = allowExtensions;
        return this;
    }

    public WebSocketServerBuilder<T> withMaxFramePayloadLength(int maxFramePayloadLength) {
        this.maxFramePayloadLength = maxFramePayloadLength;
        return this;
    }

    public WebSocketServerBuilder<T> withMessageAggregator(boolean messageAggregator) {
        this.messageAggregator = messageAggregator;
        return this;
    }

    @Override
    protected MetricEventsListener<? extends ServerMetricsEvent<? extends Enum>> newMetricsListener(MetricEventsListenerFactory factory, RxServer<T, T> server) {
        return null;
    }
}
