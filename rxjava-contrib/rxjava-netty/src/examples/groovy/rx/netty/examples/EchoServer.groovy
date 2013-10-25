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
                .parallel({ Observable<TcpConnection> o ->
                    // for each connection
                    return o.flatMap({ TcpConnection connection ->
                        // for each message we receive on the connection
                        return connection.getChannelObservable().map({ ByteBuf bb ->
                            String msg = bb.toString(Charset.forName("UTF8")).trim();
                            return new ReceivedMessage(connection, msg);
                        });
                    });
                })
                .toBlockingObservable().forEach({ ReceivedMessage receivedMessage ->
                    receivedMessage.connection.write("Echo => " + receivedMessage.message + "\n");
                    System.out.println("Received Message: " + receivedMessage.message);
                });
    }

    def static class ReceivedMessage {
        // I want value types

        final TcpConnection connection;
        final String message;

        public ReceivedMessage(TcpConnection connection, String message) {
            this.connection = connection;
            this.message = message;
        }
    }
}
