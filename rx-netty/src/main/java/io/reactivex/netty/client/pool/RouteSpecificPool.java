package io.reactivex.netty.client.pool;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import rx.Observable;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.reactivex.netty.client.RxClient.ServerInfo;

public class RouteSpecificPool extends AbstractQueueBasedChannelPool {

    private final Queue<Channel> idleQueue = new ConcurrentLinkedQueue<Channel>();
    private final ServerInfo serverInfo;
    
    public RouteSpecificPool(ServerInfo serverInfo, int maxConnections, long defaultIdleTimeout) {
        super(maxConnections, defaultIdleTimeout);
        this.serverInfo = serverInfo;
    }
    
    public RouteSpecificPool(ServerInfo serverInfo, int maxConnections) {
        super(maxConnections);
        this.serverInfo = serverInfo;
    }


    @Override
    public int getIdleChannels() {
        return idleQueue.size();
    }


    @Override
    protected Queue<Channel> getIdleQueue(ServerInfo serverInfo) {
        return getQueues(serverInfo);
    }
    
    private Queue<Channel> getQueues(ServerInfo serverInfo) {
        checkServer(serverInfo);
        return idleQueue;
    }

    private void checkServer(ServerInfo serverInfo) {
        if (!this.serverInfo.equals(serverInfo)) {
            throw new IllegalArgumentException(String.format("%s does not match this pool's specific route %s", serverInfo, this.serverInfo));
        }
    }
    
    @Override
    public Observable<Channel> requestChannel(ServerInfo serverInfo,
            Bootstrap bootStrap, ChannelInitializer<? extends Channel> initializer) {
        try {
            checkServer(serverInfo);
            return super.requestChannel(serverInfo, bootStrap, initializer);
        } catch (Exception e) {
            return Observable.<Channel>error(e);
        }
    }
}
