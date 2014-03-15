package io.reactivex.netty.client;

import rx.Observable;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.reactivex.netty.client.RxClient.ServerInfo;

public class RouteSpecificPool extends AbstractQueueBasedChannelPool {

    private final ChannelQueues queues = new ChannelQueues();
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
        return queues.getIdleChannels().size();
    }

    @Override
    public int getTotalChannelsInPool() {
        return queues.getBusyChannels().size() + queues.getIdleChannels().size();
    }

    @Override
    protected ChannelQueues getOrCreateChannelQueues(ServerInfo serverInfo) {
        return getQueues(serverInfo);
    }

    @Override
    protected ChannelQueues getChannelQueues(ServerInfo serverInfo) {
        return getQueues(serverInfo);
    }
    
    private ChannelQueues getQueues(ServerInfo serverInfo) {
        checkServer(serverInfo);
        return queues;
    }

    private void checkServer(ServerInfo serverInfo) {
        if (!this.serverInfo.equals(serverInfo)) {
            throw new IllegalArgumentException(String.format("%s does not match this pool's specific route %s", serverInfo, this.serverInfo));
        }
    }
    
    @Override
    public Observable<Channel> requestChannel(String host, int port,
            Bootstrap bootStrap, ChannelInitializer<? extends Channel> initializer) {
        try {
            checkServer(new ServerInfo(host, port));
            return super.requestChannel(host, port, bootStrap, initializer);
        } catch (Exception e) {
            return Observable.<Channel>error(e);
        }
    }
}
