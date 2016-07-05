package io.reactivex.netty.protocol.tcp.client.events;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.client.pool.PooledConnection;
import io.reactivex.netty.protocol.tcp.client.MockTcpClientEventListener;
import io.reactivex.netty.protocol.tcp.client.TcpClientRule;
import io.reactivex.netty.test.util.MockClientEventListener.ClientEvent;
import io.reactivex.netty.test.util.MockConnectionEventListener.Event;
import org.junit.Rule;
import org.junit.Test;
import rx.Observable;
import rx.observers.TestSubscriber;

public class TcpClientEventsTest {

    @Rule
    public final TcpClientRule clientRule = new TcpClientRule();

    @Test(timeout = 60000)
    public void testEventsPublished() throws Exception {
        MockTcpClientEventListener listener = sendRequests();

        listener.assertMethodCalled(ClientEvent.AcquireStart);
        listener.assertMethodCalled(ClientEvent.AcquireSuccess);
        listener.assertMethodCalled(ClientEvent.ConnectStart);
        listener.assertMethodCalled(ClientEvent.ConnectSuccess);
        listener.assertMethodCalled(Event.WriteStart);
        listener.assertMethodCalled(Event.WriteSuccess);
        listener.assertMethodCalled(Event.FlushStart);
        listener.assertMethodCalled(Event.FlushSuccess);
        listener.assertMethodCalled(Event.BytesRead);
    }

    protected MockTcpClientEventListener sendRequests() {
        clientRule.startServer(10);
        MockTcpClientEventListener listener = new MockTcpClientEventListener();
        clientRule.getClient().subscribe(listener);
        PooledConnection<ByteBuf, ByteBuf> connection = clientRule.connect();
        TestSubscriber<ByteBuf> testSubscriber = new TestSubscriber<>();
        connection.writeStringAndFlushOnEach(Observable.just("Hello"))
                  .toCompletable()
                  .<ByteBuf>toObservable()
                  .concatWith(connection.getInput())
                  .take(1)
                  .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        return listener;
    }
}
