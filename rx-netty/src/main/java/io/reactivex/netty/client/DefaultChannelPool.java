package io.reactivex.netty.client;

import io.reactivex.netty.client.RxClient.ServerInfo;

import java.util.concurrent.ConcurrentHashMap;

public class DefaultChannelPool extends AbstractQueueBasedChannelPool {

    private ConcurrentHashMap<ServerInfo, ChannelQueues> routePools = new ConcurrentHashMap<ServerInfo, ChannelQueues>();

    public DefaultChannelPool(int maxConnections, long defaultIdleTimeout) {
        super(maxConnections, defaultIdleTimeout);
    }
    
    public DefaultChannelPool(int maxConnections) {
        super(maxConnections);
    }

        
    @Override
    protected ChannelQueues getOrCreateChannelQueues(ServerInfo serverInfo) {
        ChannelQueues pool = routePools.get(serverInfo);
        if  (pool != null) {
            return pool; 
        } else {
            pool = new ChannelQueues();
            ChannelQueues old = routePools.putIfAbsent(serverInfo, pool);
            if (old != null) {
                return old;
            } else {
                return pool;
            }
        }
    }
    
    @Override
    protected ChannelQueues getChannelQueues(ServerInfo serverInfo) {
        return routePools.get(serverInfo);
    }
    
    @Override
    public int getIdleChannels() {
        int total = 0;
        for (ChannelQueues pool: routePools.values()) {
            total += pool.getIdleChannels().size();
        }
        return total;
    }

    @Override
    public int getTotalChannelsInPool() {
        int total = 0;
        for (ChannelQueues pool: routePools.values()) {
            total += pool.getIdleChannels().size() + pool.getBusyChannels().size();
        }
        return total;
    }
}
