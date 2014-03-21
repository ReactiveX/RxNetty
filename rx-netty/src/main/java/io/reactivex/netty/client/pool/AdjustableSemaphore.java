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

import java.util.concurrent.Semaphore;

class AdjustableSemaphore {
 
    /**
     * semaphore starts at 0 capacity; must be set by setMaxPermits before use
     */
    private final ResizeableSemaphore semaphore;
 
    /**
     * how many permits are allowed as governed by this semaphore.
     * Access must be synchronized on this object.
     */
    private int maxPermits = 0;
 
    /**
     * New instances should be configured with setMaxPermits().
     */
    public AdjustableSemaphore(int initialMaxPermits) {
        this.maxPermits = initialMaxPermits;
        this.semaphore= new ResizeableSemaphore(initialMaxPermits); 
    }
 
    /**
     * Set the max number of permits. Must be greater than zero.
     *
     * Note that if there are more than the new max number of permits currently
     * outstanding, any currently blocking threads or any new threads that start
     * to block after the call will wait until enough permits have been released to
     * have the number of outstanding permits fall below the new maximum. In
     * other words, it does what you probably think it should.
     */
    synchronized void setMaxPermits(int newMax) {
        if (newMax < 1) {
            throw new IllegalArgumentException("Semaphore size must be at least 1,"
                + " was " + newMax);
        }
 
        int delta = newMax - this.maxPermits;
 
        if (delta == 0) {
            return;
        } else if (delta > 0) {
            // new max is higher, so release that many permits
            this.semaphore.release(delta);
        } else {
            delta *= -1;
            // delta < 0.
            // reducePermits needs a positive #, though.
            this.semaphore.reducePermits(delta);
        }
 
        this.maxPermits = newMax;
    }
 
    void release() {
        this.semaphore.release();
    }
 
    /**
     * Get a permit, blocking if necessary.
     *
     * @throws InterruptedException
     *             if interrupted while waiting for a permit
     */
    void acquire() throws InterruptedException {
        this.semaphore.acquire();
    }
 
    boolean tryAcquire() {
        return this.semaphore.tryAcquire();
    }
    
    int availablePermits() {
        return this.semaphore.availablePermits();
    }
    
    /**
     * A trivial subclass of <code>Semaphore</code> that exposes the reducePermits
     * call to the parent class.
     */
    private static final class ResizeableSemaphore extends Semaphore {
        /**
         *
         */
        private static final long serialVersionUID = 1L;
 
        /**
         * Create a new semaphore with 0 permits.
         */
        ResizeableSemaphore(int max) {
            super(max);
        }
 
        @Override
        protected void reducePermits(int reduction) {
            super.reducePermits(reduction);
        }
    }
}
