package io.reactivex.netty.examples.java;

import io.reactivex.netty.ObservableConnection;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import rx.Observable;
import rx.util.functions.Action0;
import rx.util.functions.Action1;
import rx.util.functions.Func1;

import java.util.concurrent.TimeUnit;

/**
 * @author Nitesh Kant
 */
public final class TcpEchoClient {

    public static void main(String[] args) {
        Observable<ObservableConnection<String, String>> connectionObservable =
                RxNetty.createTcpClient("localhost", 8181, PipelineConfigurators.textOnlyConfigurator()).connect();

        connectionObservable.flatMap(new Func1<ObservableConnection<String, String>, Observable<?>>() {
            @Override
            public Observable<?> call(final ObservableConnection<String, String> connection) {
                // we expect the EchoServer to output a single value at the beginning
                // so let's take the first value ... we can do this without it closing the connection
                // because the unsubscribe will hit the ChannelObservable is a PublishSubject
                // so we can re-subscribe to the 'hot' stream of data
                Observable<String> helloMessage = connection.getInput()
                                                            .take(1).map(new Func1<String, String>() {
                            @Override
                            public String call(String s) {
                                return s.trim();
                            }
                        });

                // output 10 values at intervals and receive the echo back
                Observable<String> intervalOutput =
                        Observable.interval(500, TimeUnit.MILLISECONDS)
                                  .flatMap(new Func1<Long, Observable<String>>() {
                                      @Override
                                      public Observable<String> call(Long aLong) {
                                          return connection.write(String.valueOf(aLong + 1))
                                                           .map(new Func1<Void, String>() {
                                                               @Override
                                                               public String call(Void aVoid) {
                                                                   return "";
                                                               }
                                                           });
                                      }
                                  });

                // capture the output from the server
                Observable<String> echo = connection.getInput().map(new Func1<String, String>() {
                    @Override
                    public String call(String s) {
                        return s.trim();
                    }
                });

                // wait for the helloMessage then start the output and receive echo input
                return Observable.concat(helloMessage, Observable.merge(intervalOutput, echo));
            }
        }).take(10).doOnCompleted(new Action0() {
            @Override
            public void call() {
                System.out.println("COMPLETED!");
            }
        }).toBlockingObservable().forEach(new Action1<Object>() {
            @Override
            public void call(Object o) {
                System.out.println("onNext: " + o);
            }
        });
    }
}
