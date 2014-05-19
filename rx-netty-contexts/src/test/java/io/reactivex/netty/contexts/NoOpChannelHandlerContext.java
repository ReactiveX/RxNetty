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
package io.reactivex.netty.contexts;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelProgressivePromise;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelProgressivePromise;
import io.netty.channel.DefaultChannelPromise;
import io.netty.channel.local.LocalChannel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.DefaultAttributeMap;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.GlobalEventExecutor;

import javax.naming.OperationNotSupportedException;
import java.net.SocketAddress;

/**
 * @author Nitesh Kant
 */
public class NoOpChannelHandlerContext implements ChannelHandlerContext {

    private final DefaultAttributeMap attributeMap = new DefaultAttributeMap();
    private final Channel channel;
    private final DefaultChannelPromise failedPromise;

    public NoOpChannelHandlerContext() {
        channel = new LocalChannel() {
            @Override
            public <T> Attribute<T> attr(AttributeKey<T> key) {
                return attributeMap.attr(key);
            }
        };
        failedPromise = new DefaultChannelPromise(channel, executor());
        failedPromise.setFailure(new OperationNotSupportedException());
    }

    @Override
    public Channel channel() {
        return channel;
    }

    @Override
    public EventExecutor executor() {
        return GlobalEventExecutor.INSTANCE;
    }

    @Override
    public String name() {
        return "";
    }

    @Override
    public ChannelHandler handler() {
        return null;
    }

    @Override
    public boolean isRemoved() {
        return false;
    }

    @Override
    public ChannelHandlerContext fireChannelRegistered() {
        return this;
    }

    @Override
    @Deprecated
    public ChannelHandlerContext fireChannelUnregistered() {
        return this;
    }

    @Override
    public ChannelHandlerContext fireChannelActive() {
        return this;
    }

    @Override
    public ChannelHandlerContext fireChannelInactive() {
        return this;
    }

    @Override
    public ChannelHandlerContext fireExceptionCaught(Throwable cause) {
        return this;
    }

    @Override
    public ChannelHandlerContext fireUserEventTriggered(Object event) {
        return this;
    }

    @Override
    public ChannelHandlerContext fireChannelRead(Object msg) {
        return this;
    }

    @Override
    public ChannelHandlerContext fireChannelReadComplete() {
        return this;
    }

    @Override
    public ChannelHandlerContext fireChannelWritabilityChanged() {
        return this;
    }

    @Override
    public ChannelHandlerContext flush() {
        return this;
    }

    @Override
    public <T> Attribute<T> attr(AttributeKey<T> key) {
        return attributeMap.attr(key);
    }

    @Override
    public ChannelFuture bind(SocketAddress localAddress) {
        return failedPromise;
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress) {
        return failedPromise;
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress) {
        return failedPromise;
    }

    @Override
    public ChannelFuture disconnect() {
        return failedPromise;
    }

    @Override
    public ChannelFuture close() {
        return failedPromise;
    }

    @Override
    @Deprecated
    public ChannelFuture deregister() {
        return failedPromise;
    }

    @Override
    public ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise) {
        return failedPromise;
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise) {
        return failedPromise;
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress,
                                 ChannelPromise promise) {
        return failedPromise;
    }

    @Override
    public ChannelFuture disconnect(ChannelPromise promise) {
        return failedPromise;
    }

    @Override
    public ChannelFuture close(ChannelPromise promise) {
        return failedPromise;
    }

    @Override
    @Deprecated
    public ChannelFuture deregister(ChannelPromise promise) {
        return failedPromise;
    }

    @Override
    public ChannelHandlerContext read() {
        return this;
    }

    @Override
    public ChannelFuture write(Object msg) {
        return failedPromise;
    }

    @Override
    public ChannelFuture write(Object msg, ChannelPromise promise) {
        return failedPromise;
    }

    @Override
    public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
        return failedPromise;
    }

    @Override
    public ChannelFuture writeAndFlush(Object msg) {
        return failedPromise;
    }

    @Override
    public ChannelPipeline pipeline() {
        return null;
    }

    @Override
    public ByteBufAllocator alloc() {
        return UnpooledByteBufAllocator.DEFAULT;
    }

    @Override
    public ChannelPromise newPromise() {
        return failedPromise;
    }

    @Override
    public ChannelProgressivePromise newProgressivePromise() {
        return new DefaultChannelProgressivePromise(channel, executor());
    }

    @Override
    public ChannelFuture newSucceededFuture() {
        return failedPromise;
    }

    @Override
    public ChannelFuture newFailedFuture(Throwable cause) {
        return failedPromise;
    }

    @Override
    public ChannelPromise voidPromise() {
        return failedPromise;
    }
}
