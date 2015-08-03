package io.reactivex.netty.examples.http.ws.messaging;

import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import rx.functions.Func1;

public class AcceptOnlyBinaryFramesFilter implements Func1<WebSocketFrame, Boolean> {

    public static final AcceptOnlyBinaryFramesFilter INSTANCE = new AcceptOnlyBinaryFramesFilter();

    private AcceptOnlyBinaryFramesFilter() {
    }

    @Override
    public Boolean call(WebSocketFrame f) {
        if (f instanceof BinaryWebSocketFrame) {
            return true;
        } else {
            f.release();
            return false;
        }
    }
}