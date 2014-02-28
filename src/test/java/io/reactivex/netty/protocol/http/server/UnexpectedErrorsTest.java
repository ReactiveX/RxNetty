package io.reactivex.netty.protocol.http.server;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.ConnectionHandler;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.server.ErrorHandler;
import io.reactivex.netty.server.RxServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.testng.Assert;
import rx.Observable;
import rx.functions.Func1;

import java.util.concurrent.ExecutionException;

/**
 * @author Nitesh Kant
 */
public class UnexpectedErrorsTest {

    public static final int PORT = 1999;
    private RxServer<ByteBuf,ByteBuf> server;

    @Before
    public void setUp() throws Exception {
        server = RxNetty.createTcpServer(PORT, new ConnectionHandler<ByteBuf, ByteBuf>() {
            @Override
            public Observable<Void> handle(ObservableConnection<ByteBuf, ByteBuf> newConnection) {
                return Observable.error(new IllegalStateException("I always throw an error."));
            }
        });
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    public void testErrorHandlerReturnsNull() throws Exception {
        TestableErrorHandler errorHandler = new TestableErrorHandler(null);
        server.withErrorHandler(errorHandler).start();

        blockTillConnected();

        Thread.sleep(1000); // Sucks but we want to wait for the server connection handling to finish

        Assert.assertTrue(errorHandler.invoked, "Error handler not invoked.");
    }

    @Test
    public void testConnectionHandlerReturnsError() throws Exception {
        TestableErrorHandler errorHandler = new TestableErrorHandler(
                Observable.<Void>error(new IllegalStateException("I always throw an error.")));

        server.withErrorHandler(errorHandler).start();

        blockTillConnected();

        Thread.sleep(1000); // Sucks but we want to wait for the server connection handling to finish

        Assert.assertTrue(errorHandler.invoked, "Error handler not invoked.");
    }

    private static void blockTillConnected() throws InterruptedException, ExecutionException {
        RxNetty.createTcpClient("localhost", PORT).connect().flatMap(
                new Func1<ObservableConnection<ByteBuf, ByteBuf>, Observable<?>>() {
                    @Override
                    public Observable<Void> call(ObservableConnection<ByteBuf, ByteBuf> connection) {
                        return connection.close();
                    }
                }).toBlockingObservable().toFuture().get();
    }


    private static class TestableErrorHandler implements ErrorHandler {

        private final Observable<Void> toReturn;
        private boolean invoked;

        private TestableErrorHandler(Observable<Void> toReturn) {
            this.toReturn = toReturn;
        }

        @Override
        public Observable<Void> handleError(Throwable throwable) {
            invoked = true;
            return toReturn;
        }
    }
}
