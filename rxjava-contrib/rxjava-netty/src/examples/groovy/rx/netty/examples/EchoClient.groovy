package rx.netty.examples

import rx.Observable
import rx.experimental.remote.RemoteSubscription
import rx.netty.experimental.RxNetty;
import rx.netty.experimental.impl.TcpConnection
import rx.netty.experimental.protocol.ProtocolHandlers;

class EchoClient {


    def static void main(String[] args) {

        RemoteSubscription s = RxNetty.createTcpClient("localhost", 8181, ProtocolHandlers.stringCodec())
                .onConnect({ TcpConnection<String, String> connection ->
//                    Observable<?> write = connection.write("hello");
                    // TODO write is eager so race conditions can occur ... need composable solution to this

                    // return the transformed output stream that will be subscribed to
                    return connection.getChannelObservable().map({ String msg -> msg.trim()});
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
