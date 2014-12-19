/*
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.reactivex.netty.examples.http.chunk;

import static io.reactivex.netty.examples.http.chunk.HttpChunkServer.DEFAULT_PORT;
import io.netty.buffer.ByteBuf;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientPipelineConfigurator;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;

import java.nio.charset.Charset;
import java.util.regex.Pattern;

import rx.Observable;
import rx.Subscriber;

/**
 * This reads a stream of data and splits it into a stream of words across HTTP chunks.
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

        return client.submit(HttpClientRequest.createGet("/chunkedResponse"))
                .flatMap(response -> {
                    return response.getContent().map((ByteBuf content) -> {
                        return content.toString(Charset.defaultCharset());
                    });
                })
                .lift(new WordSplitOperator())
                .map(someWord -> someWord.equals(word) ? 1 : 0)
                .reduce((Integer accumulator, Integer value) -> {
                    return accumulator + value;
                }).toBlocking().last();
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
