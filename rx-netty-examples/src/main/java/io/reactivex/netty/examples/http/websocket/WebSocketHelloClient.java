package io.reactivex.netty.examples.http.websocket;

import java.util.concurrent.TimeUnit;

import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.protocol.http.websocket.WebSocketClient;
import rx.Notification;
import rx.Observable;
import rx.functions.Func1;

import static io.reactivex.netty.examples.http.websocket.WebSocketHelloServer.*;

/**
 * @author Tomasz Bak
 */
public class WebSocketHelloClient {

    private final int port;

    public WebSocketHelloClient(int port) {
        this.port = port;
    }

    public void sendHelloRequest() {
        WebSocketClient<TextWebSocketFrame> rxClient = RxNetty.<TextWebSocketFrame>newWebSocketClientBuilder("localhost", port)
                .withWebSocketURI("/websocket")
                .withWebSocketVersion(WebSocketVersion.V13)
                .build();

        Notification<Void> result = rxClient.connect()
                .flatMap(new Func1<ObservableConnection<TextWebSocketFrame, TextWebSocketFrame>, Observable<Void>>() {
                    @Override
                    public Observable<Void> call(final ObservableConnection<TextWebSocketFrame, TextWebSocketFrame> connection) {
                        return Observable.concat(
                                connection.writeAndFlush(new TextWebSocketFrame("Hello!!!")),
                                connection.getInput().flatMap(new Func1<WebSocketFrame, Observable<Void>>() {
                                    @Override
                                    public Observable<Void> call(WebSocketFrame webSocketFrame) {
                                        System.out.println("Got back: " + webSocketFrame);
                                        return Observable.timer(1, TimeUnit.SECONDS).flatMap(new Func1<Long, Observable<Void>>() {
                                            @Override
                                            public Observable<Void> call(Long aLong) {
                                                return connection.writeAndFlush(new TextWebSocketFrame("Hello!!!"));
                                            }
                                        });
                                    }
                                }));
                    }
                }).materialize().toBlocking().last();

        if (result.isOnError()) {
            System.out.println("ERROR: " + result.getThrowable());
            result.getThrowable().printStackTrace();
        }
    }

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }
        new WebSocketHelloClient(port).sendHelloRequest();
    }

}
