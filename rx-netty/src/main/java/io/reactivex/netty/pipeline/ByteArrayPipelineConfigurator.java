package io.reactivex.netty.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;

/**
 * @author Nitesh Kant
 */
public class ByteArrayPipelineConfigurator implements PipelineConfigurator<byte[], byte[]> {

    @Override
    public void configureNewPipeline(ChannelPipeline pipeline) {
        pipeline.addLast(new ChannelDuplexHandler() {

            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                boolean handled = false;

                if (ByteBuf.class.isAssignableFrom(msg.getClass())) {
                    ByteBuf byteBuf = (ByteBuf) msg;
                    if (byteBuf.isReadable()) {
                        int readableBytes = byteBuf.readableBytes();
                        byte[] msgToPass = new byte[readableBytes];
                        byteBuf.readBytes(msgToPass);
                        handled = true;
                        ctx.fireChannelRead(msgToPass);
                    }
                }
                if (!handled) {
                    super.channelRead(ctx, msg);
                }
            }

            @Override
            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                if (msg instanceof byte[]) {
                    byte[] msgAsBytes = (byte[]) msg;
                    ByteBuf byteBuf = ctx.alloc().buffer(msgAsBytes.length).writeBytes(msgAsBytes);
                    super.write(ctx, byteBuf, promise);
                } else {
                    super.write(ctx, msg, promise); // pass-through
                }
            }
        });
    }
}
