package rx.netty.examples;

import io.netty.buffer.ByteBuf;

import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import rx.Notification;
import rx.Observable;
import rx.Subscription;
import rx.netty.experimental.RxNetty;
import rx.netty.experimental.impl.TcpConnection;
import rx.util.functions.Action0;
import rx.util.functions.Action1;
import rx.util.functions.Func1;

public class IntervalServer {

    public static void main(String[] args) {
        createServer(8181).toBlockingObservable().last();
    }

    public static Observable<String> createServer(final int port) {
        return RxNetty.createTcpServer(port)
                // process each connection in parallel
                .parallel(new Func1<Observable<TcpConnection<String, String>>, Observable<String>>() {

                    @Override
                    public Observable<String> call(Observable<TcpConnection<String, String>> o) {
                        // for each connection 
                        return o.flatMap(new Func1<TcpConnection<String, String>, Observable<String>>() {

                            @Override
                            public Observable<String> call(final TcpConnection<String, String> connection) {
                                // for each message we receive on the connection
                                return connection.getChannelObservable().map(new Func1<String, String>() {

                                    @Override
                                    public String call(String msg) {
                                        msg = msg.trim();
                                        if (msg.startsWith("subscribe:")) {
                                            System.out.println("-------------------------------------");
                                            System.out.println("Received 'subscribe' from client so starting interval ...");
                                            // TODO how can we do this with startInterval returning an Observable instead of subscription?
                                            connection.addSubscription(startInterval(connection));
                                        } else if (msg.startsWith("unsubscribe:")) {
                                            System.out.println("Received 'unsubscribe' from client so stopping interval ...");
                                            connection.unsubscribe();
                                        } else {
                                            if (!msg.isEmpty()) {
                                                connection.write("\nERROR => Unknown command: " + msg + "\nCommands => subscribe:, unsubscribe:\n");
                                            }
                                        }

                                        return msg;
                                    }

                                });
                            }

                        });

                    }
                });
    }

    private static Subscription startInterval(final TcpConnection<String, String> connection) {
        return Observable.interval(1000, TimeUnit.MILLISECONDS)
                .flatMap(new Func1<Long, Observable<Notification<Void>>>() {

                    @Override
                    public Observable<Notification<Void>> call(Long interval) {
                        System.out.println("Writing interval: " + interval);
                        // emit the interval to the output and return the notification received from it
                        return connection.write("interval => " + interval + "\n").materialize();
                    }
                })
                .takeWhile(new Func1<Notification<Void>, Boolean>() {

                    @Override
                    public Boolean call(Notification<Void> n) {
                        // unsubscribe from interval if we receive an error
                        return !n.isOnError();
                    }
                })
                .subscribe(new Action1<Notification<Void>>() {

                    @Override
                    public void call(Notification<Void> interval) {
                        // do nothing
                    }
                }, new Action1<Throwable>() {

                    @Override
                    public void call(Throwable cause) {
                        System.out.println("Interval stopped: " + cause);
                    }

                }, new Action0() {

                    @Override
                    public void call() {
                        System.out.println("Connection closed");
                    }

                });
    }

}
