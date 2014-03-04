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
package io.reactivex.netty.protocol.http;

/**
 * @author Nitesh Kant
 */
public abstract class AbstractHttpConfigurator {

    public static final int MAX_INITIAL_LINE_LENGTH_DEFAULT = 4096;
    public static final int MAX_HEADER_SIZE_DEFAULT = 8192;
    public static final int MAX_CHUNK_SIZE_DEFAULT = 8192;
    public static final boolean VALIDATE_HEADERS_DEFAULT = true;
    protected final int maxInitialLineLength;
    protected final int maxHeaderSize;
    protected final int maxChunkSize;
    protected final boolean validateHeaders;

    protected AbstractHttpConfigurator(int maxInitialLineLength, int maxChunkSize, int maxHeaderSize,
                                       boolean validateHeaders) {
        this.maxInitialLineLength = maxInitialLineLength;
        this.validateHeaders = validateHeaders;
        this.maxChunkSize = maxChunkSize;
        this.maxHeaderSize = maxHeaderSize;
    }
}
