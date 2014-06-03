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
package io.reactivex.netty.examples.java;

import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * @author Nitesh Kant
 */
public final class TcpEventStreamClientSlow {

    public static void main(String[] args) {
        Observable<ObservableConnection<String, String>> connectionObservable =
                RxNetty.createTcpClient("localhost", 8181, PipelineConfigurators.stringMessageConfigurator()).connect();
        connectionObservable.flatMap(new Func1<ObservableConnection<String, String>, Observable<?>>() {
            @Override
            public Observable<?> call(ObservableConnection<String, String> connection) {
                return connection.getInput().map(new Func1<String, String>() {
                    @Override
                    public String call(String msg) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        return msg.trim();
                    }
                });
            }
        }).toBlocking().forEach(new Action1<Object>() {
            @Override
            public void call(Object o) {
                System.out.println("onNext event => " + o);
            }
        });

    }
}
