/*
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.reactivex.netty.protocol.tcp.ssl;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import io.reactivex.netty.protocol.tcp.ConnectionCreationFailedEvent;
import io.reactivex.netty.protocol.tcp.EmitConnectionEvent;
import rx.functions.Action1;

import static io.reactivex.netty.codec.HandlerNames.*;

/**
 * A codec to use when enabling SSL/TLS on <a href="http://en.wikipedia.org/wiki/Transport_Layer_Security">SSL
 * &middot; TLS</a> and StartTLS support for a TCP client/server.
 *
 * This codec requires an {@link SslHandler} instance and adds the necessary infrastructure required for the
 * TCP client/server to work.
 */
public abstract class SslCodec implements Action1<ChannelPipeline> {

    protected SslCodec() {
    }

    @Override
    public final void call(final ChannelPipeline pipeline) {
        final SslHandler sslHandler = newSslHandler(pipeline);
        ChannelHandler wireLogging = pipeline.get(WireLogging.getName());
        if (null != wireLogging) {
            /*So that, all activity on the channel is printed including SSL.*/
            pipeline.addAfter(WireLogging.getName(), SslHandler.getName(), sslHandler);
        } else {
            pipeline.addFirst(SslHandler.getName(), sslHandler);
        }

        pipeline.addAfter(SslHandler.getName(), SslConnectionEmissionHandler.getName(), new SslConnEmissionHandler());
    }

    protected abstract SslHandler newSslHandler(ChannelPipeline pipeline);

    private static final class SslConnEmissionHandler extends ChannelDuplexHandler {

        private boolean handshakeDone;

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            ctx.read(); // Read till handshake is over, else handshake will never be done, without reading from channel.
            super.channelActive(ctx);
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            if (!handshakeDone) {
                ctx.read(); /*Read till handshake over.*/
            }
            super.channelReadComplete(ctx);
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof SslHandshakeCompletionEvent) {
                handshakeDone = true;
                SslHandshakeCompletionEvent handshakeCompletionEvent = (SslHandshakeCompletionEvent) evt;
                if (handshakeCompletionEvent.isSuccess()) {
                    ctx.fireUserEventTriggered(EmitConnectionEvent.INSTANCE);
                } else {
                    ctx.fireUserEventTriggered(new ConnectionCreationFailedEvent(handshakeCompletionEvent.cause()));
                }
            }
            super.userEventTriggered(ctx, evt);
        }
    }
}
