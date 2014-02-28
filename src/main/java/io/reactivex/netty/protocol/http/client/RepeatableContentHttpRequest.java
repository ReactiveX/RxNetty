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
 * A request that uses a {@link ContentFactory} where its {@link ContentFactory#newContentSource()}
 * method always iterates from the start of the {@link ContentSource} of the wrapped {@link HttpRequest} 
 * 
 * @author awang
 *
 * @param <T>
 */
public class RepeatableContentHttpRequest<T> extends HttpRequest<T> {
    public RepeatableContentHttpRequest(HttpRequest<T> request) {
        super(request.getNettyRequest());
        if (request.contentSource != null) {
            this.contentFactory = new RepeatableContentFactory<T>(request.contentSource);
        } else if (request.contentFactory != null) {
            this.contentFactory = request.contentFactory;
        }
    }
}

class RepeatableContentFactory<T> implements ContentFactory<T, ReplayingContentSource<T>> {

    private ReplayingContentSource<T> recordingSource;
        
    public RepeatableContentFactory(ContentSource<T> original) {
        if (original instanceof RawContentSource) {
            recordingSource = new RecordingRawContentSource<T>((RawContentSource) original);
        } else {
            recordingSource = new ReplayingContentSource<T>(original);
        }
    }

    @Override
    public ReplayingContentSource<T> newContentSource() {
        recordingSource.rewind();
        return recordingSource;
    }
}

class ReplayingContentSource<T> implements ContentSource<T> {
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
        if (iteratorOfRecorded.hasNext()) {
            return iteratorOfRecorded.hasNext();
        } else {
            return original.hasNext();
        }
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

class RecordingRawContentSource<T> extends ReplayingContentSource<T> implements RawContentSource<T> {
    
    private RawContentSource<T> original;
    
    public RecordingRawContentSource(RawContentSource<T> original) {
        super(original);
        this.original = original;
    }
    
    @Override
    public ContentTransformer<T> getTransformer() {
        return original.getTransformer();
    }
}

