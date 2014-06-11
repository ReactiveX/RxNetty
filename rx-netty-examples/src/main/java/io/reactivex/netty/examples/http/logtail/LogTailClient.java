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

package io.reactivex.netty.examples.http.logtail;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.text.sse.ServerSentEvent;
import rx.Notification;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

/**
 * @author Tomasz Bak
 */
public class LogTailClient {

    ConcurrentLinkedQueue<LogEvent> logs = new ConcurrentLinkedQueue<LogEvent>();

    private int port;
    private final int tailSize;

    private CountDownLatch doneSignal = new CountDownLatch(1);

    public LogTailClient(int port, int tailSize) {
        this.port = port;
        this.tailSize = tailSize;
    }

    public void startCollectionProcess() {
        HttpClient<ByteBuf, ServerSentEvent> client =
                RxNetty.createHttpClient("localhost", port, PipelineConfigurators.<ByteBuf>sseClientConfigurator());

        client.submit(HttpClientRequest.createGet("/logstream")).flatMap(new Func1<HttpClientResponse<ServerSentEvent>, Observable<ServerSentEvent>>() {
            @Override
            public Observable<ServerSentEvent> call(HttpClientResponse<ServerSentEvent> response) {
                return response.getContent();
            }
        }).map(new Func1<ServerSentEvent, LogEvent>() {
            @Override
            public LogEvent call(ServerSentEvent serverSentEvent) {
                return LogEvent.fromCSV(serverSentEvent.getEventData());
            }
        }).filter(new Func1<LogEvent, Boolean>() {
            @Override
            public Boolean call(LogEvent logEvent) {
                return logEvent.getLevel() == LogEvent.LogLevel.ERROR;
            }
        }).takeWhile(new Func1<LogEvent, Boolean>() {
            @Override
            public Boolean call(LogEvent serverSentEvent) {
                if (logs.size() < tailSize) {
                    return true;
                }
                doneSignal.countDown();
                return false;
            }
        }).materialize().forEach(new Action1<Notification<LogEvent>>() {
            @Override
            public void call(Notification<LogEvent> notification) {
                if (notification.isOnNext()) {
                    LogEvent event = notification.getValue();
                    logs.add(event);
                    System.out.println("event " + logs.size() + ": " + event);
                } else {
                    if (notification.isOnError()) {
                        System.err.println("ERROR: connection failure");
                        notification.getThrowable().printStackTrace();
                    }
                    doneSignal.countDown();
                }
            }
        });
    }

    public List<LogEvent> tail() {
        try {
            doneSignal.await();
        } catch (InterruptedException e) {
            // IGNORE
        }
        return new ArrayList<LogEvent>(logs);
    }

    public static void main(String[] args) {
        int tailSize = 25;
        int agPort = 8080;
        if (args.length > 1) {
            tailSize = Integer.valueOf(args[0]);
            agPort = Integer.valueOf(args[1]);
        }
        LogTailClient client = new LogTailClient(agPort, tailSize);
        client.startCollectionProcess();
        client.tail();
        System.out.println("LogTailClient service terminated");
    }
}
