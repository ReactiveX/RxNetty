package io.reactivex.netty.examples.java;

import io.reactivex.netty.ObservableConnection;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.server.RxServer;
import rx.Observer;
import rx.util.functions.Action1;

/**
 * @author Nitesh Kant
 */
public final class TcpEchoServer {

    public static void main(final String[] args) throws InterruptedException {
        final int port = 8181;
        RxServer<String, String> tcpServer = RxNetty.createTcpServer(port, PipelineConfigurators.textOnlyConfigurator());
        tcpServer.start(new Action1<ObservableConnection<String, String>>() {
            @Override
            public void call(final ObservableConnection<String, String> connection) {
                System.out.println("New client connection established.");
                // writing to the connection is the only place where anything is remote
                connection.write("Welcome! \n\n");
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
                            connection.write("echo => " + msg + '\n');
                        }
                    }
                });
            }
        });

        tcpServer.waitTillShutdown();
    }
}
