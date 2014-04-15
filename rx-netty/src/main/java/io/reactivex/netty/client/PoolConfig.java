package io.reactivex.netty.client;

/**
* @author Nitesh Kant
*/
public class PoolConfig {

    public static final PoolConfig DEFAULT_CONFIG = new PoolConfig(30000);

    private final long maxIdleTimeMillis;

    public PoolConfig(long maxIdleTimeMillis) {
        this.maxIdleTimeMillis = maxIdleTimeMillis;
    }

    public long getMaxIdleTimeMillis() {
        return maxIdleTimeMillis;
    }
}
