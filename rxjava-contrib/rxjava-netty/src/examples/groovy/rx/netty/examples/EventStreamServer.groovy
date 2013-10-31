package rx.netty.examples

import java.util.concurrent.TimeUnit

import rx.*
import rx.netty.experimental.*
import rx.netty.experimental.impl.TcpConnection
import rx.netty.experimental.protocol.ProtocolHandlers

/**
 * When a client connects it will start emitting an infinite stream of events.
 */
class EventStreamServer {

    public static void main(String[] args) {
        createServer(8181).toBlockingObservable().last();
    }

    public static Observable<String> createServer(final int port) {
        return RxNetty.createTcpServer(port, ProtocolHandlers.stringCodec())
        .onConnect({ TcpConnection<String, String> connection ->
            return getEventStream(connection);
        });
    }

    public static Observable<Void> getEventStream(final TcpConnection<String, String> connection) {
        return Observable.interval(200, TimeUnit.MILLISECONDS)
        .flatMap({ Long interval ->
            System.out.println("Writing event: " + interval);
            // emit the interval to the output and return the notification received from it
            return connection.write("data: {\"type\":\"Command\",\"name\":\"GetAccount\",,\"currentTime\":1376957348166,\"errorPercentage\":0,\"errorCount\":0,\"requestCount\":" + interval + "}\n").materialize();
        })
        .takeWhile({ Notification<Void> n ->
            // unsubscribe from interval if we receive an error
            return !n.isOnError();
        }).finallyDo({ println(" --> Closing connection and stream") })
    }
}