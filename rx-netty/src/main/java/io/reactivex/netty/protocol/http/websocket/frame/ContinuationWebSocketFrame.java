package io.reactivex.netty.protocol.http.websocket.frame;

import io.netty.buffer.ByteBuf;

/**
 * @author Tomasz Bak
 */
public class ContinuationWebSocketFrame extends WebSocketFrame {

    ContinuationWebSocketFrame(io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame nettyFrame) {
        super(nettyFrame);
    }

    public ContinuationWebSocketFrame(boolean finalFragment, int rsv, String text) {
        super(new io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame(finalFragment, rsv, text));
    }

    public ContinuationWebSocketFrame(ByteBuf binaryData) {
        super(new io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame(binaryData));
    }

    public ContinuationWebSocketFrame(boolean finalFragment, int rsv, ByteBuf binaryData) {
        super(new io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame(finalFragment, rsv, binaryData));
    }
}
