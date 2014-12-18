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

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.server.HttpServer;

import java.io.FileReader;

import rx.observables.StringObservable;

/**
 * Server that reads a file and emits it over HTTP. 
 * <p>
 * NOTE: This is copying via user space to allow for transformations and just as a demo for streaming data. 
 * This should not be used as a file server which should be done with zero-copy support. 
 */
public class HttpChunkServer {
    static final int DEFAULT_PORT = 8103;

    private final int port;
    private final String textFile;

    public HttpChunkServer(int port, String textFile) {
        this.port = port;
        this.textFile = textFile;
    }

    public HttpServer<ByteBuf, ByteBuf> createServer() {
        return RxNetty.createHttpServer(port, (request, response) -> {
            return StringObservable.using(() -> new FileReader(textFile), (reader) -> StringObservable.from(reader))
                    .flatMap(text -> response.writeStringAndFlush(text));
        });
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("ERROR: give text file name");
            return;
        }
        String textFile = args[0];
        
        System.out.println("HTTP chunk server starting on port " + DEFAULT_PORT + " ...");
        new HttpChunkServer(DEFAULT_PORT, textFile).createServer().startAndWait();
    }
}
