/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.netty.examples

import rx.Observable
import rx.Subscription
import rx.netty.RxNetty
import rx.netty.impl.ObservableConnection
import rx.netty.protocol.tcp.ProtocolHandlers


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
