package rx.netty.examples

import java.util.concurrent.TimeUnit

import rx.*
import rx.netty.experimental.*
import rx.netty.experimental.impl.ObservableConnection
import rx.netty.experimental.protocol.tcp.ProtocolHandlers

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
class TcpIntervalServer {

    public static void main(String[] args) {
        createServer(8181).toBlockingObservable().last();
    }

    public static Observable<String> createServer(final int port) {
        return RxNetty.createTcpServer(port, ProtocolHandlers.stringCodec())
        .onConnect({ ObservableConnection<String, String> connection ->

            println("--- Connection Started ---")

            Observable<String> input = connection.getInput().map({ String m ->
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
                    }
                    return Observable.empty();
                }

            }).finallyDo({ println("--- Connection Closed ---") });


        });
    }

    public static Observable<Void> getIntervalObservable(final ObservableConnection<String, String> connection) {
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
