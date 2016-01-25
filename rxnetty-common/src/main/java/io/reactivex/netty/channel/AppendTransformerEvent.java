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

/**
 * An event to register a custom transformer of data written on a channel.
 *
 * @param <T> Source type for the transformer.
 * @param <TT> Target type for the transformer.
 */
public final class AppendTransformerEvent<T, TT> {

    private final AllocatingTransformer<T, TT> transformer;

    public AppendTransformerEvent(AllocatingTransformer<T, TT> transformer) {
        if (null == transformer) {
            throw new NullPointerException("Transformer can not be null.");
        }
        this.transformer = transformer;
    }

    public AllocatingTransformer<T, TT> getTransformer() {
        return transformer;
    }
}
