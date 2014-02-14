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
package io.reactivex.netty.examples

class HttpSseClient {

    public static void main(String[] args) {
/*

        RxNetty.createSseClient("localhost", 8080)
                .submit(new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/hello"))
                .flatMap({ ObservableHttpResponse<SSEEvent> response ->
                    response.header().subscribe({HttpResponse header ->
                        println("New response recieved.");
                        println("========================");
                        println(header.getProtocolVersion().text() + ' ' + header.getStatus().code()
                                + ' ' + header.getStatus().reasonPhrase());
                        for (Map.Entry<String, String> aHeader : header.headers().entries()) {
                            println(aHeader.getKey() + ": " + aHeader.getValue());
                        }
                    })
                    return response.content();
                })
                .take(10)
                .toBlockingObservable().forEach({ println(it)});
*/
    }
}
