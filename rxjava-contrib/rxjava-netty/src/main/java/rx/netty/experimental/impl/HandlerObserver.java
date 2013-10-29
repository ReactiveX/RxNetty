package rx.netty.experimental.impl;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import rx.Observer;

public class HandlerObserver<I, O> extends ChannelInboundHandlerAdapter {

    private final Observer<? super TcpConnection<I, O>> observer;
    private volatile TcpConnection<I, O> connection;

    public HandlerObserver(Observer<? super TcpConnection<I, O>> observer) {
        this.observer = observer;
    }

    // suppressing because Netty uses Object but we have typed HandlerObserver to I and expect only I
    @SuppressWarnings("unchecked")
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        connection.getChannelObserver().onNext((I) msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (connection != null) {
            connection.getChannelObserver().onError(cause);
        } else {
            observer.onError(new RuntimeException("Error occurred and connection does not exist: " + cause));
        }
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
        connection.getChannelObserver().onCompleted();
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        connection = TcpConnection.create(ctx);
        observer.onNext(connection);
    }

}