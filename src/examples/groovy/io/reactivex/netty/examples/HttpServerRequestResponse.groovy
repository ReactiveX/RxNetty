package io.reactivex.netty.examples

import java.util.concurrent.TimeUnit;

import rx.Observable;
import io.netty.buffer.ByteBuf;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.server.HttpRequest;
import io.reactivex.netty.protocol.http.server.HttpResponse;
import io.reactivex.netty.server.RxServer;

class HttpServerRequestResponse {

    public static void main(String[] args) {

        RxNetty.createHttpServer(8080, { HttpRequest<ByteBuf> request, HttpResponse<ByteBuf> response ->
            if(request.getUri().equals("/hello")) {
                return response.writeAndFlush("Hello World!\n");
            } else if(request.getUri().equals("/events")) {
                return Observable.interval(500, TimeUnit.MILLISECONDS).doOnNext({ num ->
                    println("emitting event: " + num)
                    response.writeAndFlush("Event: " + num + "\n");
                })
                .take(10)
                .doOnCompleted({ response.writeAndFlush("Event: completed\n"); });
            } else if(request.getUri().equals("/error")) {
                return Observable.error(new RuntimeException("user error"));
            } else if(request.getUri().equals("/fatal")) {
                return null;
            } else {
                return response.writeAndFlush("You didn't say hello.\n");
            }
        }).startAndWait();
    }
}
