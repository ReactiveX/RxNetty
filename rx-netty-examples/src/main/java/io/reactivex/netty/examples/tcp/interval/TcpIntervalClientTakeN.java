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

package io.reactivex.netty.examples.tcp.interval;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.string.StringDecoder;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import rx.Observable;
import rx.functions.Func1;

import static io.reactivex.netty.examples.tcp.interval.TcpIntervalServer.DEFAULT_PORT;

/**
 * @author Nitesh Kant
 */
public final class TcpIntervalClientTakeN {

    private final int port;
    private final int noOfMsg;

    public TcpIntervalClientTakeN(int port, int noOfMsg) {
        this.port = port;
        this.noOfMsg = noOfMsg;
    }

    public int collectMessages() {
        Observable<ObservableConnection<String, ByteBuf>> connectionObservable =
                RxNetty.createTcpClient("localhost", port, new PipelineConfigurator<String, ByteBuf>() {
                    @Override
                    public void configureNewPipeline(ChannelPipeline pipeline) {
                        pipeline.addLast(new StringDecoder());
                    }
                }).connect();
        Iterable<String> msgIterable = connectionObservable.flatMap(new Func1<ObservableConnection<String, ByteBuf>, Observable<String>>() {
            @Override
            public Observable<String> call(ObservableConnection<String, ByteBuf> connection) {
                ByteBuf request = Unpooled.copiedBuffer("subscribe:".getBytes());
                Observable<String> subscribeWrite = connection.writeAndFlush(request).map(new Func1<Void, String>() {
                    @Override
                    public String call(Void aVoid) {
                        return "";
                    }
                });

                Observable<String> data = connection.getInput().map(new Func1<String, String>() {
                    @Override
                    public String call(String msg) {
                        return msg.trim();
                    }
                });

                return Observable.concat(subscribeWrite, data);
            }
        }).take(noOfMsg).toBlocking().toIterable();

        int count = 0;
        for (String m : msgIterable) {
            System.out.println("onNext: " + m);
            count++;
        }
        return count;
    }

    public static void main(String[] args) {
        int noOfMsg = 100;
        if (args.length > 0) {
            noOfMsg = Integer.valueOf(args[0]);
        }
        new TcpIntervalClientTakeN(DEFAULT_PORT, noOfMsg).collectMessages();
    }
}
