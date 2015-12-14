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
 *
 */
package io.reactivex.netty.channel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.FileRegion;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.internal.TypeParameterMatcher;
import io.reactivex.netty.HandlerNames;
import rx.Observable;
import rx.annotations.Beta;
import rx.functions.Action1;
import rx.functions.Func1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An implementation of {@link Connection} primarily intended to transform read and write types of a connection and
 * delegate all the other operations to a provided connection implementation.
 *
 * <h2>Manipulating writes</h2>
 *
 * In order to manipulate writes to the connection, this class provides a {@link Transformer} abstraction that produces
 * one or more items for every item transformed.
 *
 * These transformers are useful when the produced item requires {@link ByteBuf} allocations, for which the
 * transformation is invoked by passing a {@link ByteBufAllocator}. For transformations that do not need a
 * {@link ByteBuf} allocation, can be done by overriding the appropriate methods in this class.
 *
 * <h2>Manipulating reads</h2>
 *
 * Reads can be manipulated by implementing the {@link #getInput()} method.
 *
 * @param <R> Type of objects read from the underlying connection.
 * @param <W> Type of objects written to the underlying connection.
 * @param <RR> Type of objects read from this connection.
 * @param <WW> Type of objects written to this connection.
 */
@Beta
public abstract class AbstractDelegatingConnection<R, W, RR, WW> extends Connection<RR, WW> {

    private static final AttributeKey<List<Transformer>> transformerKey =
            AttributeKey.newInstance("_rxnetty-transformer-1-*-key");

    private final Connection<R, WW> delegate;

    protected AbstractDelegatingConnection(Connection<R, WW> delegate) {
        super(delegate.unsafeNettyChannel());
        this.delegate = delegate;
    }

    protected AbstractDelegatingConnection(Connection<R, W> delegate, final Transformer<WW, W> transformer) {
        super(delegate.unsafeNettyChannel());
        this.delegate = delegate.pipelineConfigurator(new Action1<ChannelPipeline>() {
            @Override
            public void call(ChannelPipeline pipeline) {
                final List<Transformer> finalTrans = getTransformersFromChannel(pipeline, transformer, transformerKey);
                MessageToMessageEncoder<WW> encoder = new MessageToMessageEncoder<WW>() {

                    @Override
                    protected void encode(ChannelHandlerContext ctx, WW msg, List<Object> out) throws Exception {
                        List<Object> lastTransform = Collections.singletonList((Object) msg);

                        for (Transformer transformer : finalTrans) {
                            List<Object> thisTransformResult = null;
                            for (Object toTransform : lastTransform) {
                                List<Object> transformed = transform(ctx, toTransform, transformer);
                                if (null == thisTransformResult) {
                                    thisTransformResult = new ArrayList<>();
                                }

                                thisTransformResult.addAll(transformed);
                            }
                            lastTransform = null != thisTransformResult ? thisTransformResult : lastTransform;
                        }

                        out.addAll(lastTransform);
                    }

                    @SuppressWarnings("unchecked")
                    private List<Object> transform(ChannelHandlerContext ctx, Object msg, Transformer transformer) {
                        return transformer.transform(msg, ctx.alloc());
                    }

                    @Override
                    public boolean acceptOutboundMessage(Object msg) throws Exception {
                        return transformer.acceptMessage(msg);
                    }
                };

                addOrReplaceEncoder("write-transformer-1-*-dynamic", pipeline, encoder);
            }
        });
    }

    @Override
    public abstract ContentSource<RR> getInput();

    @Override
    public Observable<Void> write(Observable<WW> msgs) {
        return delegate.write(msgs);
    }

    @Override
    public Observable<Void> write(Observable<WW> msgs, Func1<WW, Boolean> flushSelector) {
        return delegate.write(msgs, flushSelector);
    }

    @Override
    public Observable<Void> writeAndFlushOnEach(Observable<WW> msgs) {
        return delegate.writeAndFlushOnEach(msgs);
    }

    @Override
    public Observable<Void> writeBytes(Observable<byte[]> msgs) {
        return delegate.writeBytes(msgs);
    }

    @Override
    public Observable<Void> writeBytes(Observable<byte[]> msgs, Func1<byte[], Boolean> flushSelector) {
        return delegate.writeBytes(msgs, flushSelector);
    }

    @Override
    public Observable<Void> writeBytesAndFlushOnEach(Observable<byte[]> msgs) {
        return delegate.writeBytesAndFlushOnEach(msgs);
    }

    @Override
    public Observable<Void> writeFileRegion(Observable<FileRegion> msgs) {
        return delegate.writeFileRegion(msgs);
    }

    @Override
    public Observable<Void> writeFileRegion(Observable<FileRegion> msgs,
                                            Func1<FileRegion, Boolean> flushSelector) {
        return delegate.writeFileRegion(msgs, flushSelector);
    }

    @Override
    public Observable<Void> writeFileRegionAndFlushOnEach(Observable<FileRegion> msgs) {
        return delegate.writeFileRegionAndFlushOnEach(msgs);
    }

    @Override
    public Observable<Void> writeString(Observable<String> msgs) {
        return delegate.writeString(msgs);
    }

    @Override
    public Observable<Void> writeString(Observable<String> msgs,
                                        Func1<String, Boolean> flushSelector) {
        return delegate.writeString(msgs, flushSelector);
    }

    @Override
    public Observable<Void> writeStringAndFlushOnEach(Observable<String> msgs) {
        return delegate.writeStringAndFlushOnEach(msgs);
    }

    @Override
    public void flush() {
        delegate.flush();
    }

    @Override
    public void closeNow() {
        delegate.closeNow();
    }

    @Override
    public Observable<Void> close(boolean flush) {
        return delegate.close(flush);
    }

    @Override
    public Observable<Void> close() {
        return delegate.close();
    }

    @Override
    public ChannelPipeline getChannelPipeline() {
        return delegate.getChannelPipeline();
    }

    @Override
    public MarkAwarePipeline getResettableChannelPipeline() {
        return delegate.getResettableChannelPipeline();
    }

    @Override
    public Observable<Void> ignoreInput() {
        return delegate.ignoreInput();
    }

    @Override
    public Channel unsafeNettyChannel() {
        return delegate.unsafeNettyChannel();
    }

    @Override
    public Observable<Void> closeListener() {
        return delegate.closeListener();
    }

    @Override
    public <RRR, WWW> Connection<RRR, WWW> addChannelHandlerAfter(String baseName, String name,
                                                                  ChannelHandler handler) {
        return delegate.addChannelHandlerAfter(baseName, name, handler);
    }

    @Override
    public <RRR, WWW> Connection<RRR, WWW> addChannelHandlerAfter(EventExecutorGroup group,
                                                                  String baseName, String name,
                                                                  ChannelHandler handler) {
        return delegate.addChannelHandlerAfter(group, baseName, name, handler);
    }

    @Override
    public <RRR, WWW> Connection<RRR, WWW> addChannelHandlerBefore(String baseName, String name,
                                                                   ChannelHandler handler) {
        return delegate.addChannelHandlerBefore(baseName, name, handler);
    }

    @Override
    public <RRR, WWW> Connection<RRR, WWW> addChannelHandlerBefore(EventExecutorGroup group,
                                                                   String baseName, String name,
                                                                   ChannelHandler handler) {
        return delegate.addChannelHandlerBefore(group, baseName, name, handler);
    }

    @Override
    public <RRR, WWW> Connection<RRR, WWW> addChannelHandlerFirst(EventExecutorGroup group,
                                                                  String name, ChannelHandler handler) {
        return delegate.addChannelHandlerFirst(group, name, handler);
    }

    @Override
    public <RRR, WWW> Connection<RRR, WWW> addChannelHandlerFirst(String name, ChannelHandler handler) {
        return delegate.addChannelHandlerFirst(name, handler);
    }

    @Override
    public <RRR, WWW> Connection<RRR, WWW> addChannelHandlerLast(EventExecutorGroup group,
                                                               String name, ChannelHandler handler) {
        return delegate.addChannelHandlerLast(group, name, handler);
    }

    @Override
    public <RRR, WWW> Connection<RRR, WWW> addChannelHandlerLast(String name, ChannelHandler handler) {
        return delegate.addChannelHandlerLast(name, handler);
    }

    @Override
    public <RRR, WWW> Connection<RRR, WWW> pipelineConfigurator(Action1<ChannelPipeline> pipelineConfigurator) {
        return delegate.pipelineConfigurator(pipelineConfigurator);
    }

    private static void addOrReplaceEncoder(String handlerName, ChannelPipeline pipeline,
                                            MessageToMessageEncoder<?> encoder) {
        synchronized (pipeline.channel()) {
            ChannelHandler existing = pipeline.get(handlerName);
            if (null == existing) {
                pipeline.addAfter(HandlerNames.PrimitiveConverter.getName(), handlerName, encoder);
            } else {
                pipeline.replace(handlerName, handlerName, encoder);
            }
        }
    }

    private static <T> List<T> getTransformersFromChannel(ChannelPipeline pipeline, T transformer,
                                                          AttributeKey<List<T>> key) {

        List<T> trans;

        /*Only one AbstractDelegatingConnection instance can modify the attribute to hold the transformers
         at a time. Once the attribute is set, the transformers list contents do not change*/
        synchronized (pipeline.channel()) {
            trans = pipeline.channel().attr(key).get();
            if (null == trans) {
                trans = Collections.singletonList(transformer);
            } else {
                List<T> newTrans = new ArrayList<>(trans.size() + 1);
                newTrans.add(transformer);
                newTrans.addAll(trans);
                trans = Collections.unmodifiableList(newTrans);
            }

            pipeline.channel().attr(key).set(trans);
        }

        return trans;
    }

    /**
     * A transformer that produces one or more items for every item transformed.
     *
     * @param <X> Type of object to be transformed.
     * @param <XX> Type of object produced after transformation.
     */
    public static abstract class Transformer<X, XX> {

        private final TypeParameterMatcher matcher;

        protected Transformer() {
            matcher = TypeParameterMatcher.find(this, Transformer.class, "X");
        }

        protected final boolean acceptMessage(Object msg) {
            return matcher.match(msg);
        }

        public abstract List<XX> transform(X toTransform, ByteBufAllocator allocator);

    }
}
