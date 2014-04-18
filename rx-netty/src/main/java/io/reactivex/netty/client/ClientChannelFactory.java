package io.reactivex.netty.client;

import io.netty.channel.ChannelFuture;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import rx.Subscriber;

/**
 *
 * @param <I> The type of the object that is read from the channel created by this factory.
 * @param <O> The type of objects that are written to the channel created by this factory.
 *
 * @author Nitesh Kant
 */
public interface ClientChannelFactory<I, O> {

    ChannelFuture connect(Subscriber<? super ObservableConnection<I, O>> subscriber,
                          PipelineConfigurator<I, O> pipelineConfigurator);
}
