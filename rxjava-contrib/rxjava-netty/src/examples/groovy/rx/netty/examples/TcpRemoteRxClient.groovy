package rx.netty.examples

import rx.Observable
import rx.experimental.remote.RemoteSubscription
import rx.netty.experimental.RxNetty
import rx.netty.experimental.impl.ObservableConnection
import rx.netty.experimental.protocol.tcp.ProtocolHandlers;

class TcpRemoteRxClient {


    def static void main(String[] args) {

        RemoteSubscription s = RxNetty.createTcpClient("localhost", 8181, ProtocolHandlers.stringCodec())
                .onConnect({ ObservableConnection<String, String> connection ->
                    // side-affecting work can be done in here once the connection is established
                    Observable<?> write = connection.write("subscribe:");
                    // TODO write is eager so race conditions can occur ... need composable solution to this

                    // return the transformed output stream that will be subscribed to
                    return connection.getInput().map({ String msg -> msg.trim()});
                }).subscribe({ String o ->
                    println("onNext: " + o)
                });

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
