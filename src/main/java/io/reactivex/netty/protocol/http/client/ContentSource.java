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
