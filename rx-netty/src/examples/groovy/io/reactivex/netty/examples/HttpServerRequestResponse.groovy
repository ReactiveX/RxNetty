package io.reactivex.netty.examples

import io.netty.buffer.ByteBuf
import io.reactivex.netty.RxNetty
import io.reactivex.netty.protocol.http.server.HttpServerRequest
import io.reactivex.netty.protocol.http.server.HttpServerResponse
import rx.Observable

import java.util.concurrent.TimeUnit

class HttpServerRequestResponse {

    public static void main(String[] args) {

        RxNetty.createHttpServer(8080, { HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response ->
            if(request.getUri().equals("/hello")) {
                return response.writeStringAndFlush("Hello World!\n");
            } else if(request.getUri().equals("/events")) {
                return Observable.interval(500, TimeUnit.MILLISECONDS).doOnNext({ num ->
                    println("emitting event: " + num)
                    response.writeStringAndFlush("Event: " + num + "\n");
                })
                .take(10)
                .doOnCompleted({ response.writeStringAndFlush("Event: completed\n"); });
            } else if(request.getUri().equals("/error")) {
                return Observable.error(new RuntimeException("user error"));
            } else if(request.getUri().equals("/fatal")) {
                return null;
            } else {
                return response.writeStringAndFlush("You didn't say hello.\n");
            }
        }).startAndWait();
    }
}
