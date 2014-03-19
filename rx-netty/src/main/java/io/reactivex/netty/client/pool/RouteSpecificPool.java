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

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import rx.Observable;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.reactivex.netty.client.RxClient.ServerInfo;

/**
 * An implementation of {@link ChannelPool} that holds connections to a specific route. Exception will be
 * thrown if channel requested or released does not belong to the pool.
 * 
 * @author awang
 *
 */
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

    @Override
    protected Queue<Channel> removeFromIdleChannels(int numberDesired) {
        Queue<Channel> idleQueueToRemove = new ConcurrentLinkedQueue<Channel>();
        Channel idle;
        int count = 0;
        while ((idle = idleQueue.poll()) != null && (numberDesired <= 0  || count < numberDesired)) {
            idleQueueToRemove.add(idle);
            count++;
        }
        return idleQueueToRemove;
    }
}
