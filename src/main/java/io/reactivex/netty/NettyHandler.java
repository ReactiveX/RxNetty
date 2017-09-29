/*
 * Copyright (c) 2017 RxNetty Contributors.
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

import io.reactivex.Flowable;
import io.reactivex.MaybeEmitter;
import io.reactivex.MaybeOnSubscribe;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Cancellable;
import io.reactivex.internal.disposables.CancellableDisposable;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.plugins.RxJavaPlugins;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReference;

public final class NettyHandler extends Flowable<NettyContext> {

  public static NettyHandler create(MaybeOnSubscribe<NettyContext> source) {
    return new NettyHandler(source);
  }

  final MaybeOnSubscribe<NettyContext> source;

  NettyHandler(MaybeOnSubscribe<NettyContext> source) {
    this.source = source;
  }

  @Override
  public void subscribeActual(Subscriber<? super NettyContext> actual) {
    NettyContextEmitter emitter = new NettyContextEmitter(actual);
    actual.onSubscribe(emitter);

    try {
      source.subscribe(emitter);
    }
    catch (Throwable ex) {
      Exceptions.throwIfFatal(ex);
      emitter.onError(ex);
    }
  }

  static final class NettyContextEmitter extends AtomicReference<Disposable>
      implements MaybeEmitter<NettyContext>, Subscription {

    final Subscriber<? super NettyContext> actual;

    volatile int state;
    @SuppressWarnings("rawtypes")
    static final AtomicIntegerFieldUpdater<NettyContextEmitter> STATE =
        AtomicIntegerFieldUpdater.newUpdater(NettyContextEmitter.class, "state");

    NettyContext context;

    static final int NO_REQUEST_HAS_CONTEXT = 1;
    static final int HAS_REQUEST_NO_CONTEXT = 2;
    static final int HAS_REQUEST_HAS_CONTEXT = 3;

    NettyContextEmitter(Subscriber<? super NettyContext> actual) {
      this.actual = actual;
    }

    @Override
    public void onComplete() {
      if (STATE.getAndSet(this, HAS_REQUEST_HAS_CONTEXT) != HAS_REQUEST_HAS_CONTEXT) {
        actual.onComplete();
      }
    }

    @Override
    public void onSuccess(NettyContext context) {
      if (context == null) {
        onComplete();
        return;
      }
      for (; ; ) {
        int s = state;
        if (s == HAS_REQUEST_HAS_CONTEXT || s == NO_REQUEST_HAS_CONTEXT) {
          return;
        }
        if (s == HAS_REQUEST_NO_CONTEXT) {
          if (STATE.compareAndSet(this, s, HAS_REQUEST_HAS_CONTEXT)) {
            actual.onNext(context);
            actual.onComplete();
          }
          return;
        }
        this.context = context;
        if (STATE.compareAndSet(this, s, NO_REQUEST_HAS_CONTEXT)) {
          return;
        }
      }
    }

    @Override
    public void onError(Throwable e) {
      if (!tryOnError(e)) {
        RxJavaPlugins.onError(e);
      }
    }

    @Override
    public boolean tryOnError(Throwable e) {
      if (STATE.getAndSet(this, HAS_REQUEST_HAS_CONTEXT) != HAS_REQUEST_HAS_CONTEXT) {
        actual.onError(e);
        return true;
      }
      return false;
    }

    @Override
    public void setDisposable(Disposable d) {
      DisposableHelper.set(this, d);
    }

    @Override
    public void setCancellable(Cancellable c) {
      setDisposable(new CancellableDisposable(c));
    }

    @Override
    public void request(long n) {
      if (SubscriptionHelper.validate(n)) {
        for (; ; ) {
          int s = state;
          if (s == HAS_REQUEST_NO_CONTEXT || s == HAS_REQUEST_HAS_CONTEXT) {
            return;
          }
          if (s == NO_REQUEST_HAS_CONTEXT) {
            if (STATE.compareAndSet(this, s, HAS_REQUEST_HAS_CONTEXT)) {
              actual.onNext(context);
              actual.onComplete();
            }
            return;
          }
          if (STATE.compareAndSet(this, s, HAS_REQUEST_NO_CONTEXT)) {
            return;
          }
        }
      }
    }

    @Override
    public void cancel() {
      if (STATE.getAndSet(this, HAS_REQUEST_HAS_CONTEXT) != HAS_REQUEST_HAS_CONTEXT) {
        context = null;
        DisposableHelper.dispose(this);
      }
    }

    @Override
    public boolean isDisposed() {
      return DisposableHelper.isDisposed(get());
    }
  }
}
