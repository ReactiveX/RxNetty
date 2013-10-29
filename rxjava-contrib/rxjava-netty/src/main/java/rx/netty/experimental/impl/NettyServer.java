package rx.netty.experimental.impl;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.netty.experimental.RxNetty;
import rx.netty.experimental.protocol.ProtocolHandler;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;

public class NettyServer<I, O> {

    public static <I, O> Observable<TcpConnection<I, O>> createServer(
        final int port,
        final EventLoopGroup acceptorEventLoops,
        final EventLoopGroup workerEventLoops,
        final ProtocolHandler<I, O> handler) {

        return Observable.create(new OnSubscribeFunc<TcpConnection<I, O>>() {

            @Override
            public Subscription onSubscribe(final Observer<? super TcpConnection<I, O>> observer) {
                try {
                    ServerBootstrap b = new ServerBootstrap();
                    b.group(acceptorEventLoops, workerEventLoops)
                            .channel(NioServerSocketChannel.class)
                            // TODO allow ChannelOptions to be passed in
                            .option(ChannelOption.SO_BACKLOG, 100)
                            .handler(new ParentHandler())
                            .childHandler(new ChannelInitializer<SocketChannel>() {
                                @Override
                                public void initChannel(SocketChannel ch) throws Exception {
                                    handler.configure(ch.pipeline());

                                    // add the handler that will emit responses to the observer
                                    ch.pipeline().addLast(new HandlerObserver<I, O>(observer));
                                }
                            });

                    // start the server
                    final ChannelFuture f = b.bind(port).sync();

                    // return a subscription that can shut down the server
                    return Subscriptions.create(new Action0() {

                        @Override
                        public void call() {
                            try {
                                f.channel().close().sync();
                            } catch (InterruptedException e) {
                                throw new RuntimeException("Failed to unsubscribe");
                            }
                        }

                    });
                } catch (Throwable e) {
                    observer.onError(e);
                    return Subscriptions.empty();
                }
            }
        });
    }
}
