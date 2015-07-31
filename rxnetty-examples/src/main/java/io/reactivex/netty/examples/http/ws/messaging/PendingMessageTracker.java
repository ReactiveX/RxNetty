package io.reactivex.netty.examples.http.ws.messaging;

import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.reactivex.netty.examples.http.ws.messaging.MessageFrame.MessageType;
import io.reactivex.netty.util.UnicastBufferingSubject;
import rx.Observable;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public class PendingMessageTracker {

    private final ConcurrentLinkedQueue<Long> unacknowledgedIds;

    public PendingMessageTracker(UnicastBufferingSubject<WebSocketFrame> sender) {
        unacknowledgedIds = new ConcurrentLinkedQueue<>();
        Observable.interval(10, TimeUnit.SECONDS)
                  .forEach(aTick -> {
                      Long unacked;
                      while ((unacked = unacknowledgedIds.poll()) != null) {
                          sender.onNext(new MessageFrame(MessageType.Message, unacked));
                      }
                  });
    }

    public MessageFrame addPendingMessage(MessageFrame messageFrame) {
        unacknowledgedIds.add(messageFrame.getId());
        return messageFrame;
    }

    public MessageFrame removePendingMessage(BinaryWebSocketFrame bFrame) {
        MessageFrame mf = new MessageFrame(bFrame.content());
        unacknowledgedIds.remove(mf.getId());
        return mf;
    }
}
