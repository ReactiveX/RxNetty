package io.reactivex.netty;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.reactivex.netty.spi.NettyPipelineConfigurator;
import rx.Observer;

import java.util.concurrent.TimeUnit;

/**
 * An implementation of {@link NettyPipelineConfigurator} to configure a read time handler. <br/>
 * A read timeout is defined as lack of bytes read from the channel over the specified period. <br/>
 * This configurator, adds the {@link ReadTimeoutHandler} after every write and is removed after the {@link Observer}
 * associated with the response recieving, i.e., {@link ObservableConnection#getInput()} is completed.
 *
 * @see {@link ReadTimeoutHandler}
 *
 * @author Nitesh Kant
 */
public class ReadTimeoutPipelineConfigurator implements NettyPipelineConfigurator {

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
        pipeline.addFirst(READ_TIMEOUT_LIFECYCLE_MANAGER_HANDLER_NAME,
                          new ReadTimeoutHandlerLifecycleManager());
    }

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
