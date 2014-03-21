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
import io.netty.channel.ChannelInitializer;
import io.netty.util.AttributeKey;
import io.reactivex.netty.client.RxClient.ServerInfo;
import rx.Observable;

public interface ChannelPool {
    
    public static final AttributeKey<ChannelPool> POOL_ATTR = AttributeKey.<ChannelPool>valueOf("CHANNEL_POOL");
    public static final AttributeKey<Long> IDLE_START_ATTR = AttributeKey.<Long>valueOf("IDLE_START");
    public static final AttributeKey<Long> IDLE_TIMEOUT_ATTR = AttributeKey.<Long>valueOf("IDLE_TIMEOUT");

    public Observable<Channel> requestChannel(ServerInfo serverInfo, Bootstrap bootStrap, ChannelInitializer<? extends Channel> initializer);
    
    public Observable<Void> releaseChannel(Channel channel);
}
