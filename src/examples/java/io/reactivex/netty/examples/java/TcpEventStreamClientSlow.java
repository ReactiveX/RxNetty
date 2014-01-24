package io.reactivex.netty.examples.java;

import io.reactivex.netty.ObservableConnection;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import rx.Observable;
import rx.util.functions.Action1;
import rx.util.functions.Func1;

/**
 * @author Nitesh Kant
 */
public final class TcpEventStreamClientSlow {

    public static void main(String[] args) {
        Observable<ObservableConnection<String, String>> connectionObservable =
                RxNetty.createTcpClient("localhost", 8181, PipelineConfigurators.stringLineCodec());
        connectionObservable.flatMap(new Func1<ObservableConnection<String, String>, Observable<?>>() {
            @Override
            public Observable<?> call(ObservableConnection<String, String> connection) {
                return connection.getInput().map(new Func1<String, String>() {
                    @Override
                    public String call(String msg) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        return msg.trim();
                    }
                });
            }
        }).toBlockingObservable().forEach(new Action1<Object>() {
            @Override
            public void call(Object o) {
                System.out.println("onNext event => " + o);
            }
        });

    }
}
