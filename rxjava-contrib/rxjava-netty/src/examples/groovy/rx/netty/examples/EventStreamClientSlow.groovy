package rx.netty.examples

import rx.experimental.remote.RemoteSubscription
import rx.netty.experimental.RxNetty
import rx.netty.experimental.impl.TcpConnection
import rx.netty.experimental.protocol.ProtocolHandlers

/**
 * Connects to EventStreamServer and simulates a slow consumer. 
 * <p>
 * The server outputs events like this:
 * <p>
 * <pre>
 * Writing event: 263778
 * Writing event: 263779
 * Writing event: 263780
 * Writing event: 263781
 * Writing event: 263782
 * </pre>
 * <p>
 * This consumer will only be at 2632 by the time the server has emitted 263782 events:
 * <p>
 * <pre> 
 * onNext event => data: {"type":"Command","name":"GetAccount","currentTime":1376957348166,"errorPercentage":0,"errorCount":0,"requestCount":2631}
 * onNext event => data: {"type":"Command","name":"GetAccount","currentTime":1376957348166,"errorPercentage":0,"errorCount":0,"requestCount":2632}
 * </pre>
 */
class EventStreamClientSlow {

    def static void main(String[] args) {

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
        Thread.sleep(1000000000);
    }
}
