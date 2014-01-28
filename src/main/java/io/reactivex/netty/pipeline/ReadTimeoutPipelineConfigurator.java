package io.reactivex.netty.pipeline;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.handler.timeout.ReadTimeoutHandler;

import java.util.concurrent.TimeUnit;

/**
 * An implementation of {@link PipelineConfigurator} to configure a read time handler. <br/>
 * A read timeout is defined as lack of bytes read from the channel over the specified period. <br/>
 * This configurator, adds the {@link ReadTimeoutHandler} after every write, if not present.
 *
 * <h1>Reusable connections and timeout</h1>
 *
 * In cases where the connection is reused (like HTTP persistent connections), it is the responsibility of the protocol
 * to remove this timeout handler, for not being timed out (resulting in connection close) due to inactivity when the
 * connection is not in use.
 *
 * @see {@link ReadTimeoutHandler}
 *
 * @author Nitesh Kant
 */
@ChannelHandler.Sharable
public class ReadTimeoutPipelineConfigurator implements PipelineConfigurator<Object, Object> {

    public static final String READ_TIMEOUT_HANDLER_NAME = "readtimeout-handler";
    public static final String READ_TIMEOUT_LIFECYCLE_MANAGER_HANDLER_NAME = "readtimeout-handler-lifecycle-manager";
    private final long timeout;
    private final TimeUnit timeUnit;

    public ReadTimeoutPipelineConfigurator(long timeout, TimeUnit timeUnit) {
        this.timeout = timeout;
        this.timeUnit = timeUnit;
    }

    @Override
    public void configureNewPipeline(ChannelPipeline pipeline) {
        pipeline.addFirst(READ_TIMEOUT_LIFECYCLE_MANAGER_HANDLER_NAME, new ReadTimeoutHandlerLifecycleManager());
    }

    public static void removeTimeoutHandler(ChannelPipeline pipeline) {
        if (pipeline.get(READ_TIMEOUT_HANDLER_NAME) != null) {
            pipeline.remove(READ_TIMEOUT_HANDLER_NAME);
        }
    }

    @ChannelHandler.Sharable
    private class ReadTimeoutHandlerLifecycleManager extends ChannelOutboundHandlerAdapter {

        @Override
        public void write(final ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            // Add the timeout handler when write is complete.
            promise.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (null == ctx.pipeline().get(READ_TIMEOUT_HANDLER_NAME)) {
                        ctx.pipeline().addFirst(READ_TIMEOUT_HANDLER_NAME, new ReadTimeoutHandler(timeout, timeUnit));
                    }
                }
            });
            super.write(ctx, msg, promise);
        }
    }
}
