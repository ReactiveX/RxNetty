/*
 * Copyright 2016 Netflix, Inc.
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

import io.netty.buffer.ByteBufAllocator;
import io.netty.util.internal.TypeParameterMatcher;
import rx.annotations.Beta;

import java.util.List;

/**
 * A transformer to be used for modifying the type of objects written on a {@link Connection}.
 *
 * <h2>Why is this required?</h2>
 *
 * The type of an object can usually be transformed using {@code Observable.map()}, however, while writing on a
 * {@link Connection}, typically one requires to allocate buffers. Although a {@code Connection} provides a way to
 * retrieve the {@link ByteBufAllocator} via the {@code Channel}, allocating buffers from outside the eventloop will
 * lead to buffer bloats as the allocators will typically use thread-local buffer pools. <p>
 *
 * This transformer is always invoked from within the eventloop and hence does not have buffer bloating issues, even
 * when transformations happen outside the eventloop.
 *
 * @param <T> Source type.
 * @param <TT> Target type.
 */
@Beta
public abstract class AllocatingTransformer<T, TT> {

    private final TypeParameterMatcher matcher;

    protected AllocatingTransformer() {
        matcher = TypeParameterMatcher.find(this, AllocatingTransformer.class, "T");
    }

    /**
     * Asserts whether the passed message can be transformed using this transformer.
     *
     * @param msg Message to transform.
     *
     * @return {@code true} if the message can be transformed.
     */
    protected boolean acceptMessage(Object msg) {
        return matcher.match(msg);
    }

    /**
     * Transforms the passed message and adds the output to the returned list.
     *
     * @param toTransform Message to transform.
     * @param allocator Allocating for allocating buffers, if required.
     *
     * @return Output of the transformation.
     */
    public abstract List<TT> transform(T toTransform, ByteBufAllocator allocator);

}
