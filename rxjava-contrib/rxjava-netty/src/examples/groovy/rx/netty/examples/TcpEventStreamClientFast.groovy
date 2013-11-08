package rx.netty.examples

import rx.Observable
import rx.experimental.remote.RemoteSubscription
import rx.netty.experimental.RxNetty
import rx.netty.experimental.impl.ObservableConnection
import rx.netty.experimental.protocol.tcp.ProtocolHandlers


/**
 * Connects to EventStreamServer and processes events as fast as possible. This should not queue or require back-pressure.
 */
class TcpEventStreamClientFast {

    def static void main(String[] args) {

        RemoteSubscription s = RxNetty.createTcpClient("localhost", 8181, ProtocolHandlers.stringLineCodec())
                .onConnect({ ObservableConnection<String, String> connection ->
                    return connection.getInput().map({ String msg ->
                        return msg.trim()
                    });
                }).subscribe({ String o ->
                    println("onNext event => " + o)
                }, {Throwable e ->
                    println("error => " + e); e.printStackTrace()
                });

        /*
         * one problem of having RemoteObservable/RemoteSubscription is that we lose the Observable
         * extensions such as toBlockingObservable().
         * 
         * In other words, RemoteSubscription makes this non-composable with normal Observable/Subscription
         */

        // artificially waiting since the above is non-blocking
        Thread.sleep(100000);
    }
}
