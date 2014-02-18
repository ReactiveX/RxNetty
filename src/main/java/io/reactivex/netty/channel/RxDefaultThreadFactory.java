package io.reactivex.netty.channel;

import io.netty.util.concurrent.DefaultThreadFactory;

/**
 * @author Nitesh Kant
 */
public class RxDefaultThreadFactory extends DefaultThreadFactory {

    public RxDefaultThreadFactory(String threadNamePrefix) {
        super(threadNamePrefix, true);
    }
}
