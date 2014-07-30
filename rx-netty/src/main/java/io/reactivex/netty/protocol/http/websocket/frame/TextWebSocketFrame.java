package io.reactivex.netty.protocol.http.websocket.frame;

/**
 * @author Tomasz Bak
 */
public class TextWebSocketFrame extends WebSocketFrame {

    TextWebSocketFrame(io.netty.handler.codec.http.websocketx.TextWebSocketFrame nettyFrame) {
        super(nettyFrame);
    }

    public TextWebSocketFrame(String text) {
        super(new io.netty.handler.codec.http.websocketx.TextWebSocketFrame(text));
    }

    public TextWebSocketFrame(boolean finalFragment, int rsv, String text) {
        super(new io.netty.handler.codec.http.websocketx.TextWebSocketFrame(finalFragment, rsv, text));
    }

    public String text() {
        return ((io.netty.handler.codec.http.websocketx.TextWebSocketFrame) nettyFrame).text();
    }

    @Override
    public String toString() {
        return "{finalFragment=" + isFinalFragment() + ", rsv=" + rsv() + ", text=" + text() + '}';
    }
}
