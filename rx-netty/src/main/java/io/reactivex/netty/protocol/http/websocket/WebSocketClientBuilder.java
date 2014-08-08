package io.reactivex.netty.protocol.http.websocket;

import java.net.URI;
import java.net.URISyntaxException;

import io.netty.bootstrap.Bootstrap;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.client.AbstractClientBuilder;
import io.reactivex.netty.client.ClientChannelFactory;
import io.reactivex.netty.client.ClientChannelFactoryImpl;
import io.reactivex.netty.client.ClientConnectionFactory;
import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.client.UnpooledClientConnectionFactory;
import io.reactivex.netty.metrics.MetricEventsListener;
import io.reactivex.netty.metrics.MetricEventsListenerFactory;
import io.reactivex.netty.pipeline.PipelineConfigurator;

/**
 * @author Tomasz Bak
 */
public class WebSocketClientBuilder<I extends WebSocketFrame, O extends WebSocketFrame> extends AbstractClientBuilder<I, O, WebSocketClientBuilder<I, O>, WebSocketClient<I, O>> {

    private URI webSocketURI = URI.create("/");
    private WebSocketVersion webSocketVersion = WebSocketVersion.V13;
    private boolean messageAggregation;
    private String subprotocol;
    private boolean allowExtensions;
    private int maxFramePayloadLength = 65536;


    public WebSocketClientBuilder(String host, int port) {
        this(host, port, new Bootstrap());
    }

    public WebSocketClientBuilder(String host, int port, Bootstrap bootstrap) {
        this(bootstrap, host, port, new UnpooledClientConnectionFactory<O, I>(),
                new ClientChannelFactoryImpl<O, I>(bootstrap));
    }

    public WebSocketClientBuilder(Bootstrap bootstrap, String host, int port,
                                  ClientConnectionFactory<O, I, ? extends ObservableConnection<O, I>> connectionFactory,
                                  ClientChannelFactory<O, I> factory) {
        super(bootstrap, host, port, connectionFactory, factory);
    }

    @Override
    protected WebSocketClient<I, O> createClient() {
        PipelineConfigurator<O, I> webSocketPipeline = new WebSocketClientPipelineConfigurator<O, I>(
                webSocketURI, webSocketVersion, subprotocol, allowExtensions,
                maxFramePayloadLength, messageAggregation, eventsSubject);
        if (getPipelineConfigurator() != null) {
            appendPipelineConfigurator(webSocketPipeline);
        } else {
            pipelineConfigurator(webSocketPipeline);
        }
        return new WebSocketClient<I, O>(getOrCreateName(), serverInfo, bootstrap, pipelineConfigurator, clientConfig,
                channelFactory, connectionFactory, eventsSubject);
    }

    @Override
    protected String generatedNamePrefix() {
        return "WebSocketClient-";
    }

    public WebSocketClientBuilder<I, O> withWebSocketURI(String uri) {
        try {
            webSocketURI = new URI(uri);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
        return this;
    }

    public WebSocketClientBuilder<I, O> withWebSocketVersion(WebSocketVersion version) {
        webSocketVersion = version;
        return this;
    }

    public WebSocketClientBuilder<I, O> withMessageAggregator(boolean messageAggregation) {
        this.messageAggregation = messageAggregation;
        return this;
    }

    public WebSocketClientBuilder<I, O> withSubprotocol(String subprotocol) {
        this.subprotocol = subprotocol;
        return this;
    }

    public WebSocketClientBuilder<I, O> withAllowExtensions(boolean allowExtensions) {
        this.allowExtensions = allowExtensions;
        return this;
    }

    public WebSocketClientBuilder<I, O> withMaxFramePayloadLength(int maxFramePayloadLength) {
        this.maxFramePayloadLength = maxFramePayloadLength;
        return this;
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected MetricEventsListener<? extends ClientMetricsEvent<? extends Enum>> newMetricsListener(MetricEventsListenerFactory factory, WebSocketClient<I, O> client) {
        return factory.forWebSocketClient(client);
    }
}
