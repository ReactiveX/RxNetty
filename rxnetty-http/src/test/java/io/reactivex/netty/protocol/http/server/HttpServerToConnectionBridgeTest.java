package io.reactivex.netty.protocol.http.server;

import io.reactivex.netty.protocol.http.internal.AbstractHttpConnectionBridge;
import io.reactivex.netty.protocol.http.internal.AbstractHttpConnectionBridgeTest.AbstractHttpConnectionBridgeMock;
import io.reactivex.netty.protocol.http.internal.AbstractHttpConnectionBridgeTest.HandlerRule;
import io.reactivex.netty.protocol.http.internal.HttpContentSubscriberEvent;
import io.reactivex.netty.protocol.http.server.events.HttpServerEventPublisher;
import io.reactivex.netty.protocol.tcp.server.events.TcpServerEventPublisher;
import org.junit.Rule;
import org.junit.Test;
import rx.observers.TestSubscriber;

import java.nio.channels.ClosedChannelException;

public class HttpServerToConnectionBridgeTest {

    @Rule
    public final HandlerRule handlerRule = new HandlerRule() {
        @Override
        protected AbstractHttpConnectionBridge<String> newAbstractHttpConnectionBridgeMock() {
            return new HttpServerToConnectionBridge<>(new HttpServerEventPublisher(new TcpServerEventPublisher()));
        }
    };

    @Test(timeout = 60000)
    public void testPendingContentSubscriber() throws Exception {
        handlerRule.setupAndAssertConnectionInputSub();
        handlerRule.simulateHeaderReceive(); /*Simulate header receive, required for content sub.*/
        TestSubscriber<String> subscriber = new TestSubscriber<>();
        handlerRule.getChannel().pipeline().fireUserEventTriggered(new HttpContentSubscriberEvent<>(subscriber));
        TestSubscriber<String> subscriber1 = new TestSubscriber<>();
        handlerRule.getChannel().pipeline().fireUserEventTriggered(new HttpContentSubscriberEvent<>(subscriber1));

        subscriber.assertNoErrors();
        subscriber1.assertNoErrors();
        subscriber.unsubscribe();

        subscriber.assertUnsubscribed();

        handlerRule.getChannel().close().await();

        subscriber.assertNoErrors();
        subscriber1.assertError(ClosedChannelException.class);
    }
}