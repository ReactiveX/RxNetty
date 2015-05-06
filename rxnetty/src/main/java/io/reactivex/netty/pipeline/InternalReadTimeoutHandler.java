package io.reactivex.netty.pipeline;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * A copy of netty's {@link ReadTimeoutHandler}. This is required because {@link ReadTimeoutHandler} does not allow
 * reuse in the same pipeline, which is required for connection pooling.
 * See issue https://github.com/ReactiveX/RxNetty/issues/344
 */
class InternalReadTimeoutHandler extends ChannelDuplexHandler {

    private static final Logger logger = LoggerFactory.getLogger(InternalReadTimeoutHandler.class);


    private static final long MIN_TIMEOUT_NANOS = TimeUnit.MILLISECONDS.toNanos(1);

    private final long timeoutNanos;

    private volatile ScheduledFuture<?> timeout;
    private volatile long lastReadTime;

    private enum State {
        Created,
        Active,
        Paused,
        Destroyed
    }

    private volatile State state = State.Created;

    private boolean closed;

    /**
     * Creates a new instance.
     *
     * @param timeout
     *        read timeout
     * @param unit
     *        the {@link TimeUnit} of {@code timeout}
     */
    public InternalReadTimeoutHandler(long timeout, TimeUnit unit) {
        if (unit == null) {
            throw new NullPointerException("unit");
        }

        if (timeout <= 0) {
            timeoutNanos = 0;
        } else {
            timeoutNanos = Math.max(unit.toNanos(timeout), MIN_TIMEOUT_NANOS);
        }
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        if (ctx.channel().isActive() && ctx.channel().isRegistered()) {
            // channelActive() event has been fired already, which means this.channelActive() will
            // not be invoked. We have to scheduleAfresh here instead.
            scheduleAfresh(ctx);
        } else {
            // channelActive() event has not been fired yet.  this.channelActive() will be invoked
            // and initialization will occur there.
        }
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        destroy();
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        // Initialize early if channel is active already.
        if (ctx.channel().isActive()) {
            scheduleAfresh(ctx);
        }
        super.channelRegistered(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // This method will be invoked only if this handler was added
        // before channelActive() event is fired.  If a user adds this handler
        // after the channelActive() event, scheduleAfresh() will be called by beforeAdd().
        scheduleAfresh(ctx);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        destroy();
        super.channelInactive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        lastReadTime = System.nanoTime();
        ctx.fireChannelRead(msg);
    }

    @Override
    public void write(final ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (State.Paused == state) {
            // Add the timeout handler when write is complete.
            promise.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (State.Paused == state) {
                        /*
                         * Multiple writes can all add a listener, till it is active again (on write success), so it is
                         * required to only schedule next when the state is actually paused.
                         */
                        scheduleAfresh(ctx);
                    }
                }
            });
        }

        super.write(ctx, msg, promise);
    }

    void cancelTimeoutSchedule(ChannelHandlerContext ctx) {
        assert ctx.channel().eventLoop().inEventLoop(); /*should only be called from the owner eventloop*/
        if (State.Active == state) {
            state = State.Paused;
            timeout.cancel(false);
        }
    }

    private void scheduleAfresh(ChannelHandlerContext ctx) {
        // Avoid the case where destroy() is called before scheduling timeouts.
        // See: https://github.com/netty/netty/issues/143
        switch (state) {
        case Created:
            break;
        case Active:
            logger.warn("Not scheduling next read timeout task as it is already active.");
            return;
        case Paused:
            break;
        case Destroyed:
            logger.warn("Not scheduling next read timeout task as the channel handler is removed.");
            return;
        }

        state = State.Active;

        lastReadTime = System.nanoTime();
        if (timeoutNanos > 0) {
            timeout = ctx.executor().schedule(new ReadTimeoutTask(ctx), timeoutNanos, TimeUnit.NANOSECONDS);
        }
    }

    private void destroy() {
        state = State.Destroyed;

        if (timeout != null) {
            timeout.cancel(false);
            timeout = null;
        }
    }

    /**
     * Is called when a read timeout was detected.
     */
    protected void readTimedOut(ChannelHandlerContext ctx) throws Exception {
        if (!closed) {
            ctx.fireExceptionCaught(ReadTimeoutException.INSTANCE);
            ctx.close();
            closed = true;
        }
    }

    private final class ReadTimeoutTask implements Runnable {

        private final ChannelHandlerContext ctx;

        ReadTimeoutTask(ChannelHandlerContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public void run() {
            if (!ctx.channel().isOpen()) {
                return;
            }

            long currentTime = System.nanoTime();
            long nextDelay = timeoutNanos - (currentTime - lastReadTime);
            if (nextDelay <= 0) {
                // Read timed out - set a new timeout and notify the callback.
                timeout = ctx.executor().schedule(this, timeoutNanos, TimeUnit.NANOSECONDS);
                try {
                    readTimedOut(ctx);
                } catch (Throwable t) {
                    ctx.fireExceptionCaught(t);
                }
            } else {
                // Read occurred before the timeout - set a new timeout with shorter delay.
                timeout = ctx.executor().schedule(this, nextDelay, TimeUnit.NANOSECONDS);
            }
        }
    }
}
