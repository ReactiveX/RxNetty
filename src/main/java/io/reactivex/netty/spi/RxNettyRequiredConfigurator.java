package io.reactivex.netty.spi;

import io.netty.channel.ChannelPipeline;
import io.reactivex.netty.ConnectionLifecycleHandler;
import io.reactivex.netty.NettyObservableAdapter;
import io.reactivex.netty.ObservableConnection;
import rx.Observable;
import rx.Observer;

/**
 * An implementation of {@link NettyPipelineConfigurator} which is ALWAYS added at the end of the pipeline. This
 * pipeline configurator brides between netty's pipeline processing and Rx {@link Observable}
 *
 * @param <I> Input type for the pipeline. This is the type one writes to this pipeline.
 * @param <O> Output type of the emitted observable.  This is the type one reads from this pipeline.
 *
 * @author Nitesh Kant
 */
public class RxNettyRequiredConfigurator<I, O> implements NettyPipelineConfigurator {

    public static final String CONN_LIFECYCLE_HANDLER_NAME = "conn_lifecycle_handler";
    public static final String NETTY_OBSERVABLE_ADAPTER_NAME = "netty_observable_adapter";

    private final Observer<? super ObservableConnection<I, O>> connectionObserver;

    public RxNettyRequiredConfigurator(final Observer<? super ObservableConnection<I, O>> connectionObserver) {
        this.connectionObserver = connectionObserver;
    }

    @Override
    public void configureNewPipeline(ChannelPipeline pipeline) {
        NettyObservableAdapter observableAdapter = new NettyObservableAdapter();
        ConnectionLifecycleHandler<I, O> lifecycleHandler = new ConnectionLifecycleHandler<I, O>(connectionObserver,
                                                                                                 observableAdapter);
        pipeline.addLast(CONN_LIFECYCLE_HANDLER_NAME, lifecycleHandler);
        pipeline.addLast(NETTY_OBSERVABLE_ADAPTER_NAME, observableAdapter);
    }
}
