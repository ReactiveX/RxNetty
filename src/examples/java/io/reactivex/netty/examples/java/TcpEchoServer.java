package io.reactivex.netty.examples.java;

import io.reactivex.netty.NettyServer;
import io.reactivex.netty.ObservableConnection;
import io.reactivex.netty.ProtocolHandlers;
import io.reactivex.netty.RxNetty;
import rx.Observer;
import rx.util.functions.Action1;

/**
 * @author Nitesh Kant
 */
public final class TcpEchoServer {

    public static void main(final String[] args) throws InterruptedException {
        final int port = 8181;
        NettyServer<String, String> tcpServer = RxNetty.createTcpServer(port, ProtocolHandlers.stringCodec());
        tcpServer.startNow(new Action1<ObservableConnection<String, String>>() {
            @Override
            public void call(final ObservableConnection<String, String> connection) {
                System.out.println("New client connection established.");
                // writing to the connection is the only place where anything is remote
                connection.writeNow("Welcome! \n\n");
                // perform echo logic and return the transformed output stream that will be subscribed to
                connection.getInput().subscribe(new Observer<String>() {
                    @Override
                    public void onCompleted() {
                        System.out.println("A client request completed.");
                    }

                    @Override
                    public void onError(Throwable e) {
                        System.out.println("Tcp server input stream got an error." + e.getMessage());
                    }

                    @Override
                    public void onNext(String msg) {
                        System.out.println("onNext: " + msg);
                        msg = msg.trim();
                        if (!msg.isEmpty()) {
                            connection.writeNow("echo => " + msg + '\n');
                        }
                    }
                });
            }
        }).subscribe(new Observer<Void>() {

            @Override
            public void onCompleted() {
                System.out.println("Tcp server on port: " + port + " stopped.");
            }

            @Override
            public void onError(Throwable e) {
                System.out.println("Error starting tcp server on port:  " + port);
            }

            @Override
            public void onNext(final Void newClientConnectedCallback) {
                System.out.println("Tcp server on port: " + port + " started");
            }
        });

        tcpServer.waitTillShutdown();
    }
}
