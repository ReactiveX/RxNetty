package rx.netty.examples;

import io.netty.buffer.ByteBuf;

import java.nio.charset.Charset;

import rx.Observable;
import rx.netty.experimental.RxNetty;
import rx.netty.experimental.impl.TcpConnection;
import rx.util.functions.Action1;
import rx.util.functions.Func1;

public class EchoServer {

    public static void main(String[] args) {
        RxNetty.createTcpServer(8181)
                // process each connection in parallel
                .parallel(new Func1<Observable<TcpConnection<String, String>>, Observable<ReceivedMessage<String>>>() {

                    @Override
                    public Observable<ReceivedMessage<String>> call(Observable<TcpConnection<String, String>> o) {
                        // for each connection 
                        return o.flatMap(new Func1<TcpConnection<String, String>, Observable<ReceivedMessage<String>>>() {

                            @Override
                            public Observable<ReceivedMessage<String>> call(final TcpConnection<String, String> connection) {
                                // for each message we receive on the connection
                                return connection.getChannelObservable().map(new Func1<String, ReceivedMessage<String>>() {

                                    @Override
                                    public ReceivedMessage<String> call(String msg) {
                                        return new ReceivedMessage<String>(connection, msg.trim());
                                    }

                                });
                            }

                        });

                    }
                })
                .toBlockingObservable().forEach(new Action1<ReceivedMessage<String>>() {

                    @Override
                    public void call(ReceivedMessage<String> receivedMessage) {
                        receivedMessage.connection.write("Echo => " + receivedMessage.message + "\n");
                        System.out.println("Received Message: " + receivedMessage.message);
                    }
                });
    }

    public static class ReceivedMessage<I> {
        // I want tuples in java

        final TcpConnection<I, String> connection;
        final String message;

        public ReceivedMessage(TcpConnection<I, String> connection, String message) {
            this.connection = connection;
            this.message = message;
        }
    }
}
