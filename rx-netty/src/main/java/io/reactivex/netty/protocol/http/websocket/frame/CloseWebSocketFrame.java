package io.reactivex.netty.protocol.http.websocket.frame;

/**
 * @author Tomasz Bak
 */
public class CloseWebSocketFrame extends WebSocketFrame {

    CloseWebSocketFrame(io.netty.handler.codec.http.websocketx.CloseWebSocketFrame nettyFrame) {
        super(nettyFrame);
    }

    public CloseWebSocketFrame(boolean finalFragment, int rsv, int statusCode, String reasonText) {
        super(new io.netty.handler.codec.http.websocketx.CloseWebSocketFrame(finalFragment, rsv, statusCode, reasonText));
    }

    public CloseWebSocketFrame(int statusCode, String reasonText) {
        super(new io.netty.handler.codec.http.websocketx.CloseWebSocketFrame(statusCode, reasonText));
    }

    public int statusCode() {
        return ((io.netty.handler.codec.http.websocketx.CloseWebSocketFrame) nettyFrame).statusCode();
    }

    public String reasonText() {
        return ((io.netty.handler.codec.http.websocketx.CloseWebSocketFrame) nettyFrame).reasonText();
    }

    @Override
    public String toString() {
        return "{finalFragment=" + isFinalFragment() + ", rsv=" + rsv() + ", statusCode=" + statusCode()
                + ". reasonText=" + reasonText() + '}';
    }
}
