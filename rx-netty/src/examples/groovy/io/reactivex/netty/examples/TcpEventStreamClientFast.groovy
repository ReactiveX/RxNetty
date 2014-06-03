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

import io.reactivex.netty.RxNetty
import io.reactivex.netty.channel.ObservableConnection
import io.reactivex.netty.pipeline.PipelineConfigurators

/**
 * Connects to EventStreamServer and processes events as fast as possible. This should not queue or require back-pressure.
 */
class TcpEventStreamClientFast {

    def static void main(String[] args) {

        RxNetty.createTcpClient("localhost", 8181, PipelineConfigurators.stringMessageConfigurator()).connect()
                .flatMap({ ObservableConnection<String, String> connection ->
                    return connection.getInput().map({ String msg ->
                        return msg.trim()
                    });
                }).toBlocking().forEach({ String o ->
                    println("onNext event => " + o)
                });

    }
}
