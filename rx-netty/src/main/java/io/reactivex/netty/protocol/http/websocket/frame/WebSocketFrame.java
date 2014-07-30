package io.reactivex.netty.protocol.http.websocket.frame;

import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;

/**
 * To avoid dependency on netty WebSocket API we wrap WebSocket frame classes with ower own.
 *
 * For resource management, we do not implement {@link io.netty.util.ReferenceCounted}, but
 * we need those methods so we can release buffer after message is handled.
 *
 * @author Tomasz Bak
 */
public class WebSocketFrame {

    protected final io.netty.handler.codec.http.websocketx.WebSocketFrame nettyFrame;

    WebSocketFrame(io.netty.handler.codec.http.websocketx.WebSocketFrame nettyFrame) {
        this.nettyFrame = nettyFrame;
    }

    io.netty.handler.codec.http.websocketx.WebSocketFrame getNettyFrame() {
        return nettyFrame;
    }

    public boolean isFinalFragment() {
        return nettyFrame.isFinalFragment();
    }

    public int rsv() {
        return nettyFrame.rsv();
    }

    public ByteBuf content() {
        return nettyFrame.content();
    }

    public int refCnt() {
        return nettyFrame.refCnt();
    }

    public WebSocketFrame retain() {
        nettyFrame.retain();
        return this;
    }

    public WebSocketFrame retain(int increment) {
        nettyFrame.retain(increment);
        return this;
    }

    public boolean release() {
        return nettyFrame.release();
    }

    public boolean release(int decrement) {
        return nettyFrame.release(decrement);
    }

    @Override
    public String toString() {
        return "{finalFragment=" + isFinalFragment() + ", rsv=" + rsv() + ", content=" + content().toString(Charset.defaultCharset()) + '}';
    }

    static WebSocketFrame from(io.netty.handler.codec.http.websocketx.WebSocketFrame nettyFrame) {
        if (nettyFrame instanceof io.netty.handler.codec.http.websocketx.TextWebSocketFrame) {
            return new TextWebSocketFrame((io.netty.handler.codec.http.websocketx.TextWebSocketFrame) nettyFrame);
        }
        if (nettyFrame instanceof io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame) {
            return new BinaryWebSocketFrame((io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame) nettyFrame);
        }
        if (nettyFrame instanceof io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame) {
            return new ContinuationWebSocketFrame((io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame) nettyFrame);
        }
        if (nettyFrame instanceof io.netty.handler.codec.http.websocketx.PingWebSocketFrame) {
            return new PingWebSocketFrame((io.netty.handler.codec.http.websocketx.PingWebSocketFrame) nettyFrame);
        }
        if (nettyFrame instanceof io.netty.handler.codec.http.websocketx.PongWebSocketFrame) {
            return new PongWebSocketFrame((io.netty.handler.codec.http.websocketx.PongWebSocketFrame) nettyFrame);
        }
        if (nettyFrame instanceof io.netty.handler.codec.http.websocketx.CloseWebSocketFrame) {
            return new CloseWebSocketFrame((io.netty.handler.codec.http.websocketx.CloseWebSocketFrame) nettyFrame);
        }
        throw new IllegalArgumentException("Unrecognized websocket frame type: " + nettyFrame.getClass());
    }
}
