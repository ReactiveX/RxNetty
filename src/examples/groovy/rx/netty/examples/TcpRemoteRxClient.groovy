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
import rx.experimental.remote.RemoteSubscription
import rx.netty.RxNetty
import rx.netty.impl.ObservableConnection
import rx.netty.protocol.tcp.ProtocolHandlers;

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
