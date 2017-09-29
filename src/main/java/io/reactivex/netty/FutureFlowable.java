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
package io.reactivex.netty;

import java.util.concurrent.Callable;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.reactivex.Flowable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.subscriptions.EmptySubscription;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Convert Netty Future into void {@link Flowable}.
 *
 * @author Stephane Maldini
 */
public abstract class FutureFlowable extends Flowable<Void> {

	/**
	 * Convert a {@link Future} into {@link Flowable}. {@link Flowable#subscribe(Subscriber)}
	 * will bridge to {@link Future#addListener(GenericFutureListener)}.
	 *
	 * @param future the future to convert from
	 * @param <F> the future type
	 *
	 * @return A {@link Flowable} forwarding {@link Future} success or failure
	 */
	public static <F extends Future<Void>> Flowable<Void> from(F future) {
		if(future.isDone()){
			if(!future.isSuccess()){
				return Flowable.error(future.cause());
			}
			return Flowable.empty();
		}
		return new ImmediateFutureFlowable<>(future);
	}

	/**
	 * Convert a supplied {@link Future} for each subscriber into {@link Flowable}.
	 * {@link Flowable#subscribe(Subscriber)}
	 * will bridge to {@link Future#addListener(GenericFutureListener)}.
	 *
	 * @param deferredFuture the future to evaluate and convert from
	 * @param <F> the future type
	 *
	 * @return A {@link Flowable} forwarding {@link Future} success or failure
	 */
	public static <F extends Future<Void>> Flowable<Void> deferFuture(Callable<F> deferredFuture) {
		return new DeferredFutureFlowable<>(deferredFuture);
	}

	final static class ImmediateFutureFlowable<F extends Future<Void>> extends FutureFlowable {

		final F future;

		ImmediateFutureFlowable(F future) {
			this.future = ObjectHelper.requireNonNull(future, "future");
		}

		@Override
		public final void subscribeActual(final Subscriber<? super Void> s) {
			if(future.isDone()){
				if(future.isSuccess()){
					EmptySubscription.complete(s);
				}
				else{
					EmptySubscription.error(future.cause(), s);
				}
				return;
			}

			FutureSubscription<F> fs = new FutureSubscription<>(future, s);
			s.onSubscribe(fs);
			future.addListener(fs);
		}
	}

	final static class DeferredFutureFlowable<F extends Future<Void>> extends FutureFlowable {

		final Callable<F> deferredFuture;

		DeferredFutureFlowable(Callable<F> deferredFuture) {
			this.deferredFuture =
					ObjectHelper.requireNonNull(deferredFuture, "deferredFuture");
		}

		@Override
		public void subscribeActual(Subscriber<? super Void> s) {
			try {
				F f = deferredFuture.call();

				if (f == null) {
					EmptySubscription.error(new NullPointerException("Deferred supplied null"), s);
					return;
				}

				if (f.isDone()) {
					if (f.isSuccess()) {
						EmptySubscription.complete(s);
					} else {
						EmptySubscription.error(f.cause(), s);
					}
					return;
				}

				FutureSubscription<F> fs = new FutureSubscription<>(f, s);
				s.onSubscribe(fs);
				f.addListener(fs);
			} catch (Throwable e) {
				Exceptions.throwIfFatal(e);
				EmptySubscription.error(e, s);
			}
		}


	}

	final static class FutureSubscription<F extends Future<Void>> implements
			GenericFutureListener<F>,
			Subscription {

		final Subscriber<? super Void> s;
		final F                        future;

		FutureSubscription(F future, Subscriber<? super Void> s) {
			this.s = s;
			this.future = future;
		}

		@Override
		public void request(long n) {
			//noop
		}

		@Override
		public void cancel() {
			future.removeListener(this);
		}

		@Override
		@SuppressWarnings("unchecked")
		public void operationComplete(F future) throws Exception {
			if (!future.isSuccess()) {
				s.onError(future.cause());
			}
			else {
				s.onComplete();
			}
		}
	}
}