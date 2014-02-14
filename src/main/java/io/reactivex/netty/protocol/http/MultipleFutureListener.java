package io.reactivex.netty.protocol.http;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import rx.Observable;
import rx.subjects.PublishSubject;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Nitesh Kant
 */
public class MultipleFutureListener implements ChannelFutureListener {

    private final ChannelPromise finalPromise;

    private final AtomicInteger listeningToCount = new AtomicInteger();
    private final PublishSubject<ChannelFuture> lastCompletedFuture; // This never completes or throw an error.
    private final ChannelFuture futureWhenNoPendingFutures;

    public MultipleFutureListener(ChannelPromise finalPromise) {
        if (null == finalPromise) {
            throw new NullPointerException("Promise can not be null.");
        }
        this.finalPromise = finalPromise;
        lastCompletedFuture = PublishSubject.create();
        futureWhenNoPendingFutures = finalPromise.channel().newPromise().setSuccess();
    }

    public MultipleFutureListener(ChannelHandlerContext ctx) {
        finalPromise = null;
        lastCompletedFuture = PublishSubject.create();
        futureWhenNoPendingFutures = ctx.newPromise();
    }

    public void listen(ChannelFuture future) {
        listeningToCount.incrementAndGet();
        future.addListener(this);
    }

    public Observable<ChannelFuture> listenForNextCompletion() {
        if (listeningToCount.get() > 0) {
            return lastCompletedFuture;
        } else {
            return Observable.<ChannelFuture>from(futureWhenNoPendingFutures);
        }
    }

    @Override
    public void operationComplete(ChannelFuture future) throws Exception {
        int nowListeningTo = listeningToCount.decrementAndGet();
        if (!future.isSuccess()) {
            if (null != finalPromise) {
                finalPromise.tryFailure(future.cause());// TODO: Cancel pending futures (good to have)
            }
        } else if (nowListeningTo <= 0) {
            if (null != finalPromise) {
                finalPromise.trySuccess(null);
            }
            lastCompletedFuture.onNext(future);
        }
    }
}
