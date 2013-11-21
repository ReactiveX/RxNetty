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
package rx.netty.examples;

import io.netty.buffer.ByteBuf;

import java.nio.charset.Charset;

import rx.Observable;
import rx.netty.RxNetty;
import rx.netty.impl.ObservableConnection;
import rx.util.functions.Action1;
import rx.util.functions.Func1;

public class TcpIntervalClientWithDisconnect {

    public static void main(String[] args) {
        new TcpIntervalClientWithDisconnect().run();
    }

    public void run() {
        RxNetty.createTcpClient("localhost", 8181)
                .flatMap({ ObservableConnection connection ->
                    System.out.println("received connection: " + connection);

                    Observable<String> subscribeMessage = connection.write("subscribe:")
                            // the intent of the flatMap to string is so onError can
                            // be propagated via the concat below
                            .flatMap({ Void t1 ->
                                System.out.println("Send subscribe!");
                                return Observable.empty();
                            });

                    Observable<String> messageHandling = connection.getInput().map({ ByteBuf bb ->
                        return bb.toString(Charset.forName("UTF8")).trim();
                    });

                    return Observable.concat(subscribeMessage, messageHandling);
                })
                .take(10)
                .toBlockingObservable().forEach({ String v ->
                    System.out.println("Received: " + v);
                });

    }
}
