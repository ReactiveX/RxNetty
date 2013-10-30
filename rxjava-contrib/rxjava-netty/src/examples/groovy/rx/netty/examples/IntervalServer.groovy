package rx.netty.examples

import java.util.concurrent.TimeUnit

import rx.*
import rx.netty.experimental.*
import rx.netty.experimental.impl.TcpConnection
import rx.netty.experimental.protocol.ProtocolHandlers

/**
 * When a client connects and sends "subscribe:" it will start emitting until it receives "unsubscribe:"
 * <p>
 * It will look something like this:
 * <pre>
 * -------------------------------------
 * Received 'subscribe' from client so starting interval ...
 * Writing interval: 0
 * Writing interval: 1
 * Writing interval: 2
 * Writing interval: 3
 * Received 'unsubscribe' from client so stopping interval (or ignoring if nothing subscribed) ...
 * </pre>
 */
class IntervalServer {

    public static void main(String[] args) {
        createServer(8181).toBlockingObservable().last();
    }

    public static Observable<String> createServer(final int port) {
<<<<<<< HEAD
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
=======
        return RxNetty.createTcpServer(port, ProtocolHandlers.stringCodec())
        .onConnect({ TcpConnection<String, String> connection ->

            println("--- Connection Started ---")

            Observable<String> input = connection.getChannelObservable().map({ String m ->
                return m.trim()
            });

            // for each message we receive on the connection
            return input.flatMap({ String msg ->
                if (msg.startsWith("subscribe:")) {
                    System.out.println("-------------------------------------");
                    System.out.println("Received 'subscribe' from client so starting interval ...");
                    return getIntervalObservable(connection).takeUntil(input.filter({ String m -> m.equals("unsubscribe:")}))
                } else if (msg.startsWith("unsubscribe:")) {
                    // this is here just for verbose logging
                    System.out.println("Received 'unsubscribe' from client so stopping interval (or ignoring if nothing subscribed) ...");
                    return Observable.empty();
                } else {
                    if (!(msg.isEmpty() || "unsubscribe:".equals(msg))) {
                        connection.write("\nERROR => Unknown command: " + msg + "\nCommands => subscribe:, unsubscribe:\n");
>>>>>>> IntervalServer example functional
                    }
                    return Observable.empty();
                }

            }).finallyDo({ println("--- Connection Closed ---") });


        });
    }

<<<<<<< HEAD
    public static Subscription startInterval(final TcpConnection connection) {
=======
    public static Observable<Void> getIntervalObservable(final TcpConnection<String, String> connection) {
>>>>>>> IntervalServer example functional
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
    }
}
