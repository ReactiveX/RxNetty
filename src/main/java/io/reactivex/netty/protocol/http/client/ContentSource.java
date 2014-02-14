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
package io.reactivex.netty.protocol.http.client;

import java.util.NoSuchElementException;

/**
 * @author Nitesh Kant
 */
public interface ContentSource<T> {

    boolean hasNext();

    T next();


    class EmptySource<T> implements ContentSource<T> {

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public T next() {
            throw new NoSuchElementException("Empty source does not have any content.");
        }
    }

    class SingletonSource<T> implements ContentSource<T> {

        private T element;

        public SingletonSource(T element) {
            this.element = element;
        }

        @Override
        public boolean hasNext() {
            return null != element;
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            T toReturn = element;
            element = null;
            return toReturn;
        }
    }
}
