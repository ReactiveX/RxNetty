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
package io.reactivex.netty.protocol.tcp;

import io.netty.channel.ChannelPipeline;

/**
 * Implement this interface to provide logic of handling an application protocol
 * on top of Netty's channel. The type I is the type of incoming data, and the type O
 * is the type of the outbound data
 * 
 */
public interface ProtocolHandler<I, O> {
    public void configure(ChannelPipeline pipeline);
}
