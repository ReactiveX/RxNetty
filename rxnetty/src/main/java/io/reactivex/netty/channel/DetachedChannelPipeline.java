/*
 * Copyright 2015 Netflix, Inc.
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
package io.reactivex.netty.channel;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerInvoker;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.util.concurrent.EventExecutorGroup;
import rx.functions.Action1;
import rx.functions.Func0;

import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * An implementation of {@link ChannelPipeline} which is detached from a channel and provides a
 * {@link #getChannelInitializer()} to be invoked when this pipeline handlers are to be added to an actual channel
 * pipeline.
 *
 * This must NOT be used on an actual channel, it does not support any channel operations. It only supports pipeline
 * modification operations.
 *
 * @author Nitesh Kant
 */
public class DetachedChannelPipeline {

    private final LinkedList<HandlerHolder> holdersInOrder;

    private final ChannelInitializer<Channel> channelInitializer = new ChannelInitializer<Channel>() {
        @Override
        protected void initChannel(Channel ch) throws Exception {
            final ChannelPipeline pipeline = ch.pipeline();
            synchronized (holdersInOrder) {
                unguardedCopyToPipeline(pipeline);
            }
        }
    };

    private final Func0<ChannelHandler> nullableTail;

    public DetachedChannelPipeline() {
        this(null);
    }

    public DetachedChannelPipeline(final Func0<ChannelHandler> nullableTail) {
        this.nullableTail = nullableTail;
        holdersInOrder = new LinkedList<>();
    }

    public DetachedChannelPipeline(final DetachedChannelPipeline copyFrom, final Func0<ChannelHandler> nullableTail) {
        this.nullableTail = nullableTail;
        holdersInOrder = new LinkedList<>();
        synchronized (copyFrom.holdersInOrder) {
            for (HandlerHolder handlerHolder : copyFrom.holdersInOrder) {
                holdersInOrder.addLast(handlerHolder);
            }
        }
    }

    public ChannelInitializer<Channel> getChannelInitializer() {
        return channelInitializer;
    }

    public DetachedChannelPipeline copy() {
        return new DetachedChannelPipeline(this, nullableTail);
    }

    public DetachedChannelPipeline addFirst(String name, Func0<ChannelHandler> handlerFactory) {
        return _guardedAddFirst(new HandlerHolder(name, handlerFactory));
    }

    public DetachedChannelPipeline addFirst(EventExecutorGroup group,
                                    String name, Func0<ChannelHandler> handlerFactory) {
        return _guardedAddFirst(new HandlerHolder(group, name, handlerFactory));
    }

    public DetachedChannelPipeline addFirst(ChannelHandlerInvoker invoker, String name, Func0<ChannelHandler> handlerFactory) {
        return _guardedAddFirst(new HandlerHolder(invoker, name, handlerFactory));
    }

    public DetachedChannelPipeline addLast(String name, Func0<ChannelHandler> handlerFactory) {
        return _guardedAddLast(new HandlerHolder(name, handlerFactory));
    }

    public DetachedChannelPipeline addLast(EventExecutorGroup group, String name, Func0<ChannelHandler> handlerFactory) {
        return _guardedAddLast(new HandlerHolder(group, name, handlerFactory));
    }

    public DetachedChannelPipeline addLast(ChannelHandlerInvoker invoker, String name, Func0<ChannelHandler> handlerFactory) {
        return _guardedAddLast(new HandlerHolder(invoker, name, handlerFactory));
    }

    public DetachedChannelPipeline addBefore(String baseName, String name, Func0<ChannelHandler> handlerFactory) {
        return _guardedAddBefore(baseName, new HandlerHolder(name, handlerFactory));
    }

    public DetachedChannelPipeline addBefore(EventExecutorGroup group, String baseName, String name, Func0<ChannelHandler> handlerFactory) {
        return _guardedAddBefore(baseName, new HandlerHolder(group, name, handlerFactory));
    }

    public DetachedChannelPipeline addBefore(ChannelHandlerInvoker invoker, String baseName, String name,
                                     Func0<ChannelHandler> handlerFactory) {
        return _guardedAddBefore(baseName, new HandlerHolder(invoker, name, handlerFactory));
    }

    public DetachedChannelPipeline addAfter(String baseName, String name, Func0<ChannelHandler> handlerFactory) {
        return _guardedAddAfter(baseName, new HandlerHolder(name, handlerFactory));
    }

    public DetachedChannelPipeline addAfter(EventExecutorGroup group, String baseName, String name, Func0<ChannelHandler> handlerFactory) {
        return _guardedAddAfter(baseName, new HandlerHolder(group, name, handlerFactory));
    }

    public DetachedChannelPipeline addAfter(ChannelHandlerInvoker invoker, String baseName, String name,
                                    Func0<ChannelHandler> handlerFactory) {
        return _guardedAddAfter(baseName, new HandlerHolder(invoker, name, handlerFactory));
    }

    @SafeVarargs
    public final DetachedChannelPipeline addFirst(Func0<ChannelHandler>... handlerFactories) {
        synchronized (holdersInOrder) {
            for (int i = handlerFactories.length - 1; i >= 0; i--) {
                Func0<ChannelHandler> handlerFactory = handlerFactories[i];
                holdersInOrder.addFirst(new HandlerHolder(handlerFactory));
            }
        }
        return this;
    }

    @SafeVarargs
    public final DetachedChannelPipeline addFirst(EventExecutorGroup group, Func0<ChannelHandler>... handlerFactories) {
        synchronized (holdersInOrder) {
            for (int i = handlerFactories.length - 1; i >= 0; i--) {
                Func0<ChannelHandler> handlerFactory = handlerFactories[i];
                holdersInOrder.addFirst(new HandlerHolder(group, null, handlerFactory));
            }
        }
        return this;
    }

    @SafeVarargs
    public final DetachedChannelPipeline addFirst(ChannelHandlerInvoker invoker,
                                                  Func0<ChannelHandler>... handlerFactories) {
        synchronized (holdersInOrder) {
            for (int i = handlerFactories.length - 1; i >= 0; i--) {
                Func0<ChannelHandler> handlerFactory = handlerFactories[i];
                holdersInOrder.addFirst(new HandlerHolder(invoker, null, handlerFactory));
            }
        }
        return this;
    }

    @SafeVarargs
    public final DetachedChannelPipeline addLast(Func0<ChannelHandler>... handlerFactories) {
        for (Func0<ChannelHandler> handlerFactory : handlerFactories) {
            _guardedAddLast(new HandlerHolder(handlerFactory));
        }
        return this;
    }

    @SafeVarargs
    public final DetachedChannelPipeline addLast(EventExecutorGroup group, Func0<ChannelHandler>... handlerFactories) {
        for (Func0<ChannelHandler> handlerFactory : handlerFactories) {
            _guardedAddLast(new HandlerHolder(group, null, handlerFactory));
        }
        return this;
    }

    @SafeVarargs
    public final DetachedChannelPipeline addLast(ChannelHandlerInvoker invoker,
                                                 Func0<ChannelHandler>... handlerFactories) {
        for (Func0<ChannelHandler> handlerFactory : handlerFactories) {
            _guardedAddLast(new HandlerHolder(invoker, null, handlerFactory));
        }
        return this;
    }

    public DetachedChannelPipeline configure(Action1<ChannelPipeline> configurator) {
        _guardedAddLast(new HandlerHolder(configurator));
        return this;
    }

    /*Visible for testing*/ LinkedList<HandlerHolder> getHoldersInOrder() {
        return holdersInOrder;
    }

    private void unguardedCopyToPipeline(ChannelPipeline pipeline) { /*To be guarded by lock on holders*/
        for (HandlerHolder holder : holdersInOrder) {
            if (holder.hasPipelineConfigurator()) {
                holder.getPipelineConfigurator().call(pipeline);
                continue;
            }

            if (holder.hasGroup()) {
                if (holder.hasName()) {
                    pipeline.addLast(holder.getGroupIfConfigured(), holder.getNameIfConfigured(),
                                     holder.getHandlerFactoryIfConfigured().call());
                } else {
                    pipeline.addLast(holder.getGroupIfConfigured(), holder.getHandlerFactoryIfConfigured().call());
                }
            } else if (holder.hasInvoker()) {
                if (holder.hasName()) {
                    pipeline.addLast(holder.getInvokerIfConfigured(), holder.getNameIfConfigured(),
                                     holder.getHandlerFactoryIfConfigured().call());
                } else {
                    pipeline.addLast(holder.getInvokerIfConfigured(), holder.getHandlerFactoryIfConfigured().call());
                }
            } else if (holder.hasName()) {
                pipeline.addLast(holder.getNameIfConfigured(), holder.getHandlerFactoryIfConfigured().call());
            } else {
                pipeline.addLast(holder.getHandlerFactoryIfConfigured().call());
            }
        }

        if (null != nullableTail) {
            pipeline.addLast(nullableTail.call()); // This is the last handler to be added to the pipeline always.
        }
    }

    private HandlerHolder unguardedFindHandlerByName(String baseName, boolean leniant) {
        for (HandlerHolder handlerHolder : holdersInOrder) {
            if (handlerHolder.hasName() && handlerHolder.getNameIfConfigured().equals(baseName)) {
                return handlerHolder;
            }
        }
        if (leniant) {
            return null;
        } else {
            throw new NoSuchElementException("No handler with name: " + baseName + " configured in the pipeline.");
        }
    }

    private DetachedChannelPipeline _guardedAddFirst(HandlerHolder toAdd) {
        synchronized (holdersInOrder) {
            holdersInOrder.addFirst(toAdd);
        }
        return this;
    }

    private DetachedChannelPipeline _guardedAddLast(HandlerHolder toAdd) {
        synchronized (holdersInOrder) {
            holdersInOrder.addLast(toAdd);
        }
        return this;
    }

    private DetachedChannelPipeline _guardedAddBefore(String baseName, HandlerHolder toAdd) {
        synchronized (holdersInOrder) {
            HandlerHolder before = unguardedFindHandlerByName(baseName, false);
            final int indexOfBefore = holdersInOrder.indexOf(before);
            holdersInOrder.add(indexOfBefore, toAdd);
        }
        return this;
    }

    private DetachedChannelPipeline _guardedAddAfter(String baseName, HandlerHolder toAdd) {
        synchronized (holdersInOrder) {
            HandlerHolder after = unguardedFindHandlerByName(baseName, false);
            final int indexOfAfter = holdersInOrder.indexOf(after);
            holdersInOrder.add(indexOfAfter + 1, toAdd);
        }
        return this;
    }

    /**
     * A holder class for holding handler information, required to add handlers to the actual pipeline.
     */
    /*Visible for testing*/ static class HandlerHolder {

        private final String nameIfConfigured;
        private final Func0<ChannelHandler> handlerFactoryIfConfigured;
        private final Action1<ChannelPipeline> pipelineConfigurator;
        private final ChannelHandlerInvoker invokerIfConfigured;
        private final EventExecutorGroup groupIfConfigured;

        HandlerHolder(Action1<ChannelPipeline> pipelineConfigurator) {
            this.pipelineConfigurator = pipelineConfigurator;
            nameIfConfigured = null;
            handlerFactoryIfConfigured = null;
            invokerIfConfigured = null;
            groupIfConfigured = null;
        }

        HandlerHolder(Func0<ChannelHandler> handlerFactory) {
            this(null, handlerFactory);
        }

        HandlerHolder(String name, Func0<ChannelHandler> handlerFactory) {
            this(name, handlerFactory, null, null);
        }

        HandlerHolder(ChannelHandlerInvoker invoker, String name, Func0<ChannelHandler> handlerFactory) {
            this(name, handlerFactory, invoker, null);
        }

        HandlerHolder(EventExecutorGroup group, String name, Func0<ChannelHandler> handlerFactory) {
            this(name, handlerFactory, null, group);
        }

        HandlerHolder(String name, Func0<ChannelHandler> handlerFactory, ChannelHandlerInvoker invoker,
                      EventExecutorGroup group) {
            nameIfConfigured = name;
            handlerFactoryIfConfigured = handlerFactory;
            invokerIfConfigured = invoker;
            groupIfConfigured = group;
            pipelineConfigurator = null;
        }

        public String getNameIfConfigured() {
            return nameIfConfigured;
        }

        public boolean hasName() {
            return null != nameIfConfigured;
        }

        public Func0<ChannelHandler> getHandlerFactoryIfConfigured() {
            return handlerFactoryIfConfigured;
        }

        public ChannelHandlerInvoker getInvokerIfConfigured() {
            return invokerIfConfigured;
        }

        public boolean hasInvoker() {
            return null != invokerIfConfigured;
        }

        public EventExecutorGroup getGroupIfConfigured() {
            return groupIfConfigured;
        }

        public boolean hasGroup() {
            return null != groupIfConfigured;
        }

        public Action1<ChannelPipeline> getPipelineConfigurator() {
            return pipelineConfigurator;
        }

        public boolean hasPipelineConfigurator() {
            return null != pipelineConfigurator;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof HandlerHolder)) {
                return false;
            }

            HandlerHolder that = (HandlerHolder) o;

            if (groupIfConfigured != null? !groupIfConfigured.equals(that.groupIfConfigured) :
                    that.groupIfConfigured != null) {
                return false;
            }
            if (handlerFactoryIfConfigured != null?
                    !handlerFactoryIfConfigured.equals(that.handlerFactoryIfConfigured) :
                    that.handlerFactoryIfConfigured != null) {
                return false;
            }
            if (invokerIfConfigured != null? !invokerIfConfigured.equals(that.invokerIfConfigured) :
                    that.invokerIfConfigured != null) {
                return false;
            }
            if (nameIfConfigured != null? !nameIfConfigured.equals(that.nameIfConfigured) :
                    that.nameIfConfigured != null) {
                return false;
            }
            if (pipelineConfigurator != null? !pipelineConfigurator.equals(that.pipelineConfigurator) :
                    that.pipelineConfigurator != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = nameIfConfigured != null? nameIfConfigured.hashCode() : 0;
            result = 31 * result + (handlerFactoryIfConfigured != null? handlerFactoryIfConfigured.hashCode() : 0);
            result = 31 * result + (pipelineConfigurator != null? pipelineConfigurator.hashCode() : 0);
            result = 31 * result + (invokerIfConfigured != null? invokerIfConfigured.hashCode() : 0);
            result = 31 * result + (groupIfConfigured != null? groupIfConfigured.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "HandlerHolder{" + "nameIfConfigured='" + nameIfConfigured + '\'' + ", handlerFactoryIfConfigured=" +
                   handlerFactoryIfConfigured + ", pipelineConfigurator=" + pipelineConfigurator +
                   ", invokerIfConfigured=" + invokerIfConfigured + ", groupIfConfigured=" + groupIfConfigured + '}';
        }
    }
}
