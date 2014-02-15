package io.reactivex.netty.examples;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.client.HttpRequest;
import io.reactivex.netty.protocol.http.client.HttpResponse;

import java.nio.charset.Charset;

import rx.Observable;
import rx.util.functions.Action1;
import rx.util.functions.Func1;

public class HttpClient {

    public static void main(String[] args) {
        RxNetty.createHttpClient("localhost", 8080)
                .submit(HttpRequest.createGet("/hello"))
                .flatMap(new Func1<HttpResponse<ByteBuf>, Observable<String>>() {

                    @Override
                    public Observable<String> call(HttpResponse<ByteBuf> response) {
                        return response.getContent().map(new Func1<ByteBuf, String>() {

                            @Override
                            public String call(ByteBuf bb) {
                                return bb.toString(Charset.defaultCharset());
                            }

                        });
                    }

                }).toBlockingObservable().forEach(new Action1<String>() {

                    @Override
                    public void call(String s) {
                        System.out.println("Response: " + s);
                    }

                });
    }
}
