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

package io.reactivex.netty.examples.http.chunk;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientPipelineConfigurator;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;
import rx.functions.Func2;

import java.nio.charset.Charset;
import java.util.regex.Pattern;

import static io.reactivex.netty.examples.http.chunk.HttpChunkServer.DEFAULT_PORT;

/**
 * @author Tomasz Bak
 */
public class HttpChunkClient {

    private final int port;

    public HttpChunkClient(int port) {
        this.port = port;
    }

    public int filterWords(final String word) {
        PipelineConfigurator<HttpClientResponse<ByteBuf>, HttpClientRequest<ByteBuf>> configurator =
                new HttpClientPipelineConfigurator<ByteBuf, ByteBuf>();

        HttpClient<ByteBuf, ByteBuf> client =
                RxNetty.createHttpClient("localhost", port, configurator);

        int count = client.submit(HttpClientRequest.createGet("/chunkedResponse"))
                .flatMap(new Func1<HttpClientResponse<ByteBuf>, Observable<String>>() {
                    @Override
                    public Observable<String> call(HttpClientResponse<ByteBuf> response) {
                        return response.getContent().map(new Func1<ByteBuf, String>() {
                            @Override
                            public String call(ByteBuf content) {
                                return content.toString(Charset.defaultCharset());
                            }
                        });
                    }
                })
                .lift(new WordSplitOperator())
                .map(new Func1<String, Integer>() {
                    @Override
                    public Integer call(String someWord) {
                        return someWord.equals(word) ? 1 : 0;
                    }
                })
                .reduce(new Func2<Integer, Integer, Integer>() {
                    @Override
                    public Integer call(Integer accumulator, Integer value) {
                        return accumulator + value;
                    }
                }).toBlocking().last();
        return count;
    }

    static class WordSplitOperator implements Observable.Operator<String, String> {

        private static final Pattern WORD_BOUNDARY_PATTERN = Pattern.compile("[^\\w]{1,}");
        private String lastFragment = "";

        @Override
        public Subscriber<? super String> call(final Subscriber<? super String> child) {
            return new Subscriber<String>() {

                @Override
                public void onCompleted() {
                    if (!lastFragment.isEmpty()) {
                        child.onNext(lastFragment);
                    }
                    child.onCompleted();
                }

                @Override
                public void onError(Throwable e) {
                    child.onError(e);
                }

                @Override
                public void onNext(String text) {
                    if (text.isEmpty()) {
                        return;
                    }
                    String[] words = WORD_BOUNDARY_PATTERN.split(lastFragment + text);
                    int take = words.length;
                    if (Character.isLetter(text.charAt(text.length() - 1))) {
                        lastFragment = words[--take];
                    } else {
                        lastFragment = "";
                    }
                    for (int i = 0; i < take; i++) {
                        child.onNext(words[i]);
                    }
                }
            };
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("ERROR: give 'word' parameter");
            System.exit(-1);
        }
        String word = args[0];
        System.out.println("Sending GET request...");
        System.out.printf("Counted %d words '%s'", new HttpChunkClient(DEFAULT_PORT).filterWords(word), word);
    }
}
