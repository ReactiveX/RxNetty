package rx.netty.examples

import rx.Observable
import rx.experimental.remote.RemoteSubscription
import rx.netty.experimental.RxNetty
import rx.netty.experimental.impl.TcpConnection
import rx.netty.experimental.protocol.ProtocolHandlers

/**
 * Connects to IntervalServer, take N values and disconnects.
 * <p>
 * Should output results like:
 * <p>
 * <pre>
 * onNext: interval => 0
 * onNext: interval => 1
 * onNext: interval => 2
 * </pre>
 *
 */
class IntervalClientTakeN {

    def static void main(String[] args) {

        RemoteSubscription s = RxNetty.createTcpClient("localhost", 8181, ProtocolHandlers.stringCodec())
                .onConnect({ TcpConnection<String, String> connection ->

                    // output 10 values at intervals and receive the echo back
                    Observable<String> subscribeWrite = connection.write("subscribe:").map({ return ""});

                    // capture the output from the server
                    Observable<String> data = connection.getChannelObservable().map({ String msg ->
                        return msg.trim()
                    }).take(3);

                    return Observable.concat(subscribeWrite, data);
                }).subscribe({ String o ->
                    println("onNext: " + o) },
                {Throwable e ->
                    println("error: " + e); e.printStackTrace() });

        /*
         * one problem of having RemoteObservable/RemoteSubscription is that we lose the Observable
         * extensions such as toBlockingObservable().
         * 
         * In other words, RemoteSubscription makes this non-composable with normal Observable/Subscription
         */

        // artificially waiting since the above is non-blocking
        Thread.sleep(10000);
    }


}
