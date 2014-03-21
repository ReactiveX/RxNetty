/*
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
import java.util.concurrent.atomic.AtomicLong;

import rx.Observable;
import rx.Subscriber;
import rx.Observable.OnSubscribe;

/**
 * A base implementation of {@link ChannelPool} that keeps idle connection for a route in a single queue
 * 
 * @author awang
 *
 */
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
                    // get an idle channel for the route that is reusable
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
                    // No idle channel for the route, but can create new channel
                    createChannel(serverInfo, bootStrap, initializer, subscriber);
                } else {
                    // get one idle channel from other route
                    Queue<Channel> idleChannels = removeFromIdleChannels(1);
                    Channel idleChannel = idleChannels.poll();
                    if (idleChannel != null) {
                        // close channel, but do not release permit as this is just an exchange
                        closeChannel(idleChannel, false);
                        createChannel(serverInfo, bootStrap, initializer, subscriber);
                    } else {
                        failureRequestCounter.incrementAndGet();
                        subscriber.onError(new PoolExhaustedException("Pool has reached its maximal size " + maxTotal));
                    }
                }
            };
        });
    }

    private ChannelFuture createChannel(final ServerInfo serverInfo, final Bootstrap bootStrap, 
            final ChannelInitializer<? extends Channel> initializer,  final Subscriber<? super Channel> subscriber) {
        return bootStrap.handler(initializer).connect(serverInfo.getHost(), serverInfo.getPort()).addListener(new ChannelFutureListener() {                            
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
    }
    
    private ChannelFuture closeChannel(Channel channel) {
        return closeChannel(channel, true);
    }
    
    private ChannelFuture closeChannel(Channel channel, boolean returnPermit) {
        deleteCounter.incrementAndGet();
        if (returnPermit) {
            maxConnectionsLimit.release();
        }
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

    /**
     * Remove a number of idle channels from any route and put them into the
     * queue to be returned. This is used by {@link #requestChannel(ServerInfo, Bootstrap, ChannelInitializer)}
     * when the pool has reached its channel limit, but has idle channels that can be 
     * removed so that new ones can be created. It is also used by {@link #cleanUpIdleChannels()}.
     *  
     * @param numberDesired number of channels to be removed from idle queue. If 0, all 
     *           idle channels are to be removed.
     * @return Queue that contains the channels that are removed from idle queues.
     */
    protected abstract Queue<Channel> removeFromIdleChannels(int numberDesired);
    
    /**
     * Remove all idle channels. 
     *  
     * @return number of idle channels removed
     */
    public int cleanUpIdleChannels() {
        Queue<Channel> idleChannels = removeFromIdleChannels(0);
        Channel channel;
        int count = 0;
        while ((channel = idleChannels.poll()) != null) {
            closeChannel(channel);
            count++;
        }
        return count;
    }
    
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

    public synchronized void setMaxTotal(int newMax) {
        maxTotal = newMax;
        maxConnectionsLimit.setMaxPermits(newMax);
    }

    public synchronized int getTotalChannelsInPool() {
        return maxTotal - maxConnectionsLimit.availablePermits();
    }
}
