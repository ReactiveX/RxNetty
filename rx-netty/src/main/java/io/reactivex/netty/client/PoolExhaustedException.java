package io.reactivex.netty.client;

/**
 * @author Nitesh Kant
 */
public class PoolExhaustedException extends Exception {

    private static final long serialVersionUID = -6299997509113653123L;

    public PoolExhaustedException() {
    }

    public PoolExhaustedException(Throwable cause) {
        super(cause);
    }

    public PoolExhaustedException(String message) {
        super(message);
    }

    public PoolExhaustedException(String message, Throwable cause) {
        super(message, cause);
    }
}
