package io.reactivex.netty.examples


import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf
import io.reactivex.netty.RxNetty
import io.reactivex.netty.protocol.http.client.HttpClientRequest
import io.reactivex.netty.protocol.http.client.HttpClientResponse

public class HttpClientRequestResponse {

    public static void main(String[] args) {

        RxNetty.createHttpClient("localhost", 8080)
                .submit(HttpClientRequest.createGet("/hello"))
                .flatMap({ HttpClientResponse<ByteBuf> response ->
                    println("Status: " + response.getStatus());
                    return response.getContent().map({
                        println(it.toString(Charset.defaultCharset()))
                    });
                }).toBlockingObservable().last();
    }
}
