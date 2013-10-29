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
import rx.experimental.remote.RemoteFilterCriteria;
import rx.experimental.remote.RemoteMapProjection;
import rx.experimental.remote.RemoteObservableClient;
import rx.experimental.remote.RemoteObservableClient.RemoteClientOnSubscribeFunc;
import rx.experimental.remote.RemoteSubscription;
import rx.netty.experimental.protocol.ProtocolHandler;
import rx.subscriptions.Subscriptions;

public class NettyClient {

    public static <I, O> RemoteObservableClient<TcpConnection<I, O>> createClient(final String host, final int port, final EventLoopGroup eventLoops, final ProtocolHandler<I, O> handler) {
        return RemoteObservableClient.create(new RemoteClientOnSubscribeFunc<TcpConnection<I, O>>() {

            @Override
            public RemoteSubscription onSubscribe(final Observer<? super TcpConnection<I, O>> observer, RemoteFilterCriteria filterCriteria, RemoteMapProjection mapProjection) {
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
                    return new RemoteSubscription() {

                        @Override
                        public Observable<Void> unsubscribe() {
                            return Observable.create(new OnSubscribeFunc<Void>() {

                                @Override
                                public Subscription onSubscribe(Observer<? super Void> o) {
                                    try {
                                        f.channel().close().sync();
                                        o.onCompleted();
                                    } catch (InterruptedException e) {
                                        o.onError(new RuntimeException("Failed to unsubscribe", e));
                                    }
                                    return Subscriptions.empty();
                                }
                            });
                        }
                    };
                } catch (Throwable e) {
                    observer.onError(e);
                    return new RemoteSubscription() {

                        @Override
                        public Observable<Void> unsubscribe() {
                            return null;
                        }
                    };
                }
            }
        });
    }
}