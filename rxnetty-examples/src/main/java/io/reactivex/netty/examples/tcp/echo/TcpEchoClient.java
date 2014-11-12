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

package io.reactivex.netty.examples.tcp.echo;

import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import rx.Observable;
import rx.functions.Action0;
import rx.functions.Func1;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.reactivex.netty.examples.tcp.echo.TcpEchoServer.DEFAULT_PORT;

/**
 * @author Nitesh Kant
 */
public final class TcpEchoClient {

    private final int port;

    public TcpEchoClient(int port) {
        this.port = port;
    }

    public List<String> sendEchos() {
        Observable<ObservableConnection<String, String>> connectionObservable =
                RxNetty.createTcpClient("localhost", port, PipelineConfigurators.textOnlyConfigurator()).connect();

        Iterable<Object> echos = connectionObservable.flatMap(new Func1<ObservableConnection<String, String>, Observable<?>>() {
            @Override
            public Observable<?> call(final ObservableConnection<String, String> connection) {
                // we expect the EchoServer to output a single value at the beginning
                // so let's take the first value ... we can do this without it closing the connection
                // because the unsubscribe will hit the ChannelObservable is a PublishSubject
                // so we can re-subscribe to the 'hot' stream of data
                Observable<String> helloMessage = connection.getInput()
                        .take(1).map(new Func1<String, String>() {
                            @Override
                            public String call(String s) {
                                return s.trim();
                            }
                        });

                // output 10 values at intervals and receive the echo back
                Observable<String> intervalOutput =
                        Observable.interval(500, TimeUnit.MILLISECONDS)
                                .flatMap(new Func1<Long, Observable<String>>() {
                                    @Override
                                    public Observable<String> call(Long aLong) {
                                        return connection.writeAndFlush(String.valueOf(aLong + 1))
                                                .map(new Func1<Void, String>() {
                                                    @Override
                                                    public String call(Void aVoid) {
                                                        return "";
                                                    }
                                                });
                                    }
                                });

                // capture the output from the server
                Observable<String> echo = connection.getInput().map(new Func1<String, String>() {
                    @Override
                    public String call(String s) {
                        return s.trim();
                    }
                });

                // wait for the helloMessage then start the output and receive echo input
                return Observable.concat(helloMessage, Observable.merge(intervalOutput, echo));
            }
        }).take(10).doOnCompleted(new Action0() {
            @Override
            public void call() {
                System.out.println("COMPLETED!");
            }
        }).toBlocking().toIterable();

        List<String> result = new ArrayList<String>();
        for (Object e : echos) {
            System.out.println(e);
            result.add(e.toString());
        }
        return result;
    }

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }
        new TcpEchoClient(port).sendEchos();
    }
}
