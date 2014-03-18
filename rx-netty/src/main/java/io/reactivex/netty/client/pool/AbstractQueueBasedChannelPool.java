package io.reactivex.netty.client.pool;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.reactivex.netty.client.RxClient.ServerInfo;

import java.net.InetSocketAddress;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import rx.Observable;
import rx.Subscriber;
import rx.Observable.OnSubscribe;

public abstract class AbstractQueueBasedChannelPool implements ChannelPool {

    public static class PoolExhaustedException extends Exception {
        private static final long serialVersionUID = 1L;

        public PoolExhaustedException(String arg0) {
            super(arg0);
        }
        
    }
    
    public static final long DEFAULT_IDLE_TIMEOUT = 30000;
    
    private AdjustableSemaphore maxConnectionsLimit;
    private long defaultIdleTimeout;
    private volatile int maxTotal;
    private final AtomicLong reuseCounter = new AtomicLong();
    private final AtomicLong creationCounter = new AtomicLong();
    private final AtomicLong deleteCounter = new AtomicLong();
    private final AtomicLong releaseCounter = new AtomicLong();
    private final AtomicLong failureRequestCounter = new AtomicLong();
    private final AtomicLong successfulRequestCounter = new AtomicLong();


    public AbstractQueueBasedChannelPool(int maxConnections, long defaultIdleTimeoutMillis) {
        this.maxConnectionsLimit = new AdjustableSemaphore(maxConnections);
        this.maxTotal = maxConnections;
        this.defaultIdleTimeout = defaultIdleTimeoutMillis;
    }
    
    public AbstractQueueBasedChannelPool(int maxConnections) {
        this(maxConnections, DEFAULT_IDLE_TIMEOUT);
    }

    
    private Channel getFreeChannel(ServerInfo serverInfo) {
        final Queue<Channel> idleQueue = getIdleQueue(serverInfo);
        if (idleQueue == null) {
            return null;
        } else {
            Channel freeChannel = idleQueue.poll();
            while (freeChannel != null) {
                if (!isReusable(freeChannel))  {
                    closeChannel(freeChannel);
                    freeChannel = idleQueue.poll();
                } else {
                    return freeChannel;
                }
            }            
        }
        return null;
    }

    private boolean isReusable(Channel channel) {
        if (!channel.isActive() || !channel.isRegistered()) {
            return false;
        } else {
            Long idleTimeout = channel.attr(ChannelPool.IDLE_TIMEOUT_ATTR).get();
            Long idleStart = channel.attr(ChannelPool.IDLE_START_ATTR).get();

            if (idleStart != null) {
                // this is the case where we are checking if an idle channel 
                // can be reused
                if (idleTimeout == null) {
                    idleTimeout = defaultIdleTimeout;
                } else {
                    // the Keep-Alive timeout is second
                    idleTimeout = idleTimeout * 1000;
                }
                long currentTime = System.currentTimeMillis();
                return idleStart + idleTimeout > currentTime;
            } else if (idleTimeout != null && idleTimeout.longValue() == 0) {
                // this is when we check if channel being released is reusable 
                return false;
            } else {
                return true;
            }

        }
    }
    
    // protected abstract Queue<Channel> getOrCreateIdleQueue(ServerInfo serverInfo);
    
    protected abstract Queue<Channel> getIdleQueue(ServerInfo serverInfo);
    
    @Override
    public Observable<Channel> requestChannel(final ServerInfo serverInfo,
            final Bootstrap bootStrap, final ChannelInitializer<? extends Channel> initializer) {
        return Observable.<Channel>create(new OnSubscribe<Channel>() {
            @SuppressWarnings({ "rawtypes", "unchecked" })
            @Override
            public void call(final Subscriber<? super Channel> subscriber) {  
                Channel freeChannel = getFreeChannel(serverInfo);
                if (freeChannel != null) {
                    reuseCounter.incrementAndGet();
                    final Channel channel = freeChannel;
                    // make sure the channel manipulation is done in the channel's 
                    // event loop
                    Future<?> future = freeChannel.eventLoop().submit(new Runnable() {
                        @Override
                        public void run() {
                            ChannelPipeline pipeline = channel.pipeline();
                            ChannelHandler handler;
                            while ((handler = pipeline.last()) != null) {
                                pipeline.remove(handler);
                            }
                            // re-initialize the channel manually
                            pipeline.addFirst(initializer);
                            pipeline.fireChannelRegistered();
                            pipeline.fireChannelActive();
                        }
                    });
                    future.addListener((GenericFutureListener) new GenericFutureListener<Future<?>>() {
                        @Override
                        public void operationComplete(Future<?> future) throws Exception {
                            if (future.isSuccess()) {
                                completeChannelRequest(serverInfo, channel, subscriber);
                            } else {
                                // something is wrong in the channel re-initialization, close the channel
                                closeChannel(channel);
                                failureRequestCounter.incrementAndGet();
                                subscriber.onError(future.cause());
                            }
                        }

                    });
                } else if (maxConnectionsLimit.tryAcquire()) {
                    bootStrap.handler(initializer).connect(serverInfo.getHost(), serverInfo.getPort()).addListener(new ChannelFutureListener() {                            
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            if (future.isSuccess()) {
                                creationCounter.incrementAndGet();
                                Channel channel = future.channel();
                                completeChannelRequest(serverInfo, channel, subscriber);
                            } else {
                                // channel is not created, release the permit previously acquired
                                maxConnectionsLimit.release();
                                failureRequestCounter.incrementAndGet();
                                subscriber.onError(future.cause());
                            }
                        }
                    });    
                } else {
                    failureRequestCounter.incrementAndGet();
                    subscriber.onError(new PoolExhaustedException("Pool has reached its maximal size " + maxTotal));
                }
            };
        });
    }

    private ChannelFuture closeChannel(Channel channel) {
        deleteCounter.incrementAndGet();
        maxConnectionsLimit.release();
        channel.attr(ChannelPool.POOL_ATTR).getAndRemove();
        channel.attr(ChannelPool.IDLE_START_ATTR).getAndRemove();
        channel.attr(ChannelPool.IDLE_TIMEOUT_ATTR).getAndRemove();
        return channel.close();
    }
    
    private void completeChannelRequest(ServerInfo serverInfo, Channel channel, final Subscriber<? super Channel> subscriber) {
        channel.attr(ChannelPool.IDLE_START_ATTR).getAndRemove();
        successfulRequestCounter.incrementAndGet();
        subscriber.onNext(channel);
        subscriber.onCompleted();        
    }
    
    @Override
    public Observable<Void> releaseChannel(Channel channel) {
        InetSocketAddress remoteAddress = (InetSocketAddress) channel.remoteAddress();
        ServerInfo serverInfo = new ServerInfo(remoteAddress.getHostName(), remoteAddress.getPort());
        Queue<Channel> idleQueue = getIdleQueue(serverInfo);
        releaseCounter.incrementAndGet();
        final ChannelFuture closeFuture;
        if (!isReusable(channel)) {
            closeFuture = closeChannel(channel);
        } else {
            closeFuture = null;
            idleQueue.add(channel);
            long currentTime = System.currentTimeMillis();
            channel.attr(ChannelPool.IDLE_START_ATTR).set(currentTime);
        }
        return Observable.<Void>create(new OnSubscribe<Void>() {
            @Override
            public void call(final Subscriber<? super Void> t1) {
                if (closeFuture != null) {
                    // this is the case where we cannot reuse the channel
                    // but have to close the channel
                    closeFuture.addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            if (future.isSuccess()) {
                                t1.onCompleted();
                            } else {
                                t1.onError(future.cause());
                            }
                        }
                    });    
                } else {
                    // we can reuse the channel
                    t1.onCompleted();
                }
            }
        });        
    }

    @Override
    public int getMaxTotal() {
        return maxTotal;
    }
    
    public long getReuseCount() {
        return reuseCounter.get();
    }
    
    public long getCreationCount() {
        return creationCounter.get();
    }
    
    public long getDeletionCount() {
        return deleteCounter.get();
    }
    
    public long getSuccessfulRequestCount() {
        return successfulRequestCounter.get();
    }
    
    public long getReleaseCount() {
        return releaseCounter.get();
    }
        
    public long getFailedRequestCount() {
        return failureRequestCounter.get();
    }

    @Override
    public synchronized void setMaxTotal(int newMax) {
        maxTotal = newMax;
        maxConnectionsLimit.setMaxPermits(newMax);
    }

    @Override
    public int getTotalChannelsInPool() {
        return maxTotal - maxConnectionsLimit.availablePermits();
    }
}
