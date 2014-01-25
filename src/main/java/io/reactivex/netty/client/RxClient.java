package io.reactivex.netty.client;

import io.reactivex.netty.ObservableConnection;
import rx.Observable;

/**
 * @author Nitesh Kant
 */
public interface RxClient<I, O> {

    /**
     * Creates exactly one new connection for every subscription to the returned observable.
     *
     * @return A new obserbvable which creates a single connection for every connection.
     */
    Observable<ObservableConnection<O, I>> connect();

    class ServerInfo {

        private final String host;
        private final int port;

        public ServerInfo(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }
    }
}
