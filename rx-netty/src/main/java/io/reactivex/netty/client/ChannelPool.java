package io.reactivex.netty.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.util.AttributeKey;
import rx.Observable;

public interface ChannelPool {
    
    public static final AttributeKey<ChannelPool> POOL_ATTR = AttributeKey.<ChannelPool>valueOf("CHANNEL_POOL");
    public static final AttributeKey<Long> IDLE_START_ATTR = AttributeKey.<Long>valueOf("IDLE_START");
    public static final AttributeKey<Long> IDLE_TIMEOUT_ATTR = AttributeKey.<Long>valueOf("IDLE_TIMEOUT");

    public Observable<Channel> requestChannel(String host, int port, Bootstrap bootStrap, ChannelInitializer<? extends Channel> initializer);
    
    public Observable<Void> releaseChannel(Channel channel);
    
    public int getMaxTotal();
    
    public int getIdleChannels();
    
    public int getTotalChannelsInPool();
    
    public void setMaxTotal(int newMax);
}
