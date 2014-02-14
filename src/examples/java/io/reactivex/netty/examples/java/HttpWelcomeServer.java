package io.reactivex.netty.examples.java;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.server.HttpRequest;
import io.reactivex.netty.protocol.http.server.HttpResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import rx.Observable;

import java.util.Map;

/**
 * @author Nitesh Kant
 */
public final class HttpWelcomeServer {

    public static void main(final String[] args) {
        final int port = 8080;

        RxNetty.createHttpServer(port, new RequestHandler<ByteBuf, ByteBuf>() {
            @Override
            public Observable<Void> handle(HttpRequest<ByteBuf> request, final HttpResponse<ByteBuf> response) {
                System.out.println("New request recieved");
                System.out.println(request.getHttpMethod() + " " + request.getUri() + ' ' + request.getHttpVersion());
                for (Map.Entry<String, String> header : request.getHeaders().entries()) {
                    System.out.println(header.getKey() + ": " + header.getValue());
                }
                // This does not consume request content, need to figure out an elegant/correct way of doing that.
                return response.writeContentAndFlush("Welcome!!! \n\n");
            }
        }).startAndWait();
    }
}
