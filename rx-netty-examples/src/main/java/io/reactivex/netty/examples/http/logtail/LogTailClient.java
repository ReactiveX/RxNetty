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
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.text.sse.ServerSentEvent;
import rx.Observable;
import rx.functions.Func1;

import java.util.ArrayList;
import java.util.List;

import static io.reactivex.netty.examples.http.logtail.LogAggregator.DEFAULT_AG_PORT;

/**
 * @author Tomasz Bak
 */
public class LogTailClient {

    static final int DEFAULT_TAIL_SIZE = 25;

    private final int port;
    private final int tailSize;

    public LogTailClient(int port, int tailSize) {
        this.port = port;
        this.tailSize = tailSize;
    }

    public List<LogEvent> collectEventLogs() {
        HttpClient<ByteBuf, ServerSentEvent> client =
                RxNetty.createHttpClient("localhost", port, PipelineConfigurators.<ByteBuf>sseClientConfigurator());

        Iterable<LogEvent> eventIterable = client.submit(HttpClientRequest.createGet("/logstream"))
                .flatMap(new Func1<HttpClientResponse<ServerSentEvent>, Observable<ServerSentEvent>>() {
                    @Override
                    public Observable<ServerSentEvent> call(HttpClientResponse<ServerSentEvent> response) {
                        if (response.getStatus().equals(HttpResponseStatus.OK)) {
                            return response.getContent();
                        }
                        return Observable.error(new IllegalStateException("server returned status " + response.getStatus()));
                    }
                }).map(new Func1<ServerSentEvent, LogEvent>() {
                           @Override
                           public LogEvent call(ServerSentEvent serverSentEvent) {
                               return LogEvent.fromCSV(serverSentEvent.getEventData());
                           }
                       }
                ).filter(new Func1<LogEvent, Boolean>() {
                             @Override
                             public Boolean call(LogEvent logEvent) {
                                 return logEvent.getLevel() == LogEvent.LogLevel.ERROR;
                             }
                         }
                ).take(tailSize).toBlocking().toIterable();

        List<LogEvent> logs = new ArrayList<LogEvent>();
        for (LogEvent e : eventIterable) {
            System.out.println("event " + logs.size() + ": " + e);
            logs.add(e);
        }
        return logs;
    }

    public static void main(String[] args) {
        LogTailClient client = new LogTailClient(DEFAULT_AG_PORT, DEFAULT_TAIL_SIZE);
        List<LogEvent> logEvents = client.collectEventLogs();
        System.out.printf("LogTailClient service collected %d entries", logEvents.size());
    }
}
