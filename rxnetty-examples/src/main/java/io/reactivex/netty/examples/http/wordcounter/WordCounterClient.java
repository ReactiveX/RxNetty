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

package io.reactivex.netty.examples.http.wordcounter;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.StringTransformer;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.charset.Charset;

import static io.reactivex.netty.examples.http.wordcounter.WordCounterServer.DEFAULT_PORT;

/**
 * @author Tomasz Bak
 */
public class WordCounterClient {

    private final int port;
    private final String textFile;

    public WordCounterClient(int port, String textFile) {
        this.port = port;
        this.textFile = textFile;
    }

    public int countWords() throws IOException {
        PipelineConfigurator<HttpClientResponse<ByteBuf>, HttpClientRequest<ByteBuf>> pipelineConfigurator
                = PipelineConfigurators.httpClientConfigurator();

        HttpClient<ByteBuf, ByteBuf> client = RxNetty.<ByteBuf, ByteBuf>newHttpClientBuilder("localhost", port)
                                                     .pipelineConfigurator(pipelineConfigurator)
                                                     .enableWireLogging(LogLevel.ERROR).build();
        HttpClientRequest<ByteBuf> request = HttpClientRequest.create(HttpMethod.POST, "test/post");

        FileContentSource fileContentSource = new FileContentSource(new File(textFile));
        request.withRawContentSource(fileContentSource, StringTransformer.DEFAULT_INSTANCE);

        return client.submit(request)
                     .flatMap(new Func1<HttpClientResponse<ByteBuf>, Observable<Integer>>() {
                         @Override
                         public Observable<Integer> call(HttpClientResponse<ByteBuf> response) {
                             return response.getContent()
                                            .map(new Func1<ByteBuf, Integer>() {
                                                @Override
                                                public Integer call(ByteBuf byteBuf) {
                                                    return Integer.parseInt(byteBuf.toString(Charset.defaultCharset()));
                                                }
                                            });
                         }
                     }).toBlocking().single();
    }

    static class FileContentSource extends Observable<String> {

        FileContentSource(final File file) {
            super(new OnSubscribe<String>() {

                @Override
                public void call(Subscriber<? super String> subscriber) {
                    try {
                        String nextLine;
                        final LineNumberReader reader =
                                new LineNumberReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(file))));
                        subscriber.add(Subscriptions.create(new Action0() {
                            @Override
                            public void call() {
                                try {
                                    reader.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }));
                        while ((nextLine = reader.readLine()) != null) {
                            subscriber.onNext(nextLine);
                        }

                        subscriber.onCompleted();
                    } catch (Throwable throwable) {
                        subscriber.onError(throwable);
                    }
                }
            });
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("ERROR: give text file name");
            return;
        }
        String textFile = args[0];
        try {
            int count = new WordCounterClient(DEFAULT_PORT, textFile).countWords();
            System.out.printf("Counted %d words in text file %s", count, textFile);
        } catch (IOException e) {
            System.err.println("ERROR: there is a problem with reading file " + textFile);
        }
    }
}
