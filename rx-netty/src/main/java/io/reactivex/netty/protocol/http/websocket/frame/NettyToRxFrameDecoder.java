package io.reactivex.netty.protocol.http.websocket.frame;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

/**
 * @author Tomasz Bak
 */
public class NettyToRxFrameDecoder extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof io.netty.handler.codec.http.websocketx.WebSocketFrame) {
            try {
                ctx.fireChannelRead(WebSocketFrame.from((io.netty.handler.codec.http.websocketx.WebSocketFrame) msg));
            } finally {
                ReferenceCountUtil.release(msg);
            }
        } else {
            super.channelRead(ctx, msg);
        }
    }
}
