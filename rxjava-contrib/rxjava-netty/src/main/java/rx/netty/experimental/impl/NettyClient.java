package rx.netty.experimental.impl;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.netty.experimental.protocol.ProtocolHandler;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;

public class NettyClient {

    public static <I, O> Observable<TcpConnection<I, O>> createClient(final String host, final int port, final EventLoopGroup eventLoops, final ProtocolHandler<I, O> handler) {
        return Observable.create(new OnSubscribeFunc<TcpConnection<I, O>>() {

            @Override
            public Subscription onSubscribe(final Observer<? super TcpConnection<I, O>> observer) {
                try {
                    Bootstrap b = new Bootstrap();
                    b.group(eventLoops)
                        .channel(NioSocketChannel.class)
                            // TODO allow ChannelOptions to be passed in
                        .option(ChannelOption.TCP_NODELAY, true)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            public void initChannel(SocketChannel ch) throws Exception {
                                handler.configure(ch.pipeline());

                                // add the handler that will emit responses to the observer
                                ch.pipeline()
                                    .addLast(new HandlerObserver<I, O>(observer));
                            }
                        });

                    // make the connection
                    final ChannelFuture f = b.connect(host, port).sync();

                    // return a subscription that can shut down the connection
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
