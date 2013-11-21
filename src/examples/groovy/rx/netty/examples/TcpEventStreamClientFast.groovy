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
import rx.netty.RxNetty
import rx.netty.impl.ObservableConnection
import rx.netty.protocol.tcp.ProtocolHandlers


/**
 * Connects to EventStreamServer and processes events as fast as possible. This should not queue or require back-pressure.
 */
class TcpEventStreamClientFast {

    def static void main(String[] args) {

        RxNetty.createTcpClient("localhost", 8181, ProtocolHandlers.stringLineCodec())
                .flatMap({ ObservableConnection<String, String> connection ->
                    return connection.getInput().map({ String msg ->
                        return msg.trim()
                    });
                }).toBlockingObservable().forEach({ String o ->
                    println("onNext event => " + o)
                });

    }
}
