package io.reactivex.netty.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import rx.Observable;

public interface ChannelPool {
    public Observable<Channel> requestChannel(String host, int port, Bootstrap bootStrap, ChannelInitializer<? extends Channel> initializer);
    
    public Observable<Void> releaseChannel(Channel channel);
    
    public int getMaxTotal();
    
    public int getIdleChannels();
    
    public int getCurrentPoolSize();
}
