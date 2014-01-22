package io.reactivex.netty.http.sse.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

/**
 * An encoder to convert {@link SSEEvent} to a {@link ByteBuf}
 *
 * @author Nitesh Kant
 */
@ChannelHandler.Sharable
public class ServerSentEventEncoder extends MessageToMessageEncoder<SSEEvent> {

    @Override
    protected void encode(ChannelHandlerContext ctx, SSEEvent sseEvent, List<Object> out) throws Exception {
        StringBuilder eventBuilder = new StringBuilder();
        eventBuilder.append(sseEvent.getEventName());
        eventBuilder.append(": ");
        eventBuilder.append(sseEvent.getEventData());
        eventBuilder.append("\n\n");
        String data = eventBuilder.toString();
        out.add(ctx.alloc().buffer(data.length()).writeBytes(data.getBytes()));
    }
}
