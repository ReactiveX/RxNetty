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

package com.netflix.server.context;

/**
 * Merge contract for {@link BiDirectional} contexts.
 *
 * @author Nitesh Kant
 */
@SuppressWarnings("rawtypes")
public interface MergeableContext<T extends MergeableContext> {

    /**
     * Merges the passed object into this object. <br/>
     * <h2>Thread safety</h2>
     * This method is always called in a synchronized context so the implementation need not bother about thread safety.
     * In other words, it is assured that for the same context instance, at no time two threads will be calling merge.
     *
     * @param toMerge Context to merge.
     */
    void merge(T toMerge);
}
