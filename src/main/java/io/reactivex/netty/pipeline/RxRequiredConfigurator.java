package io.reactivex.netty.pipeline;

import io.netty.channel.ChannelPipeline;
import io.reactivex.netty.ObservableConnection;
import rx.Observable;
import rx.Observer;

/**
 * An implementation of {@link PipelineConfigurator} which is ALWAYS added at the end of the pipeline. This
 * pipeline configurator brides between netty's pipeline processing and Rx {@link Observable}
 *
 * @param <I> Input type for the pipeline. This is the type one writes to this pipeline.
 * @param <O> Output type of the emitted observable.  This is the type one reads from this pipeline.
 *
 * @author Nitesh Kant
 */
public class RxRequiredConfigurator<I, O> implements PipelineConfigurator<Object, Object> {

    public static final String CONN_LIFECYCLE_HANDLER_NAME = "conn_lifecycle_handler";
    public static final String NETTY_OBSERVABLE_ADAPTER_NAME = "netty_observable_adapter";

    private final ConnectionLifecycleHandler<I, O> lifecycleHandler;
    private final ObservableAdapter observableAdapter;

    public RxRequiredConfigurator(final Observer<? super ObservableConnection<I, O>> connectionObserver) {
        observableAdapter = new ObservableAdapter();
        lifecycleHandler =  new ConnectionLifecycleHandler<I, O>(connectionObserver, observableAdapter);
        
    }

    public RxRequiredConfigurator(ConnectionLifecycleHandler<I, O> lifecycleHandler, ObservableAdapter observableAdapter) {
        this.lifecycleHandler = lifecycleHandler;
        this.observableAdapter = observableAdapter;
    }

    @Override
    public void configureNewPipeline(ChannelPipeline pipeline) {
        pipeline.addLast(CONN_LIFECYCLE_HANDLER_NAME, lifecycleHandler);
        pipeline.addLast(NETTY_OBSERVABLE_ADAPTER_NAME, observableAdapter);
    }
}
