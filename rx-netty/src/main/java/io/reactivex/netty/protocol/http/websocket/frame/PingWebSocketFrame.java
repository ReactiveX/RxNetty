package io.reactivex.netty.protocol.http.websocket.frame;

import io.netty.buffer.ByteBuf;

/**
 * @author Tomasz Bak
 */
public class PingWebSocketFrame extends WebSocketFrame {
    PingWebSocketFrame(io.netty.handler.codec.http.websocketx.PingWebSocketFrame nettyFrame) {
        super(nettyFrame);
    }

    public PingWebSocketFrame() {
        super(new io.netty.handler.codec.http.websocketx.PingWebSocketFrame());
    }

    public PingWebSocketFrame(ByteBuf binaryData) {
        super(new io.netty.handler.codec.http.websocketx.PingWebSocketFrame(binaryData));
    }

    public PingWebSocketFrame(boolean finalFragment, int rsv, ByteBuf binaryData) {
        super(new io.netty.handler.codec.http.websocketx.PingWebSocketFrame(finalFragment, rsv, binaryData));
    }
}
