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

package io.reactivex.netty.examples.tcp.event;

import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import rx.Observable;
import rx.functions.Func1;

import static io.reactivex.netty.examples.tcp.event.TcpEventStreamServer.DEFAULT_PORT;

/**
 * @author Nitesh Kant
 */
public final class TcpEventStreamClient {

    private final int port;
    private final int delay;
    private final int noOfEvents;

    public TcpEventStreamClient(int port, int delay, int noOfEvents) {
        this.port = port;
        this.delay = delay;
        this.noOfEvents = noOfEvents;
    }

    public int readEvents() {
        Observable<ObservableConnection<String, String>> connectionObservable =
                RxNetty.createTcpClient("localhost", port, PipelineConfigurators.stringMessageConfigurator()).connect();
        Iterable<Object> eventIterable = connectionObservable.flatMap(new Func1<ObservableConnection<String, String>, Observable<?>>() {
            @Override
            public Observable<?> call(ObservableConnection<String, String> connection) {
                return connection.getInput().map(new Func1<String, String>() {
                    @Override
                    public String call(String msg) {
                        delayProcessing();
                        return msg.trim();
                    }
                });
            }
        }).take(noOfEvents).toBlocking().toIterable();

        int count = 0;
        for (Object e : eventIterable) {
            System.out.println("onNext event => " + e);
            count++;
        }
        return count;
    }

    private void delayProcessing() {
        if (delay > 0) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        int delay = 1000;
        int noOfEvents = 100;
        if (args.length > 1) {
            delay = Integer.valueOf(args[0]);
            noOfEvents = Integer.valueOf(args[1]);
        }
        new TcpEventStreamClient(DEFAULT_PORT, delay, noOfEvents).readEvents();
    }
}
