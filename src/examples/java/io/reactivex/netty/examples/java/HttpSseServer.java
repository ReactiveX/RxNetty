package io.reactivex.netty.examples.java;

import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.reactivex.netty.ObservableConnection;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.HttpServer;
import io.reactivex.netty.protocol.text.sse.SSEEvent;
import rx.Notification;
import rx.Observable;
import rx.Observer;
import rx.util.functions.Action1;
import rx.util.functions.Func1;

import java.util.concurrent.TimeUnit;

/**
 * @author Nitesh Kant
 */
public final class HttpSseServer {

    public static void main(String[] args) throws InterruptedException {
        final int port = 8080;

        HttpServer<FullHttpRequest, Object> httpSseServer = RxNetty.createSseServer(port);

        httpSseServer.start(new Action1<ObservableConnection<FullHttpRequest, Object>>() {
            @Override
            public void call(final ObservableConnection<FullHttpRequest, Object> observableConnection) {
                observableConnection.getInput().subscribe(new Observer<FullHttpRequest>() {
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
                        observableConnection.write(new DefaultHttpResponse(HttpVersion.HTTP_1_1,
                                                                           HttpResponseStatus.OK));
                        getIntervalObservable(observableConnection).subscribe();
                    }
                });
            }
        });

        httpSseServer.waitTillShutdown();
    }


    private static Observable<Void> getIntervalObservable(final ObservableConnection<FullHttpRequest, Object> connection) {
        return Observable.interval(1000, TimeUnit.MILLISECONDS)
                         .flatMap(new Func1<Long, Observable<Notification<Void>>>() {
                             @Override
                             public Observable<Notification<Void>> call(Long interval) {
                                 System.out.println("Writing SSE event for interval: " + interval);
                                 return connection.write(new SSEEvent("1", "data: ", String.valueOf(interval))).materialize();
                             }
                         })
                         .takeWhile(new Func1<Notification<Void>, Boolean>() {
                             @Override
                             public Boolean call(Notification<Void> notification) {
                                 return !notification.isOnError();
                             }
                         })
                         .map(new Func1<Notification<Void>, Void>() {
                             @Override
                             public Void call(Notification<Void> notification) {
                                 return null;
                             }
                         });
    }
}
