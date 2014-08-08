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
public class WebSocketServerBuilder<I extends WebSocketFrame, O extends WebSocketFrame> extends ConnectionBasedServerBuilder<I, O, WebSocketServerBuilder<I, O>> {
    private String webSocketURI;
    private String subprotocols;
    private boolean allowExtensions;
    private int maxFramePayloadLength = 65536;
    private boolean messageAggregator;

    public WebSocketServerBuilder(int port, ConnectionHandler<I, O> connectionHandler) {
        super(port, connectionHandler);
    }

    @Override
    public WebSocketServer<I, O> build() {
        return (WebSocketServer<I, O>) super.build();
    }

    @Override
    protected RxServer<I, O> createServer() {
        PipelineConfigurator<I, O> webSocketPipeline = new WebSocketServerPipelineConfigurator<I, O>(webSocketURI,
                subprotocols, allowExtensions, maxFramePayloadLength, messageAggregator);
        if (getPipelineConfigurator() != null) {
            appendPipelineConfigurator(webSocketPipeline);
        } else {
            pipelineConfigurator(webSocketPipeline);
        }
        return new WebSocketServer<I, O>(serverBootstrap, port, pipelineConfigurator, connectionHandler, eventExecutorGroup);
    }

    public WebSocketServerBuilder<I, O> withWebSocketURI(String uri) {
        webSocketURI = uri;
        return this;
    }

    public WebSocketServerBuilder<I, O> withSubprotocol(String subprotocols) {
        this.subprotocols = subprotocols;
        return this;
    }

    public WebSocketServerBuilder<I, O> withAllowExtensions(boolean allowExtensions) {
        this.allowExtensions = allowExtensions;
        return this;
    }

    public WebSocketServerBuilder<I, O> withMaxFramePayloadLength(int maxFramePayloadLength) {
        this.maxFramePayloadLength = maxFramePayloadLength;
        return this;
    }

    public WebSocketServerBuilder<I, O> withMessageAggregator(boolean messageAggregator) {
        this.messageAggregator = messageAggregator;
        return this;
    }

    @Override
    protected MetricEventsListener<? extends ServerMetricsEvent<?>> newMetricsListener(MetricEventsListenerFactory factory, RxServer<I, O> server) {
        return factory.forWebSocketServer((WebSocketServer<I, O>) server);
    }
}
