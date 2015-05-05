/*
 * Copyright 2014 Netflix, Inc.
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
package io.reactivex.netty.pipeline;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.concurrent.EventExecutor;
import io.reactivex.netty.protocol.http.client.ClientRequestResponseConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * An implementation of {@link PipelineConfigurator} to configure a read time handler. <br/>
 * A read timeout is defined as lack of bytes read from the channel over the specified period. <br/>
 * This configurator, adds the {@link ReadTimeoutHandler} after every write, if not present.
 *
 * <h1>Reusable connections and timeout</h1>
 *
 * In cases where the connection is reused (like HTTP persistent connections), it is the responsibility of the protocol
 * to remove this timeout handler, for not being timed out (resulting in connection close) due to inactivity when the
 * connection is not in use.
 *
 * @see ReadTimeoutHandler
 *
 * @author Nitesh Kant
 */
@ChannelHandler.Sharable
public class ReadTimeoutPipelineConfigurator implements PipelineConfigurator<Object, Object> {

    private static final Logger logger = LoggerFactory.getLogger(ReadTimeoutPipelineConfigurator.class);

    public static final String READ_TIMEOUT_HANDLER_NAME = "readtimeout-handler";

    @SuppressWarnings("unused")
    @Deprecated
    public static final String READ_TIMEOUT_LIFECYCLE_MANAGER_HANDLER_NAME = "readtimeout-handler-lifecycle-manager";
    private final long timeout;
    private final TimeUnit timeUnit;

    public ReadTimeoutPipelineConfigurator(long timeout, TimeUnit timeUnit) {
        this.timeout = timeout;
        this.timeUnit = timeUnit;
    }

    @Override
    public void configureNewPipeline(ChannelPipeline pipeline) {
        pipeline.addFirst(READ_TIMEOUT_HANDLER_NAME, new InternalReadTimeoutHandler(timeout, timeUnit));
    }

    public static void disableReadTimeout(ChannelPipeline pipeline) {

        /**
         * Since, ChannelPipeline.remove() is blocking when not called from the associated eventloop, we do not remove
         * the handler. Instead we decativate the handler (invoked by the associated eventloop) here so that it does not
         * generate any more timeouts.
         * The handler is activated on next write to this pipeline.
         *
         * See issue: https://github.com/Netflix/RxNetty/issues/145
         */
        final ChannelHandler timeoutHandler = pipeline.get(READ_TIMEOUT_HANDLER_NAME);
        if (timeoutHandler != null && timeoutHandler instanceof InternalReadTimeoutHandler) {
            final ChannelHandlerContext handlerContext = pipeline.context(timeoutHandler);
            EventExecutor executor = handlerContext.executor();

            // Since, we are calling the handler directly, we need to make sure, it is in the owner eventloop, else it
            // can get concurrent callbacks.
            if (executor.inEventLoop()) {
                disableHandler(timeoutHandler, handlerContext);
            } else {
                executor.submit(new Callable<Object>() {

                    @Override
                    public Object call() throws Exception {
                        disableHandler(timeoutHandler, handlerContext);
                        return null;
                    }
                });
            }
        }
    }

    private static void disableHandler(ChannelHandler timeoutHandler, ChannelHandlerContext handlerContext) {
        InternalReadTimeoutHandler tHandler = (InternalReadTimeoutHandler) timeoutHandler;
        try {
            tHandler.cancelTimeoutSchedule(handlerContext);
        } catch (Exception e) {
            logger.error("Failed to disable read timeout handler. This connection will be discarded.", e);
            handlerContext.channel().attr(ClientRequestResponseConverter.DISCARD_CONNECTION).set(true);
        }
    }
}
