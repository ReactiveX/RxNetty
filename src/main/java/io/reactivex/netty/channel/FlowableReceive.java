/*
 * Copyright (c) 2011-2017 Pivotal Software Inc, All Rights Reserved.
 * Modifications Copyright (c) 2017 RxNetty Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.reactivex.netty.channel;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import io.netty.util.ReferenceCountUtil;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.internal.queue.SpscLinkedArrayQueue;
import io.reactivex.internal.subscriptions.EmptySubscription;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.BackpressureHelper;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * @author Stephane Maldini
 */
final class FlowableReceive extends Flowable<Object> implements Subscription, Disposable {

	final Channel           channel;
	final ChannelOperations<?, ?> parent;
	final EventLoop         eventLoop;

	Subscriber<? super Object>     receiver;
	boolean                        receiverFastpath;
	long                           receiverDemand;
	SpscLinkedArrayQueue<Object>   receiverQueue;

	volatile boolean   inboundDone;
	Throwable inboundError;

	volatile Disposable receiverCancel;
	volatile int wip;

	final static AtomicIntegerFieldUpdater<FlowableReceive> WIP = AtomicIntegerFieldUpdater.newUpdater
			(FlowableReceive.class, "wip");

	FlowableReceive(ChannelOperations<?, ?> parent) {
		this.parent = parent;
		this.channel = parent.channel;
		this.eventLoop = channel.eventLoop();
		CANCEL.lazySet(this, new Disposable() {
			@Override
			public void dispose() {
				if (eventLoop.inEventLoop()) {
					unsubscribeReceiver();
				}
				else {
					eventLoop.execute(FlowableReceive.this::unsubscribeReceiver);
				}
			}

			@Override
			public boolean isDisposed() {
				return false;
			}
		});
	}

	@Override
	public void cancel() {
		cancelReceiver();
		drainReceiver();
	}

	final long getPending() {
		return receiverQueue != null ? receiverQueue.size() : 0;
	}

	final boolean isCancelled() {
		return receiverCancel == CANCELLED;
	}

	@Override
	public void dispose() {
		cancel();
	}

	@Override
	public boolean isDisposed() {
		return (inboundDone && (receiverQueue == null || receiverQueue.isEmpty()));
	}

	@Override
	public void request(long n) {
		if (SubscriptionHelper.validate(n)) {
			if (eventLoop.inEventLoop()) {
				this.receiverDemand = BackpressureHelper.addCap(receiverDemand, n);
				drainReceiver();
			}
			else {
				eventLoop.execute(() -> {
					this.receiverDemand = BackpressureHelper.addCap(receiverDemand, n);
					drainReceiver();
				});
			}
		}
	}

	@Override
	protected void subscribeActual(Subscriber<? super Object> s) {
		if (eventLoop.inEventLoop()){
			startReceiver(s);
		}
		else {
			eventLoop.execute(() -> startReceiver(s));
		}
	}

	final boolean cancelReceiver() {
		Disposable c = receiverCancel;
		if (c != CANCELLED) {
			c = CANCEL.getAndSet(this, CANCELLED);
			if (c != CANCELLED) {
				c.dispose();
				return true;
			}
		}
		return false;
	}

	final void cleanQueue(SpscLinkedArrayQueue<Object> q){
		if (q != null) {
			Object o;
			while ((o = q.poll()) != null) {
				ReferenceCountUtil.release(o);
			}
		}
	}

	final boolean drainReceiver() {
		if(WIP.getAndIncrement(this) != 0){
			return false;
		}
		int missed = 1;
		for(;;) {
			final SpscLinkedArrayQueue<Object> q = receiverQueue;
			final Subscriber<? super Object> a = receiver;
			boolean d = inboundDone;

			if (a == null) {
				if (isCancelled()) {
					cleanQueue(q);
					return false;
				}
				if (d && getPending() == 0) {
					Throwable ex = inboundError;
					if (ex != null) {
						parent.context.fireContextError(ex);
					}
					else if(parent.shouldEmitEmptyContext()){
						parent.context.fireContextActive(null);
					}
					else {
						parent.context.fireContextActive(parent);
					}
					return false;
				}
				missed = WIP.addAndGet(this, -missed);
				if(missed == 0){
					break;
				}
				continue;
			}

			long r = receiverDemand;
			long e = 0L;

			while (e != r) {
				if (isCancelled()) {
					cleanQueue(q);
					return false;
				}

				d = inboundDone;
				Object v = q != null ? q.poll() : null;
				boolean empty = v == null;

				if (d && empty) {
					terminateReceiver(q, a);
					return false;
				}

				if (empty) {
					break;
				}

				try {
					a.onNext(v);
				}
				finally {
					ReferenceCountUtil.release(v);
				}

				e++;
			}

			if (isCancelled()) {
				cleanQueue(q);
				return false;
			}

			if (inboundDone && (q == null || q.isEmpty())) {
				terminateReceiver(q, a);
				return false;
			}

			if (r == Long.MAX_VALUE) {
				channel.config()
						.setAutoRead(true);
				channel.read();
				missed = WIP.addAndGet(this, -missed);
				if(missed == 0){
					break;
				}
				return true;
			}

			if ((receiverDemand -= e) > 0L || e > 0L) {
				channel.read();
			}

			missed = WIP.addAndGet(this, -missed);
			if(missed == 0){
				break;
			}
		}
		return false;
	}

	final void startReceiver(Subscriber<? super Object> s) {
		if (receiver == null) {
			if (inboundDone && getPending() == 0) {
				if (inboundError != null) {
					EmptySubscription.error(inboundError, s);
					return;
				}

				EmptySubscription.complete(s);
				return;
			}

			receiver = s;

			s.onSubscribe(this);
		}
		else {
			EmptySubscription.error(
					new IllegalStateException(
							"Only one connection receive subscriber allowed."), s);
		}
	}

	final void onInboundNext(Object msg) {
		if (inboundDone || isCancelled()) {
			ReferenceCountUtil.release(msg);
			return;
		}

		if (receiverFastpath && receiver != null) {
			try {
				receiver.onNext(msg);
			}
			finally {
				ReferenceCountUtil.release(msg);
			}
		}
		else {
			SpscLinkedArrayQueue<Object> q = receiverQueue;
			if (q == null) {
				q = new SpscLinkedArrayQueue<>(Flowable.bufferSize());
				receiverQueue = q;
			}
			q.offer(msg);
			if (drainReceiver()) {
				receiverFastpath = true;
			}
		}
	}

	final boolean onInboundComplete() {
		if (inboundDone) {
			return false;
		}
		inboundDone = true;
		Subscriber<?> receiver = this.receiver;
		if (receiverFastpath && receiver != null) {
			receiver.onComplete();
			return true;
		}
		drainReceiver();
		return false;
	}

	final boolean onInboundError(Throwable err) {
		if (isCancelled() || inboundDone) {
			return false;
		}
		Subscriber<?> receiver = this.receiver;
		this.inboundError = err;
		this.inboundDone = true;

		if(channel.isActive()){
			channel.close();
		}
		if (receiverFastpath && receiver != null) {
			parent.context.fireContextError(err);
			receiver.onError(err);
			return true;
		}
		else {
			drainReceiver();
		}
		return false;
	}

	final void terminateReceiver(SpscLinkedArrayQueue<?> q, Subscriber<?> a) {
		if (q != null) {
			q.clear();
		}
		Throwable ex = inboundError;
		if (ex != null) {
			parent.context.fireContextError(ex);
			a.onError(ex);
		}
		else {
			a.onComplete();
		}
	}

	final void unsubscribeReceiver() {
		receiverDemand = 0L;
		receiver = null;
		if(isCancelled()) {
			parent.onInboundCancel();
		}
	}

	@SuppressWarnings("rawtypes")
	static final AtomicReferenceFieldUpdater<FlowableReceive, Disposable> CANCEL =
			AtomicReferenceFieldUpdater.newUpdater(FlowableReceive.class,
					Disposable.class,
					"receiverCancel");

	static final Disposable CANCELLED = Disposables.disposed();
}
