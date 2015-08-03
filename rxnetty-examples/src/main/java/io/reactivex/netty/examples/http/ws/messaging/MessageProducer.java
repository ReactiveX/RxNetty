package io.reactivex.netty.examples.http.ws.messaging;

import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.reactivex.netty.examples.http.ws.messaging.MessageFrame.MessageType;
import io.reactivex.netty.util.UnicastBufferingSubject;
import rx.Observable;
import rx.internal.operators.BufferUntilSubscriber;
import rx.internal.operators.OperatorOnBackpressureBuffer;

import java.util.concurrent.TimeUnit;

/**
 * An abstraction to demonstrate asynchronous message creation. This class is configured to send a finite number of
 * messages to a {@link UnicastBufferingSubject}
 */
public class MessageProducer {

    private final UnicastBufferingSubject<WebSocketFrame> sender;
    private final Observable<WebSocketFrame> messageStream;
    private final PendingMessageTracker messageTracker;

    public MessageProducer(int messagesToSend, long interval, TimeUnit intervalDuration) {
        BufferUntilSubscriber<WebSocketFrame> source = BufferUntilSubscriber.create();
        source.lift(new OperatorOnBackpressureBuffer<>(messagesToSend));

        sender = UnicastBufferingSubject.create(messagesToSend);
        messageTracker = new PendingMessageTracker(sender);
        messageStream = sender.filter(f -> f instanceof MessageFrame)
                              .cast(MessageFrame.class)
                              .map(messageTracker::addPendingMessage)
                              .cast(WebSocketFrame.class)
                              .concatWith(Observable.just(new CloseWebSocketFrame()));

        Observable.interval(interval, intervalDuration)
                  .take(messagesToSend)
                  .forEach(aTick -> sender.onNext(new MessageFrame(MessageType.Message, aTick)));
    }

    public Observable<WebSocketFrame> getMessageStream() {
        return messageStream;
    }

    public long acceptAcknowledgment(BinaryWebSocketFrame ackMessage) {
        MessageFrame frame = messageTracker.removePendingMessage(ackMessage);
        frame.release();
        return frame.getId();
    }
}
