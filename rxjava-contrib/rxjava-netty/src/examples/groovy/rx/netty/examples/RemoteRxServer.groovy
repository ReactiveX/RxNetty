package rx.netty.examples

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import rx.Observable;
import rx.experimental.remote.RemoteObserver;
import rx.netty.experimental.impl.TcpConnection;

class RemoteRxServer {

    
    
    /**
     * This wrapper will eventually replace TcpConnection if this experiment works out.
     *
     * Doing it all here locally while exploring this RemoteRx model.
     */
    public static class RemoteConnection {
        private final TcpConnection connection;

        public RemoteConnection(TcpConnection connection) {
            this.connection = connection;
        }

        public RemoteObserver<ByteBuf> getOutputObserver() {

            /* real implementation should likely have a single RemoteObserver created by the constructor
             * so multiple calls to getOutputObserver return the same impl since once it is completed
             * the output stream will be closed.
             */

            return new RemoteObserver<ByteBuf>() {
                public Observable<Void> onNext(ByteBuf t) {
                    return connection.write(t);
                }

                public Observable<Void> onError(Throwable t) {
                    // determine a correct protocol for delivering errors
                    return connection.write("Error: " + t.getMessage());
                }

                public Observable<Void> onCompleted() {
                    ChannelFuture f = connection.close();

                    // TODO this is a blocking Observable ... make it use the listener callback instead
                    return Observable.from(f);
                }
            };
        }

        public Observable<ByteBuf> getInputObservable() {
            return connection.getChannelObservable();
        }
    }
}
