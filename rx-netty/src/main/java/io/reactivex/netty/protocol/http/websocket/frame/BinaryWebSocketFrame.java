package io.reactivex.netty.protocol.http.websocket.frame;

import io.netty.buffer.ByteBuf;

/**
 * @author Tomasz Bak
 */
public class BinaryWebSocketFrame extends WebSocketFrame {

    BinaryWebSocketFrame(io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame nettyFrame) {
        super(nettyFrame);
    }

    public BinaryWebSocketFrame(ByteBuf binaryData) {
        super(new io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame(binaryData));
    }

    public BinaryWebSocketFrame(boolean finalFragment, int rsv, ByteBuf binaryData) {
        super(new io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame(finalFragment, rsv, binaryData));
    }
}
