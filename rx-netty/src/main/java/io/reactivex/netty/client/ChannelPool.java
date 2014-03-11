package io.reactivex.netty.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import rx.Observable;

public interface ChannelPool {
    public Observable<Channel> requestChannel(String host, int port, Bootstrap bootStrap);
    
    public Observable<Void> releaseChannel(Channel channel);

}
