package io.reactivex.netty.client;

import io.reactivex.netty.channel.RxDefaultThreadFactory;

/**
 * @author Nitesh Kant
 */
public class RxClientThreadFactory extends RxDefaultThreadFactory {

    public RxClientThreadFactory() {
        super("rx-client-selector");
    }
}
