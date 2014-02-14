/*
 * Copyright 2014 Netflix, Inc.
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

import io.reactivex.netty.ObservableConnection
import io.reactivex.netty.RxNetty
import io.reactivex.netty.pipeline.PipelineConfigurators
import rx.Observable

import java.util.concurrent.TimeUnit

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
class TcpEchoClient {

    def static void main(String[] args) {

        RxNetty.createTcpClient("localhost", 8181, PipelineConfigurators.textOnlyConfigurator()).connect()
                .flatMap({ ObservableConnection<String, String> connection ->
                    
                    // we expect the EchoServer to output a single value at the beginning
                    // so let's take the first value ... we can do this without it closing the connection
                    // because the unsubscribe will hit the ChannelObservable is a PublishSubject
                    // so we can re-subscribe to the 'hot' stream of data
                    Observable<String> helloMessage = connection.getInput()
                            .takeFirst().map({ String s -> s.trim() })

                    // output 10 values at intervals and receive the echo back
                    Observable<String> intervalOutput = Observable.interval(500, TimeUnit.MILLISECONDS)
                            .flatMap({ long l ->
                                // write the output and convert from Void to String so it can merge with others
                                // (nothing will be emitted since 'write' is Observable<Void>)
                                return connection.write(String.valueOf(l+1)).map({ return ""});
                            })

                    // capture the output from the server
                    Observable<String> echo = connection.getInput().map({ String msg ->
                        return msg.trim()
                    });

                    // wait for the helloMessage then start the output and receive echo input
                    return Observable.concat(helloMessage, Observable.merge(intervalOutput, echo));
                })
                .take(10)
                .doOnCompleted({
                     println("COMPLETED!")   
                })
                .toBlockingObservable().forEach({ String o ->
                    println("onNext: " + o)
                });

            
            
            
    }


}
