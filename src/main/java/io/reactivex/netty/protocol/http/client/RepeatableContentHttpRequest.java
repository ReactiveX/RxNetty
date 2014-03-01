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

import io.reactivex.netty.serialization.ContentTransformer;

import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A request that uses a {@link ContentSourceFactory} where its {@link ContentSourceFactory#newContentSource()}
 * method always iterates from the start of the {@link ContentSource} of the wrapped {@link HttpRequest} 
 * 
 * @author awang
 *
 * @param <T>
 */
public class RepeatableContentHttpRequest<T> extends HttpRequest<T> {
    static class RepeatableContentFactory<T> implements ContentSourceFactory<T, ReplayingContentSource<T>> {

        private ReplayingContentSource<T> replayingSource;
            
        public RepeatableContentFactory(ContentSource<T> original) {
            if (original instanceof RawContentSource) {
                replayingSource = new ReplayingRawContentSource<T>((RawContentSource) original);
            } else {
                replayingSource = new ReplayingContentSource<T>(original);
            }
        }

        @Override
        public ReplayingContentSource<T> newContentSource() {
            replayingSource.rewind();
            return replayingSource;
        }
    }

    static class ReplayingContentSource<T> implements ContentSource<T> {
        private ContentSource<T> original;
        private LinkedBlockingQueue<T> recorded = new LinkedBlockingQueue<T>();
        private Iterator<T> iteratorOfRecorded;
        
        ReplayingContentSource() {
        }
        
        public ReplayingContentSource(ContentSource<T> original) {
            this.original = original;
            iteratorOfRecorded = recorded.iterator();
        }
        
        @Override
        public boolean hasNext() {
            return iteratorOfRecorded.hasNext() || original.hasNext();
        }

        @Override
        public T next() {
            if (iteratorOfRecorded.hasNext()) {
                return iteratorOfRecorded.next();
            } else {
                T next = original.next();
                if (next != null) {
                    recorded.add(next);
                }
                return next;
            }
        }
            
        public void rewind() {
            iteratorOfRecorded = recorded.iterator();
        }
    }

    static class ReplayingRawContentSource<T> extends ReplayingContentSource<T> implements RawContentSource<T> {
        
        private final ContentTransformer<T> transformer;
        
        public ReplayingRawContentSource(RawContentSource<T> original) {
            super(original);
            this.transformer = original.getTransformer();
        }
        
        @Override
        public ContentTransformer<T> getTransformer() {
            return transformer;
        }
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public RepeatableContentHttpRequest(HttpRequest<T> request) {
        super(request.getNettyRequest());
        if (!request.userPassedInFactory) {
            if (request.hasRawContentSource()) {
                this.rawContentFactory = new RepeatableContentFactory(request.getRawContentSource());
            } else if (request.contentFactory != null) {
                this.contentFactory = new RepeatableContentFactory(request.getContentSource());
            }
        } else {
            this.rawContentFactory = request.rawContentFactory;
            this.contentFactory = request.contentFactory;
        }
    }
}

