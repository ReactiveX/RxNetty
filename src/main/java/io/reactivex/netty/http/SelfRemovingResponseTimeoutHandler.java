package io.reactivex.netty.http;

import java.util.concurrent.TimeUnit;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.ReadTimeoutHandler;

public class SelfRemovingResponseTimeoutHandler extends ReadTimeoutHandler {

    public static final String NAME = "responseTimeoutHandler";
    
    public SelfRemovingResponseTimeoutHandler(int timeoutSeconds) {
        super(timeoutSeconds);
    }

    public SelfRemovingResponseTimeoutHandler(long timeout, TimeUnit unit) {
        super(timeout, unit);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {
        super.channelRead(ctx, msg);
        if (ctx.pipeline().get(NAME) != null) {
            ctx.pipeline().remove(NAME);
        }
    }
}
