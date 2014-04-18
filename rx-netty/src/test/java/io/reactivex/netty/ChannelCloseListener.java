package io.reactivex.netty;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
* @author Nitesh Kant
*/
@ChannelHandler.Sharable
public class ChannelCloseListener extends ChannelInboundHandlerAdapter {

    private final AtomicBoolean unregisteredCalled = new AtomicBoolean();
    private final Object closeMonitor = new Object();

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
        unregisteredCalled.set(true);
        synchronized (closeMonitor) {
            closeMonitor.notifyAll();
        }
    }

    public boolean channelCloseReceived() {
        return unregisteredCalled.get();
    }

    public void reset() {
        unregisteredCalled.set(false);
    }

    public boolean waitForClose(long timeout, TimeUnit timeUnit) throws InterruptedException {
        synchronized (closeMonitor) {
            if (unregisteredCalled.get()) {
                return true;
            }
            closeMonitor.wait(TimeUnit.MILLISECONDS.convert(timeout, timeUnit));
            return unregisteredCalled.get();
        }
    }
}
