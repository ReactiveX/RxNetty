package io.reactivex.netty.examples.java;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.reactivex.netty.ObservableConnection;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.HttpObjectAggregationConfigurator;
import io.reactivex.netty.protocol.http.HttpServer;
import io.reactivex.netty.protocol.http.HttpServerPipelineConfigurator;
import rx.Observer;
import rx.util.functions.Action1;

/**
 * @author Nitesh Kant
 */
public final class RawHttpServer {

    public static void main(final String[] args) throws InterruptedException {
        final int port = 8080;

        HttpServer<FullHttpRequest, HttpObject> server =
                RxNetty.createHttpServer(port, new HttpObjectAggregationConfigurator<FullHttpRequest, HttpObject>(new HttpServerPipelineConfigurator<HttpObject, HttpObject>()));

        server.start(new Action1<ObservableConnection<FullHttpRequest, HttpObject>>() {
            @Override
            public void call(final ObservableConnection<FullHttpRequest, HttpObject> connection) {
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
                        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
                        response.headers().add(HttpHeaders.Names.TRANSFER_ENCODING, "chunked");
                        connection.write(response).subscribe(new Observer<Void>() {
                            @Override
                            public void onCompleted() {
                                System.out.println("Header write successful.");
                                HttpContent content = new DefaultHttpContent(Unpooled.copiedBuffer(
                                        "Welcome! \n\n".getBytes()));
                                connection.write(content).doOnError(new Action1<Throwable>() {
                                    @Override
                                    public void call(Throwable throwable) {
                                        System.out.println("Content write failed.");
                                    }
                                });
                                connection.write(new DefaultLastHttpContent(Unpooled.copiedBuffer("".getBytes())))
                                          .doOnError(new Action1<Throwable>() {
                                              @Override
                                              public void call(Throwable throwable) {
                                                  System.out.println("Last HTTP content write failed.");
                                              }
                                          });
                            }

                            @Override
                            public void onError(Throwable e) {
                                System.out.println("Header write failed. Error: ");
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
