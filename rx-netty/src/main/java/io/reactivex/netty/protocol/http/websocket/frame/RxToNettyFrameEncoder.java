package io.reactivex.netty.protocol.http.websocket.frame;

import java.util.List;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

/**
 * @author Tomasz Bak
 */
public class RxToNettyFrameEncoder extends MessageToMessageEncoder<WebSocketFrame> {

    @Override
    protected void encode(ChannelHandlerContext ctx, WebSocketFrame msg, List<Object> out) throws Exception {
        out.add(msg.getNettyFrame());
    }
}
