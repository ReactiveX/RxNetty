package io.reactivex.netty.client.pool;

import io.netty.channel.Channel;
import io.reactivex.netty.client.RxClient.ServerInfo;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DefaultChannelPool extends AbstractQueueBasedChannelPool {

    private ConcurrentHashMap<ServerInfo, Queue<Channel>> routeSpecificIdleQueues = new ConcurrentHashMap<ServerInfo, Queue<Channel>>();

    public DefaultChannelPool(int maxConnections, long defaultIdleTimeoutMillis) {
        super(maxConnections, defaultIdleTimeoutMillis);
    }
    
    public DefaultChannelPool(int maxConnections) {
        super(maxConnections);
    }

        
    @Override
    public Queue<Channel> getIdleQueue(ServerInfo serverInfo) {
        Queue<Channel> pool = routeSpecificIdleQueues.get(serverInfo);
        if  (pool != null) {
            return pool; 
        } else {
            pool = new ConcurrentLinkedQueue<Channel>();
            Queue<Channel> old = routeSpecificIdleQueues.putIfAbsent(serverInfo, pool);
            if (old != null) {
                return old;
            } else {
                return pool;
            }
        }
    }
        
    @Override
    public int getIdleChannels() {
        int total = 0;
        for (Queue<Channel> pool: routeSpecificIdleQueues.values()) {
            total += pool.size();
        }
        return total;
    }

    @Override
    protected Queue<Channel> removeFromIdleChannels(int numberDesired) {
        int count = 0;
        Queue<Channel> idleQueue = new ConcurrentLinkedQueue<Channel>();
        boolean done = false;
        for (Queue<Channel> queue: routeSpecificIdleQueues.values()) {
            while (!done) {
                Channel channel = queue.poll();
                if (channel == null) {
                    break;
                }
                idleQueue.add(channel);
                count++;
                if (numberDesired > 0 && count == numberDesired) {
                    done = true;
                }
            }
            if (done) {
                break;
            }
        }
        return idleQueue;
    }
}
