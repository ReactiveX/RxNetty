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
package io.reactivex.netty.examples

import io.reactivex.netty.RxNetty
import io.reactivex.netty.ObservableConnection
import io.reactivex.netty.ProtocolHandlers


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
class TcpEventStreamClientSlow {

    def static void main(String[] args) {

        RxNetty.createTcpClient("localhost", 8181, ProtocolHandlers.stringLineCodec())
                .flatMap({ ObservableConnection<String, String> connection ->
                    return connection.getInput().map({ String msg ->
                        // simulate slow processing
                        Thread.sleep(1000)
                        return msg.trim()
                    });
                }).toBlockingObservable().forEach({ String o ->
                    println("onNext event => " + o + "\n")
                });

    }
}
