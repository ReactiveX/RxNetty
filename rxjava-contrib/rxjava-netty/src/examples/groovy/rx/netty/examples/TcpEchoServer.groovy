package rx.netty.examples

import rx.Observable
import rx.Subscription
import rx.netty.experimental.RxNetty
import rx.netty.experimental.impl.ObservableConnection
import rx.netty.experimental.protocol.tcp.ProtocolHandlers


class TcpEchoServer {

    def static void main(String[] args) {

        Subscription s = RxNetty.createTcpServer(8181, ProtocolHandlers.stringCodec())
                .onConnect({ ObservableConnection<String, String> connection ->
                    // writing to the connection is the only place where anything is remote
                    connection.write("Welcome! \n\n")

                    // perform echo logic and return the transformed output stream that will be subscribed to
                    return connection.getInput()
                    .map({ String msg -> msg.trim() })
                    .filter({String msg -> !msg.isEmpty()})
                    .flatMap({ String msg ->
                        // echo the input to the output stream
                        return connection.write("echo => " + msg + "\n")
                    });
                }).toBlockingObservable().forEach({ String o ->
                    println("onNext: " + o)
                });

    }
}
