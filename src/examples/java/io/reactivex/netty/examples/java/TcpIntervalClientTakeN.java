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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.string.StringDecoder;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import rx.Observable;
import rx.util.functions.Action1;
import rx.util.functions.Func1;

/**
 * @author Nitesh Kant
 */
public final class TcpIntervalClientTakeN {

    public static void main(String[] args) {
        Observable<ObservableConnection<String, ByteBuf>> connectionObservable =
                RxNetty.createTcpClient("localhost", 8181, new PipelineConfigurator<String, ByteBuf>() {
                    @Override
                    public void configureNewPipeline(ChannelPipeline pipeline) {
                        pipeline.addLast(new StringDecoder());
                    }
                }).connect();
        connectionObservable.flatMap(new Func1<ObservableConnection<String, ByteBuf>, Observable<String>>() {
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
        }).take(3).toBlockingObservable().forEach(new Action1<Object>() {
            @Override
            public void call(Object o) {
                System.out.println("onNext: " + o);
            }
        });

    }
}
