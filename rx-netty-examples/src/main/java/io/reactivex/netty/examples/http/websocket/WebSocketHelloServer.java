package io.reactivex.netty.examples.http.websocket;

import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.ConnectionHandler;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.protocol.http.websocket.frame.TextWebSocketFrame;
import io.reactivex.netty.protocol.http.websocket.frame.WebSocketFrame;
import io.reactivex.netty.server.RxServer;
import rx.Observable;
import rx.functions.Func1;

/**
 * @author Tomasz Bak
 */
public class WebSocketHelloServer {

    static final int DEFAULT_PORT = 8090;

    private final int port;

    public WebSocketHelloServer(int port) {
        this.port = port;
    }

    public RxServer<WebSocketFrame, WebSocketFrame> createServer() {
        RxServer<WebSocketFrame, WebSocketFrame> server = RxNetty.newWebSocketServerBuilder(port, new ConnectionHandler<WebSocketFrame, WebSocketFrame>() {
            @Override
            public Observable<Void> handle(final ObservableConnection<WebSocketFrame, WebSocketFrame> newConnection) {
                return newConnection.getInput().flatMap(new Func1<WebSocketFrame, Observable<Void>>() {
                    @Override
                    public Observable<Void> call(WebSocketFrame wsFrame) {
                        TextWebSocketFrame textFrame = (TextWebSocketFrame) wsFrame;
                        System.out.println("Got message: " + textFrame.text());
                        return newConnection.writeAndFlush(new TextWebSocketFrame(textFrame.text().toUpperCase()));
                    }
                });
            }
        }).enableWireLogging(LogLevel.ERROR).build();

        System.out.println("WebSocket server started...");
        return server;
    }

    public static void main(final String[] args) {
        new WebSocketHelloServer(DEFAULT_PORT).createServer().startAndWait();
    }
}
