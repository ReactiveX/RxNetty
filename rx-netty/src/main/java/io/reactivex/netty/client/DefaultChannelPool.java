package io.reactivex.netty.client;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.reactivex.netty.client.RxClient.ServerInfo;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;

public class DefaultChannelPool implements ChannelPool {

    private static class RouteSpecificPool {
        final ConcurrentLinkedQueue<Channel> idleChannels = new ConcurrentLinkedQueue<Channel>();
        final ConcurrentLinkedQueue<Channel> busyChannels = new ConcurrentLinkedQueue<Channel>();
        final ServerInfo serverInfo;
        
        public RouteSpecificPool(ServerInfo serverInfo) {
            this.serverInfo = serverInfo;
        }
    }
    
    private AdjustableSemaphore maxConnectionsLimit;
    private volatile int maxTotal;
    private ConcurrentHashMap<ServerInfo, RouteSpecificPool> routePools = new ConcurrentHashMap<ServerInfo, RouteSpecificPool>();
    private final AtomicLong reuseCounter = new AtomicLong();
    private final AtomicLong creationCounter = new AtomicLong();
    private final AtomicLong deleteCounter = new AtomicLong();
    private final AtomicLong releaseCounter = new AtomicLong();
    private final AtomicLong requestCounter = new AtomicLong();

    public DefaultChannelPool(int maxConnections) {
        this.maxConnectionsLimit= new AdjustableSemaphore(maxConnections);
        this.maxTotal = maxConnections;
    }
    
    private Channel getFreeChannel(ServerInfo serverInfo, ChannelInitializer<? extends Channel> initializer) {
        RouteSpecificPool routePool = routePools.get(serverInfo);
        if (routePool == null) {
            return null;
        } else {
            Channel freeChannel = routePool.idleChannels.poll();
            while (freeChannel != null) {
                if (!isReusable(freeChannel))  {
                    closeChannel(freeChannel);
                    freeChannel = routePool.idleChannels.poll();
                } else {
                    break;
                }
            }            
            if (freeChannel != null) {
                reuseCounter.incrementAndGet();
                ChannelPipeline pipeline = freeChannel.pipeline();
                ChannelHandler handler;
                while ((handler = pipeline.last()) != null) {
                    pipeline.remove(handler);
                }
                pipeline.addFirst(initializer);
                pipeline.fireChannelRegistered();
                pipeline.fireChannelActive();
                routePool.busyChannels.add(freeChannel);  
                return freeChannel;
            }
        }
        return null;
    }

    
    private boolean isReusable(Channel channel) {
        return channel.isActive() && channel.isRegistered();
    }
    
    private RouteSpecificPool getOrCreateRoutePool(ServerInfo serverInfo) {
        RouteSpecificPool pool = routePools.get(serverInfo);
        if  (pool != null) {
            return pool; 
        } else {
            pool = new RouteSpecificPool(serverInfo);
            RouteSpecificPool old = routePools.putIfAbsent(serverInfo, pool);
            if (old != null) {
                return old;
            } else {
                return pool;
            }
        }
    }
    
    @Override
    public Observable<Channel> requestChannel(final String host, final int port,
            final Bootstrap bootStrap, final ChannelInitializer<? extends Channel> initializer) {
        requestCounter.incrementAndGet();
        return Observable.<Channel>create(new OnSubscribe<Channel>() {
            @Override
            public void call(final Subscriber<? super Channel> subscriber) {  
                final ServerInfo serverInfo = new ServerInfo(host, port);
                Channel freeChannel = getFreeChannel(serverInfo, initializer);
                if (freeChannel != null) {
                    subscriber.onNext(freeChannel);
                } else if (maxConnectionsLimit.tryAcquire()) {
                    creationCounter.incrementAndGet();
                    bootStrap.handler(initializer).connect(host, port).addListener(new ChannelFutureListener() {                            
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            if (future.isSuccess()) {
                                Channel channel = future.channel();
                                RouteSpecificPool pool = getOrCreateRoutePool(serverInfo);
                                pool.busyChannels.add(channel);
                                subscriber.onNext(channel);
                            } else {
                                maxConnectionsLimit.release();
                                subscriber.onError(future.cause());
                            }
                        }
                    });    
                } else {
                    subscriber.onError(new Exception("Unable to obtain channel"));
                }
            };
        });
    }

    private ChannelFuture closeChannel(Channel channel) {
        System.err.println("Closing channel");
        deleteCounter.incrementAndGet();
        maxConnectionsLimit.release();
        return channel.close();
    }
    
    @Override
    public Observable<Void> releaseChannel(Channel channel) {
        InetSocketAddress remoteAddress = (InetSocketAddress) channel.remoteAddress();
        ServerInfo serverInfo = new ServerInfo(remoteAddress.getHostName(), remoteAddress.getPort());
        RouteSpecificPool pool = routePools.get(serverInfo);
        if (pool == null) {
            return Observable.error(new Exception(String.format("The remote address %s is not associated with this pool", remoteAddress)));
        }
        pool.busyChannels.remove(channel);
        releaseCounter.incrementAndGet();
        final ChannelFuture closeFuture;
        if (!isReusable(channel)) {
            closeFuture = closeChannel(channel);
        } else {
            closeFuture = null;
            pool.idleChannels.add(channel);
        }
        return Observable.<Void>create(new OnSubscribe<Void>() {
            @Override
            public void call(final Subscriber<? super Void> t1) {
                // TODO Auto-generated method stub
                if (closeFuture != null) {
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
                    t1.onCompleted();
                }
            }
            
        });        
    }

    @Override
    public int getMaxTotal() {
        return maxTotal;
    }

    @Override
    public int getIdleChannels() {
        int total = 0;
        for (RouteSpecificPool pool: routePools.values()) {
            total += pool.idleChannels.size();
        }
        return total;
    }

    @Override
    public int getTotalChannelsInPool() {
        int total = 0;
        for (RouteSpecificPool pool: routePools.values()) {
            total += pool.idleChannels.size() + pool.busyChannels.size();
        }
        return total;
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
    
    public long getRequestCount() {
        return requestCounter.get();
    }
    
    public long getReleaseCount() {
        return releaseCounter.get();
    }

    @Override
    public void setMaxTotal(int newMax) {
        maxConnectionsLimit.setMaxPermits(newMax);
    }
}
