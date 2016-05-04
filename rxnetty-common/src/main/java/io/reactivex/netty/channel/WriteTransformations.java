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

import java.util.LinkedList;
import java.util.List;

/**
 * A holder for all transformations that are applied on a channel. Out of the box, it comes with a {@code String} and
 * {@code byte[]} transformer to {@code ByteBuf}. Additional transformations can be applied using
 * {@link #appendTransformer(AllocatingTransformer)}.
 */
public class WriteTransformations {

    private TransformerChain transformers;

    public boolean transform(Object msg, ByteBufAllocator allocator, List<Object> out) {

        boolean transformed = false;

        if (msg instanceof String) {
            out.add(allocator.buffer().writeBytes(((String) msg).getBytes()));
            transformed = true;
        } else if (msg instanceof byte[]) {
            out.add(allocator.buffer().writeBytes((byte[]) msg));
            transformed = true;
        } else if (null != transformers && transformers.acceptMessage(msg)) {
            out.addAll(transformers.transform(msg, allocator));
            transformed = true;
        }

        return transformed;
    }

    public <T, TT> void appendTransformer(AllocatingTransformer<T, TT> transformer) {
        transformers = new TransformerChain(transformer, transformers);
    }

    public void resetTransformations() {
        transformers = null;
    }

    public boolean acceptMessage(Object msg) {
        return msg instanceof String || msg instanceof byte[] || null != transformers && transformers.acceptMessage(msg);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static class TransformerChain extends AllocatingTransformer {

        private final AllocatingTransformer start;
        private final AllocatingTransformer next;

        public TransformerChain(AllocatingTransformer start, AllocatingTransformer next) {
            this.start = start;
            this.next = next;
        }

        @Override
        public List transform(Object toTransform, ByteBufAllocator allocator) {
            if (null == next) {
                return start.transform(toTransform, allocator);
            }

            List transformed = start.transform(toTransform, allocator);
            if (transformed.size() == 1) {
                return next.transform(transformed.get(0), allocator);
            } else {
                final LinkedList toReturn = new LinkedList();
                for (Object nextItem : transformed) {
                    toReturn.addAll(next.transform(nextItem, allocator));
                }
                return toReturn;
            }
        }

        @Override
        protected boolean acceptMessage(Object msg) {
            return start.acceptMessage(msg);
        }
    }
}
