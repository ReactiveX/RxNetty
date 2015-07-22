package io.reactivex.netty.pipeline;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

@Sharable
public class PrimitiveConversionHandler extends ChannelOutboundHandlerAdapter {

    public static final PrimitiveConversionHandler INSTANCE = new PrimitiveConversionHandler();

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        Object msgToWrite = msg;

        if (msg instanceof String) {
            msgToWrite = ctx.alloc().buffer().writeBytes(((String) msg).getBytes());
        } else if (msg instanceof byte[]) {
            msgToWrite = ctx.alloc().buffer().writeBytes((byte[]) msg);
        } else if (msg instanceof DelayedTransformationMessage) {
            msgToWrite = ((DelayedTransformationMessage) msg).getTransformed(ctx.alloc());
        }

        super.write(ctx, msgToWrite, promise);
    }

    public interface DelayedTransformationMessage {

        Object getTransformed(ByteBufAllocator allocator);

    }
}
