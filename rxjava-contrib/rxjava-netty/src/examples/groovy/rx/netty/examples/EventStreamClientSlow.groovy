package rx.netty.examples

import rx.experimental.remote.RemoteSubscription
import rx.netty.experimental.RxNetty
import rx.netty.experimental.impl.TcpConnection
import rx.netty.experimental.protocol.ProtocolHandlers

/**
 * Connects to EventStreamServer and simulates a slow consumer. 
 */
class EventStreamClientSlow {

    def static void main(String[] args) {

        /**
         * TODO: Need better protocol than 'stringCodec' that breaks on newlines.
         * 
         * This example is not yet right because it is naturally handling backpressure by batching events 
         * into each onNext. This is because the stringCodec() just takes whatever is in the buffer and returns
         * it as a string. We need the codec to properly tokenize events on CRLF and then the delay will
         * happen on each line and cause queueing with true "slow consumer" side-effects
         */
        RemoteSubscription s = RxNetty.createTcpClient("localhost", 8181, ProtocolHandlers.stringLineCodec())
                .onConnect({ TcpConnection<String, String> connection ->
                    return connection.getChannelObservable().map({ String msg ->
                        // simulate slow processing
                        Thread.sleep(1000)
                        return msg.trim()
                    });
                }).subscribe({ String o ->
                    println("onNext event => " + o + "\n")
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
