/**
 * Copyright 2013 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.reactivex.netty.protocol.http;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPipeline;

public class HttpProtocolHandlerAdapter<T> implements HttpProtocolHandler<T> {

    @Override
    public void configure(ChannelPipeline pipeline) {
    }

    @Override
    public void onChannelConnectOperationCompleted(ChannelFuture connectFuture) {
    }

    @Override
    public void onChannelWriteOperationCompleted(ChannelFuture requestWrittenFuture) {
    }

    @Override
    public void onChannelCloseOperationCompleted(ChannelFuture channelCloseFuture) {
    }
}
