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

package io.reactivex.netty.examples.tcp.cpuintensive;

import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.ConnectionHandler;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.channel.RxDefaultThreadFactory;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.server.RxServer;
import rx.Observable;
import rx.functions.Func1;

/**
 * @author Nitesh Kant
 */
public final class CPUIntensiveServer {

    static final int DEFAULT_PORT = 8791;

    private final int port;

    public CPUIntensiveServer(int port) {
        this.port = port;
    }

    public RxServer<String, String> createServer() {
        RxServer<String, String> server =
                RxNetty.newTcpServerBuilder(port, new ConnectionHandler<String, String>() {
                                            @Override
                                            public Observable<Void> handle(
                                                    final ObservableConnection<String, String> connection) {
                                                System.out.println("New client connection established.");
                                                connection.writeAndFlush("Welcome! \n\n");
                                                return connection.getInput()
                                                                 .flatMap(new Func1<String, Observable<Void>>() {
                                                                     @Override
                                                                     public Observable<Void> call(String msg) {
                                                                         System.out.println("onNext: " + msg);
                                                                         msg = msg.trim();
                                                                         if (!msg.isEmpty()) {
                                                                             return connection.writeAndFlush(
                                                                                     "echo => " + msg + '\n');
                                                                         } else {
                                                                             return Observable.empty();
                                                                         }
                                                                     }
                                                                 });
                                            }
                                        })
                       .pipelineConfigurator(PipelineConfigurators.textOnlyConfigurator())
                       .withEventExecutorGroup(new DefaultEventExecutorGroup(50, new RxDefaultThreadFactory(
                               "rx-connection-processor"))) /*Uses 50 threads to process connections.*/
                       .build();
        return server;
    }

    public static void main(final String[] args) {
        new CPUIntensiveServer(DEFAULT_PORT).createServer().startAndWait();
    }
}
