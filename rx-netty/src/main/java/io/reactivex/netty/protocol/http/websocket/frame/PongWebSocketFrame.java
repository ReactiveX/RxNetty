package io.reactivex.netty.protocol.http.websocket.frame;

import io.netty.buffer.ByteBuf;

/**
 * @author Tomasz Bak
 */
public class PongWebSocketFrame extends WebSocketFrame {
    PongWebSocketFrame(io.netty.handler.codec.http.websocketx.PongWebSocketFrame nettyFrame) {
        super(nettyFrame);
    }

    public PongWebSocketFrame() {
        super(new io.netty.handler.codec.http.websocketx.PongWebSocketFrame());
    }

    public PongWebSocketFrame(ByteBuf binaryData) {
        super(new io.netty.handler.codec.http.websocketx.PongWebSocketFrame(binaryData));
    }

    public PongWebSocketFrame(boolean finalFragment, int rsv, ByteBuf binaryData) {
        super(new io.netty.handler.codec.http.websocketx.PongWebSocketFrame(finalFragment, rsv, binaryData));
    }
}
