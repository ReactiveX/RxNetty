package rx.netty.examples;

import io.netty.buffer.ByteBuf;

import java.nio.charset.Charset;

import rx.Observable;
import rx.netty.experimental.RxNetty;
import rx.netty.experimental.impl.TcpConnection;
import rx.util.functions.Action1;
import rx.util.functions.Func1;

public class IntervalClientWithDisconnect {

    public static void main(String[] args) {
        new IntervalClientWithDisconnect().run();
    }

    public void run() {
        Observable<TcpConnection<ByteBuf, String>> client = RxNetty.createTcpClient("localhost", 8181);

        client.flatMap(new Func1<TcpConnection<ByteBuf, String>, Observable<String>>() {

            @Override
            public Observable<String> call(TcpConnection<ByteBuf, String> connection) {

                System.out.println("received connection: " + connection);

                Observable<String> subscribeMessage = connection.write("subscribe:")
                    // the intent of the flatMap to string is so onError can
                    // be propagated via the concat below
                    .flatMap(new Func1<Void, Observable<String>>() {

                        @Override
                        public Observable<String> call(Void t1) {
                            System.out.println("Send subscribe!");
                            return Observable.empty();
                        }

                    });

                Observable<String> messageHandling = connection.getChannelObservable().map(new Func1<ByteBuf, String>() {

                    @Override
                    public String call(ByteBuf bb) {
                        return bb.toString(Charset.forName("UTF8")).trim();
                    }

                });

                return Observable.concat(subscribeMessage, messageHandling);
            }
        })
                .take(10)
                .toBlockingObservable().forEach(new Action1<String>() {

                    @Override
                    public void call(String v) {
                        System.out.println("Received: " + v);
                    }

                });

    }
}
