package io.reactivex.netty.examples.java;

import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.reactivex.netty.ObservableConnection;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.HttpServer;
import rx.Observer;
import rx.util.functions.Action1;

/**
 * @author Nitesh Kant
 */
public final class HttpWelcomeServer {

    public static void main(final String[] args) throws InterruptedException {
        final int port = 8080;

        HttpServer<FullHttpRequest, FullHttpResponse> server = RxNetty.createHttpServer(port);

        server.start(new Action1<ObservableConnection<FullHttpRequest, FullHttpResponse>>() {
            @Override
            public void call(final ObservableConnection<FullHttpRequest, FullHttpResponse> connection) {
                connection.getInput().subscribe(new Observer<FullHttpRequest>() {

                    @Override
                    public void onCompleted() {
                        System.out.println("Request/response completed.");
                    }

                    @Override
                    public void onError(Throwable e) {
                        System.out.println("Error while reading request. Error: ");
                        e.printStackTrace(System.out);
                    }

                    @Override
                    public void onNext(FullHttpRequest httpRequest) {
                        System.out.println("New request recieved: " + httpRequest);
                        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                                                                                HttpResponseStatus.OK);
                        response.content().writeBytes("Welcome! \n\n".getBytes());
                        // writing to the connection is the only place where anything is remote
                        connection.write(response).subscribe(new Observer<Void>() {
                            @Override
                            public void onCompleted() {
                                System.out.println("Response write successful.");
                            }

                            @Override
                            public void onError(Throwable e) {
                                System.out.println("Response write failed. Error: ");
                                e.printStackTrace(System.out);
                            }

                            @Override
                            public void onNext(Void args) {
                                // No op.
                            }
                        });
                    }
                });
            }
        });

        server.waitTillShutdown();
    }
}
