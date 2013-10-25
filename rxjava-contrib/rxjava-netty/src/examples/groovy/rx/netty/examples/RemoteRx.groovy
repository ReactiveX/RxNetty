package rx.netty.examples;

import io.netty.buffer.ByteBuf;

import java.nio.charset.Charset;

import rx.Observable;
import rx.experimental.remote.RemoteObservable;
import rx.experimental.remote.RemoteObservable.FilterCriteria;
import rx.experimental.remote.RemoteObservable.MapProjection;
import rx.experimental.remote.RemoteObservable.RemoteOnSubscribeFunc;
import rx.experimental.remote.RemoteObserver;
import rx.experimental.remote.RemoteSubscription;
import rx.netty.experimental.RxNetty;
import rx.netty.experimental.impl.TcpConnection;
import rx.util.functions.Action1;
import rx.util.functions.Func1;

public class RemoteRx {

    public static void main(String[] args) {
//        RemoteObservable.create(new NodeServer())
//                .toObservable().flatMap(new Func1<TcpConnection, Observable<String>>() {
//
//                    @Override
//                    public Observable<String> call(final TcpConnection connection) {
//                        System.out.println("---------------- received connection ------------------------");
//                        // for each message we receive on the connection
//                        return connection.getChannelObservable().map(new Func1<ByteBuf, String>() {
//
//                            @Override
//                            public String call(ByteBuf bb) {
//                                String msg = bb.toString(Charset.forName("UTF8")).trim();
//                                if (msg.startsWith("subscribe:")) {
//                                    System.out.println("Received 'subscribe' from client so starting interval ...");
//                                    // TODO how can we do this with startInterval returning an Observable instead of subscription?
//                                    connection.addSubscription(IntervalServer.startInterval(connection));
//                                } else if (msg.startsWith("unsubscribe:")) {
//                                    System.out.println("Received 'unsubscribe' from client so stopping interval ...");
//                                    connection.unsubscribe();
//                                } else {
//                                    if (!msg.isEmpty()) {
//                                        connection.write("\nERROR => Unknown command: " + msg + "\nCommands => subscribe:, unsubscribe:\n");
//                                    }
//                                }
//
//                                return msg;
//                            }
//
//                        });
//                    }
//                })
//                .toBlockingObservable().forEach(new Action1<String>() {
//
//                    @Override
//                    public void call(final String v) {
//                        // nothing to do here as the map above is side-affecting
//                        // should we do this differently?
//                    }
//                });
    }

    public static class NodeServer implements RemoteOnSubscribeFunc<TcpConnection> {

        @Override
        public RemoteSubscription onSubscribe(RemoteObserver<? super TcpConnection> observer, FilterCriteria filterCriteria, MapProjection mapProjection) {

            RxNetty.createTcpServer(7272)
                    // process each connection in parallel
                    .parallel(new Func1<Observable<TcpConnection>, Observable<TcpConnection>>() {

                        @Override
                        public Observable<TcpConnection> call(Observable<TcpConnection> o) {
                            // for each connection 
                            return o;

                        }
                    });
            
            return null;
            
        }

    }
}
