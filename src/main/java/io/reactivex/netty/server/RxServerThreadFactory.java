package io.reactivex.netty.server;

import io.reactivex.netty.channel.RxDefaultThreadFactory;

/**
 * @author Nitesh Kant
 */
public class RxServerThreadFactory extends RxDefaultThreadFactory {

    public RxServerThreadFactory() {
        super("rx-server-selector");
    }
}
