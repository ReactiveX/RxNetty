package rx.netty.examples

import io.netty.buffer.ByteBuf;

import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import rx.*;
import rx.netty.experimental.*;
import rx.netty.experimental.impl.TcpConnection;
import rx.util.functions.Action0;
import rx.util.functions.Action1;
import rx.util.functions.Func1;

class IntervalServer {

    public static void main(String[] args) {
        createServer(8181).toBlockingObservable().last();
    }

    public static Observable<String> createServer(final int port) {
        return RxNetty.createTcpServer(port)
        // process each connection in parallel
        .parallel({ Observable<TcpConnection> o ->
            // for each connection
            return o.flatMap({ TcpConnection connection ->
                // for each message we receive on the connection
                return connection.getChannelObservable().map({ ByteBuf bb ->
                    String msg = bb.toString(Charset.forName("UTF8")).trim();
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
                });
            });

        });
    }

    public static Subscription startInterval(final TcpConnection connection) {
        return Observable.interval(1000, TimeUnit.MILLISECONDS)
        .flatMap({ Long interval ->
            System.out.println("Writing interval: " + interval);
            // emit the interval to the output and return the notification received from it
            return connection.write("interval => " + interval + "\n").materialize();
        })
        .takeWhile({ Notification<Void> n ->
            // unsubscribe from interval if we receive an error
            return !n.isOnError();
        })
        .subscribe(
        { Notification<Void> interval ->
            // do nothing
        },
        { Throwable cause ->
            System.out.println("Interval stopped: " + cause);
        }, {
            System.out.println("Connection closed");
        });
    }
}
