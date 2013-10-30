package rx.netty.experimental.impl;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.subjects.PublishSubject;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;
import rx.util.functions.Action1;

public class TcpConnection<I, O> {

    private final PublishSubject<I> s;
    private final ChannelHandlerContext ctx;

    protected TcpConnection(ChannelHandlerContext ctx, final PublishSubject<I> s) {
        this.ctx = ctx;
        this.s = s;
    }

    public Observable<I> getChannelObservable() {
        return s;
    }

    /* package */Observer<I> getChannelObserver() {
        return new Observer<I>() {
            public synchronized void onCompleted() {
                s.onCompleted();
            }

            public synchronized void onError(Throwable e) {
                s.onError(e);
            }

            public void onNext(I o) {
                s.onNext(o);
            }
        };
    }

    public static <I, O> TcpConnection<I, O> create(ChannelHandlerContext ctx) {
        return new TcpConnection<I, O>(ctx, PublishSubject.<I> create());
    }

    /**
     * Note that this eagerly subscribes. It is NOT lazy.
     * 
     * @param msg
     * @return
     */
    public Observable<Void> write(final O msg) {
        Observable<Void> o = Observable.create(new OnSubscribeFunc<Void>() {

            @Override
            public Subscription onSubscribe(final Observer<? super Void> observer) {
                final ChannelFuture f = ctx.writeAndFlush(msg);

                f.addListener(new GenericFutureListener<Future<Void>>() {

                    @Override
                    public void operationComplete(Future<Void> future) throws Exception {
                        if (future.isSuccess()) {
                            observer.onCompleted();
                        } else {
                            observer.onError(future.cause());
                        }
                    }
                });

                return Subscriptions.create(new Action0() {

                    @Override
                    public void call() {
                        f.cancel(true);
                    }

                });
            }
        }).cache();

        o.subscribe(new Action1<Void>() {

            @Override
            public void call(Void t1) {
                // do nothing, we are eagerly executing the Observable
            }
        }, new Action1<Throwable>() {

            @Override
            public void call(Throwable cause) {
                // do nothing, it will be cached for the other subscribers
            }

        });

        // return Observable that is multicast (cached) so it can be subscribed to it wanted
        return o;

    }

}
