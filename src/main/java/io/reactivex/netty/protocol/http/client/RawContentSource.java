package io.reactivex.netty.protocol.http.client;

import io.reactivex.netty.serialization.ContentTransformer;

/**
 * @author Nitesh Kant
 */
public interface RawContentSource<T> extends ContentSource<T> {

    ContentTransformer<T> getTransformer();

    class SingletonRawSource<T> extends SingletonSource<T> implements RawContentSource<T> {

        private final ContentTransformer<T> transformer;

        public SingletonRawSource(T element, ContentTransformer<T> transformer) {
            super(element);
            this.transformer = transformer;
        }

        @Override
        public ContentTransformer<T> getTransformer() {
            return transformer;
        }
    }
}
