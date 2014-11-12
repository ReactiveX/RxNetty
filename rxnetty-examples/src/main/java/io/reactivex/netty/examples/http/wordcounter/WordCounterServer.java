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
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;

import java.nio.charset.Charset;
import java.util.regex.Pattern;

/**
 * @author Tomasz Bak
 */
public final class WordCounterServer {

    static final int DEFAULT_PORT = 8097;

    private final int port;

    public WordCounterServer(int port) {
        this.port = port;
    }

    public HttpServer<ByteBuf, ByteBuf> createServer() {
        HttpServer<ByteBuf, ByteBuf> server = RxNetty.createHttpServer(port, new RequestHandler<ByteBuf, ByteBuf>() {
            @Override
            public Observable<Void> handle(HttpServerRequest<ByteBuf> request,
                                           final HttpServerResponse<ByteBuf> response) {
                return request.getContent()
                              .map(new Func1<ByteBuf, String>() {
                                  @Override
                                  public String call(ByteBuf content) {
                                      return content.toString(Charset.defaultCharset());
                                  }
                              })
                              .lift(new WordSplitOperator())
                              .count()
                              .flatMap(new Func1<Integer, Observable<Void>>() {
                                  @Override
                                  public Observable<Void> call(Integer counter) {
                                      response.writeString(counter.toString());
                                      return response.close(false);
                                  }
                              });
            }
        });
        System.out.println("Started word counter server...");
        return server;
    }

    static class WordSplitOperator implements Observable.Operator<String, String> {

        private static final Pattern WORD_BOUNDARIES = Pattern.compile("[^\\w]{1,}");
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
                    String[] words = WORD_BOUNDARIES.split(lastFragment + text);
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

    public static void main(final String[] args) {
        new WordCounterServer(DEFAULT_PORT).createServer().startAndWait();
    }
}
