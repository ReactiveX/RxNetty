package io.reactivex.netty.protocol.http.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;
import io.reactivex.netty.ChannelCloseListener;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.ConnectionHandler;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.server.ErrorHandler;
import io.reactivex.netty.server.RxServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;
import rx.functions.Func1;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

/**
 * @author Nitesh Kant
 */
public class UnexpectedErrorsTest {

    public static final int PORT = 1999;
    private RxServer<ByteBuf,ByteBuf> server;
    private final ChannelCloseListener channelCloseListener = new ChannelCloseListener();

    @Before
    public void setUp() throws Exception {
        server = RxNetty.createTcpServer(PORT, new PipelineConfigurator<ByteBuf, ByteBuf>() {
                                             @Override
                                             public void configureNewPipeline(ChannelPipeline pipeline) {
                                                 pipeline.addLast(channelCloseListener);
                                             }
                                         },
                                         new ConnectionHandler<ByteBuf, ByteBuf>() {
                                             @Override
                                             public Observable<Void> handle(
                                                     ObservableConnection<ByteBuf, ByteBuf> newConnection) {
                                                 return Observable.error(new IllegalStateException(
                                                         "I always throw an error."));
                                             }
                                         });
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
        server.waitTillShutdown();
    }

    @Test
    public void testErrorHandlerReturnsNull() throws Exception {
        TestableErrorHandler errorHandler = new TestableErrorHandler(null);
        server.withErrorHandler(errorHandler).start();

        blockTillConnected();
        channelCloseListener.waitForClose(1, TimeUnit.MINUTES);

        assertTrue("Error handler not invoked.", errorHandler.invoked);
    }

    @Test
    public void testConnectionHandlerReturnsError() throws Exception {
        TestableErrorHandler errorHandler = new TestableErrorHandler(
                Observable.<Void>error(new IllegalStateException("I always throw an error.")));

        server.withErrorHandler(errorHandler).start();

        blockTillConnected();

        channelCloseListener.waitForClose(1, TimeUnit.MINUTES);

        assertTrue("Error handler not invoked.", errorHandler.invoked);
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
