package rx.netty.examples

import java.util.concurrent.TimeUnit

import rx.Observable
import rx.experimental.remote.RemoteSubscription
import rx.netty.experimental.RxNetty
import rx.netty.experimental.impl.TcpConnection
import rx.netty.experimental.protocol.ProtocolHandlers

/**
 * Connects to EchoServer, awaits first "Welcome!" message then outputs 10 values and receives the echo responses.
 * <p>
 * Should output results like:
 * <p>
 * <pre>
 * onNext: Welcome!
 * onNext: echo => 1
 * onNext: echo => 2
 * onNext: echo => 3
 * onNext: echo => 4
 * onNext: echo => 5
 * onNext: echo => 6
 * onNext: echo => 7
 * onNext: echo => 8
 * onNext: echo => 9
 * onNext: echo => 10
 *  </pre>
 *
 */
class EchoClient {


    def static void main(String[] args) {

        RemoteSubscription s = RxNetty.createTcpClient("localhost", 8181, ProtocolHandlers.stringCodec())
                .onConnect({ TcpConnection<String, String> connection ->
                    // we expect the EchoServer to output a single value at the beginning
                    // so let's take the first value ... we can do this without it closing the connection
                    // because the unsubscribe will hit the ChannelObservable is a PublishSubject
                    // so we can re-subscribe to the 'hot' stream of data
                    Observable<String> helloMessage = connection.getChannelObservable()
                            .takeFirst().map({ String s -> s.trim() })

                    // output 10 values at intervals and receive the echo back
                    Observable<String> intervalOutput = Observable.interval(500, TimeUnit.MILLISECONDS)
                            .skip(1).take(10).flatMap({ long l ->
                                // write the output and convert from Void to String so it can merge with others
                                // (nothing will be emitted since 'write' is Observable<Void>)
                                return connection.write(String.valueOf(l)).map({ return ""});
                            })

                    // capture the output from the server
                    Observable<String> echo = connection.getChannelObservable().map({ String msg ->
                        return msg.trim()
                    });

                    // wait for the helloMessage then start the output and receive echo input
                    return Observable.concat(helloMessage, Observable.merge(intervalOutput, echo));
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
