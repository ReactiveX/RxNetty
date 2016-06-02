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
package io.reactivex.netty.client.pool;

import io.netty.channel.EventLoop;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.FastThreadLocal;
import io.reactivex.netty.client.ClientConnectionToChannelBridge;
import io.reactivex.netty.threads.PreferCurrentEventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Actions;
import rx.functions.Func0;

import java.util.ArrayList;

/**
 * An {@link IdleConnectionsHolder} implementation that can identify if the calling thread is an {@link EventLoop} in
 * the provided {@link PreferCurrentEventLoopGroup} and prefers a connection registered with the calling
 * {@link EventLoop}.
 *
 * If the calling thread is not an {@link EventLoop} in the provided {@link PreferCurrentEventLoopGroup} then
 * {@link #poll()} and {@link #peek()} will iterate over connections from all {@link EventLoop}s however
 * {@link #add(PooledConnection)} will attempt to find the {@link EventLoop} of the added {@link PooledConnection}. If
 * the {@link EventLoop} of the connection does not belong to the provided {@link PreferCurrentEventLoopGroup} then the
 * connection will be discarded.
 *
 * @param <W> Type of object that is written to the client using this holder.
 * @param <R> Type of object that is read from the the client using this holder.
 */
public class PreferCurrentEventLoopHolder<W, R> extends IdleConnectionsHolder<W, R> {

    private static final Logger logger = LoggerFactory.getLogger(PreferCurrentEventLoopHolder.class);

    private final FastThreadLocal<IdleConnectionsHolder<W, R>> perElHolder = new FastThreadLocal<>();
    private final ArrayList<IdleConnectionsHolder<W, R>> allElHolders;
    private final Observable<PooledConnection<R, W>> pollObservable;
    private final Observable<PooledConnection<R, W>> peekObservable;

    PreferCurrentEventLoopHolder(PreferCurrentEventLoopGroup eventLoopGroup) {
        this(eventLoopGroup, new FIFOIdleConnectionsHolderFactory<W, R>());
    }

    PreferCurrentEventLoopHolder(PreferCurrentEventLoopGroup eventLoopGroup,
                                 final IdleConnectionsHolderFactory<W, R> holderFactory) {
        final ArrayList<IdleConnectionsHolder<W, R>> _allElHolders = new ArrayList<>();
        allElHolders = _allElHolders;
        for (final EventExecutor child : eventLoopGroup) {
            final IdleConnectionsHolder<W, R> newHolder = holderFactory.call();
            allElHolders.add(newHolder);
            child.submit(new Runnable() {
                @Override
                public void run() {
                    perElHolder.set(newHolder);
                }
            });
        }

        Observable<PooledConnection<R, W>> pollOverAllHolders = Observable.empty();
        Observable<PooledConnection<R, W>> peekOverAllHolders = Observable.empty();

        for (IdleConnectionsHolder<W, R> anElHolder : allElHolders) {
            pollOverAllHolders = pollOverAllHolders.concatWith(anElHolder.poll());
            peekOverAllHolders = peekOverAllHolders.concatWith(anElHolder.peek());
        }

        pollObservable = pollOverAllHolders;
        peekObservable = peekOverAllHolders;
    }

    @Override
    public Observable<PooledConnection<R, W>> poll() {
        return pollObservable;
    }

    @Override
    public Observable<PooledConnection<R, W>> pollThisEventLoopConnections() {

        return Observable.create(new OnSubscribe<PooledConnection<R, W>>() {
            @Override
            public void call(Subscriber<? super PooledConnection<R, W>> subscriber) {
                final IdleConnectionsHolder<W, R> holderForThisEL = perElHolder.get();
                if (null == holderForThisEL) {
                    /*Caller is not an eventloop*/
                    PreferCurrentEventLoopHolder.super.pollThisEventLoopConnections().unsafeSubscribe(subscriber);
                } else {
                    holderForThisEL.poll().unsafeSubscribe(subscriber);
                }
            }
        });
    }

    @Override
    public Observable<PooledConnection<R, W>> peek() {
        return peekObservable;
    }

    @Override
    public void add(final PooledConnection<R, W> toAdd) {
        final IdleConnectionsHolder<W, R> holderForThisEL = perElHolder.get();
        if (null != holderForThisEL) {
            holderForThisEL.add(toAdd);
        } else {
            /*
             * This should not happen as the code generally adds the connection from within an eventloop.
             * By executing the add on the eventloop, the owner eventloop is correctly discovered for this eventloop.
             */
            toAdd.unsafeNettyChannel().eventLoop().execute(new Runnable() {
                @Override
                public void run() {
                    IdleConnectionsHolder<W, R> holderForThisEl = perElHolder.get();
                    if (null == holderForThisEl) {
                        logger.error("Unrecognized eventloop: " + Thread.currentThread().getName() +
                                     ". Returned connection can not be added to the pool. Closing the connection.");
                        toAdd.unsafeNettyChannel().attr(ClientConnectionToChannelBridge.DISCARD_CONNECTION).set(true);
                        toAdd.close().subscribe(Actions.empty(), new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                logger.error("Failed to discard connection.", throwable);
                            }
                        });
                    } else {
                        holderForThisEl.add(toAdd);
                    }
                }
            });
        }
    }

    @Override
    public boolean remove(PooledConnection<R, W> toRemove) {
        for (IdleConnectionsHolder<W, R> anElHolder : allElHolders) {
            if (anElHolder.remove(toRemove)) {
                return true;
            }
        }
        return false;
    }

    public interface IdleConnectionsHolderFactory<W, R> extends Func0<IdleConnectionsHolder<W, R>> {
    }

    private static class FIFOIdleConnectionsHolderFactory<W, R> implements IdleConnectionsHolderFactory<W, R> {

        @Override
        public IdleConnectionsHolder<W, R> call() {
            return new FIFOIdleConnectionsHolder<>();
        }
    }
}
