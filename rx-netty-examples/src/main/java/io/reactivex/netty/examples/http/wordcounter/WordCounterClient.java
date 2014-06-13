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
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.http.client.RawContentSource;
import io.reactivex.netty.serialization.ContentTransformer;
import io.reactivex.netty.serialization.StringTransformer;
import rx.functions.Action1;

import java.io.*;
import java.nio.charset.Charset;

/**
 * @author Tomasz Bak
 */
public class WordCounterClient {

    private final int port;
    private String textFile;

    public WordCounterClient(int port, String textFile) {
        this.port = port;
        this.textFile = textFile;
    }

    public int countWords() throws IOException {
        PipelineConfigurator<HttpClientResponse<ByteBuf>, HttpClientRequest<String>> pipelineConfigurator
                = PipelineConfigurators.httpClientConfigurator();

        HttpClient<String, ByteBuf> client = RxNetty.createHttpClient("localhost", port, pipelineConfigurator);
        HttpClientRequest<String> request = HttpClientRequest.create(HttpMethod.POST, "test/post");

        FileContentSource fileContentSource = new FileContentSource(new File(textFile));
        request.withRawContentSource(fileContentSource);

        WordAction wAction = new WordAction();
        client.submit(request).toBlocking().forEach(wAction);

        fileContentSource.close();

        return wAction.wordCount;
    }

    static class FileContentSource implements RawContentSource<String> {

        private final LineNumberReader fStream;
        private boolean opened;
        private String nextLine;

        FileContentSource(File file) throws IOException {
            this.fStream = new LineNumberReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(file))));
            this.opened = true;
        }

        void close() {
            if (fStream != null) {
                try {
                    fStream.close();
                } catch (IOException e) {
                    // IGNORE
                }
            }
        }

        @Override
        public boolean hasNext() {
            try {
                return opened && (nextLine != null || (nextLine = fStream.readLine()) != null);
            } catch (IOException e) {
                e.printStackTrace();
                opened = false;
                return false;
            }
        }

        @Override
        public String next() {
            if (hasNext()) {
                String response = nextLine + " ";
                nextLine = null;
                return response;
            }
            return null;
        }

        @Override
        public ContentTransformer<String> getTransformer() {
            return new StringTransformer();
        }
    }

    static class WordAction implements Action1<HttpClientResponse<ByteBuf>> {
        public volatile int wordCount;

        @Override
        public void call(HttpClientResponse<ByteBuf> response) {
            response.getContent().forEach(new Action1<ByteBuf>() {
                @Override
                public void call(ByteBuf content) {
                    wordCount = Integer.parseInt(content.toString(Charset.defaultCharset()));
                }
            });
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("ERROR: give server port number and text file name");
            return;
        }
        Integer port = Integer.valueOf(args[0]);
        String textFile = args[1];
        try {
            new WordCounterClient(port, textFile).countWords();
        } catch (IOException e) {
            System.err.println("ERROR: there is a problem with reading file " + textFile);
        }
    }
}
