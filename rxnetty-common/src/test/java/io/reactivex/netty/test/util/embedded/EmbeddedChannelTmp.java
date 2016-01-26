/*
 * Copyright 2016 Netflix, Inc.
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
 *
 */

package io.reactivex.netty.test.util.embedded;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.internal.PlatformDependent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A temporary extension to {@link EmbeddedChannel} to allow for pipeline modifications post creation.
 * Once the PR: https://github.com/netty/netty/pull/4655 is merged and released, this class can be removed.
 */
public class EmbeddedChannelTmp extends EmbeddedChannel {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddedChannelTmp.class);

    private final EmbeddedChannelPipeline embeddedPipeline;
    private Throwable lastException;

    public EmbeddedChannelTmp() {
        this(new ChannelHandler[0]);
    }

    public EmbeddedChannelTmp(ChannelHandler... handlers) {
        super(handlers);
        ChannelPipeline p = super.pipeline();
        p.removeLast(); // Removes the LastInboundHandler to add it later.
        p.addLast(LAST_HANDLER_NAME, new LastInboundHandler());
        embeddedPipeline = new EmbeddedChannelPipeline(p);
    }

    @Override
    public ChannelPipeline pipeline() {
        return null != embeddedPipeline ? embeddedPipeline : super.pipeline();
    }

    @Override
    public void checkException() {
        Throwable t = lastException;
        if (t == null) {
            return;
        }

        lastException = null;

        PlatformDependent.throwException(t);
    }

    private void recordException(Throwable cause) {
        if (lastException == null) {
            lastException = cause;
        } else {
            logger.warn(
                    "More than one exception was raised. " +
                    "Will report only the first one and log others.", cause);
        }
    }

    /**
     * Name of this handler in the pipeline.
     */
    public static final String LAST_HANDLER_NAME = "last-inbound-handler";

    /**
     * This handler is always the last inbound handler for an embedded channel pipeline.
     */
    public final class LastInboundHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            inboundMessages().add(msg);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            recordException(cause);
        }
    }
}
