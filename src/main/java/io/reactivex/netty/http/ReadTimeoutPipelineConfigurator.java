package io.reactivex.netty.http;

import io.netty.channel.ChannelPipeline;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.reactivex.netty.ObservableConnection;
import io.reactivex.netty.spi.NettyPipelineConfigurator;
import rx.Observer;

import java.util.concurrent.TimeUnit;

/**
 * An implementation of {@link NettyPipelineConfigurator} to configure a read time handler. <br/>
 * A read timeout is defined as lack of bytes read from the channel over the specified period. <br/>
 * This configurator, adds the {@link ReadTimeoutHandler} after every write and is removed after the {@link Observer}
 * associated with the response recieving, i.e., {@link ObservableConnection#getInput()} is completed.
 *
 * @see {@link ReadTimeoutHandler}
 *
 * @author Nitesh Kant
 */
public class ReadTimeoutPipelineConfigurator implements NettyPipelineConfigurator {

    public static final String READ_TIMEOUT_HANDLER_NAME = "readtimeout-handler";
    private final long timeout;
    private final TimeUnit timeUnit;

    public ReadTimeoutPipelineConfigurator(long timeout, TimeUnit timeUnit) {
        this.timeout = timeout;
        this.timeUnit = timeUnit;
    }

    @Override
    public void configureNewPipeline(ChannelPipeline pipeline) {
        pipeline.addFirst(READ_TIMEOUT_HANDLER_NAME, new ReadTimeoutHandler(timeout, timeUnit));
    }
}
