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

package io.reactivex.netty.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandler;
import io.netty.util.ByteProcessor;

import java.nio.charset.Charset;
import java.util.List;

/**
 * A utility class to be used with a channel handler to parse {@code ByteBuffer}s that contain strings terminated with
 * a new line. This reader supports non-blocking incremental reads split across random places in the line. <p>
 *
 * This is <em>not</em> thread-safe and must only be called from a {@link ChannelHandler}.
 */
public class LineReader {

    public static final int DEFAULT_INITIAL_CAPACITY = 256;

    private static final ByteProcessor LINE_END_FINDER = new ByteProcessor() {
        public static final char LF = 10;

        @Override
        public boolean process(byte value) throws Exception {
            char nextByte = (char) value;
            return LF != nextByte;
        }
    };
    private ByteBuf incompleteBuffer;
    private final int maxLineLength;
    private final Charset encoding;

    public LineReader() {
        this(Integer.MAX_VALUE, Charset.defaultCharset());
    }

    public LineReader(int maxLineLength, Charset encoding) {
        this.maxLineLength = maxLineLength;
        this.encoding = encoding;
    }

    /**
     * Reads the {@code in} buffer as much as it can and adds all the read lines into the {@code out} list. <p>
     * If there is any outstanding data that is not read, it is stored into a temporary buffer and the next decode will
     * prepend this data to the newly read data. <p>
     *
     * {@link #dispose()} must be called when the associated {@link ChannelHandler} is removed from the pipeline.
     *
     * @param in Buffer to decode.
     * @param out List to add the read lines to.
     * @param allocator Allocator to allocate new buffers, if required.
     */
    public void decode(ByteBuf in, List<Object> out, ByteBufAllocator allocator) {
        while (in.isReadable()) {
            final int startIndex = in.readerIndex();

            int lastReadIndex = in.forEachByte(LINE_END_FINDER);

            if (-1 == lastReadIndex) {
                // Buffer end without line termination
                if (null == incompleteBuffer) {
                    incompleteBuffer = allocator.buffer(DEFAULT_INITIAL_CAPACITY, maxLineLength);
                }

                /*Add to the incomplete buffer*/
                incompleteBuffer.ensureWritable(in.readableBytes());
                incompleteBuffer.writeBytes(in);
            } else {
                ByteBuf lineBuf = in.readSlice(lastReadIndex - startIndex);
                String line;
                if (null != incompleteBuffer) {
                    line = incompleteBuffer.toString(encoding) + lineBuf.toString(encoding);
                    incompleteBuffer.release();
                    incompleteBuffer = null;
                } else {
                    line = lineBuf.toString(encoding);
                }
                out.add(line);
                in.skipBytes(1); // Skip new line character.
            }
        }
    }

    /**
     * Same as {@link #decode(ByteBuf, List, ByteBufAllocator)} but it also produces the left-over buffer, even in
     * absence of a line termination.
     *
     * {@link #dispose()} must be called when the associated {@link ChannelHandler} is removed from the pipeline.
     *
     * @param in Buffer to decode.
     * @param out List to add the read lines to.
     * @param allocator Allocator to allocate new buffers, if required.
     */
    public void decodeLast(ByteBuf in, List<Object> out, ByteBufAllocator allocator) {
        decode(in, out, allocator);
        if (null != incompleteBuffer && incompleteBuffer.isReadable()) {
            out.add(incompleteBuffer.toString(encoding));
        }
    }

    /**
     * Disposes any half-read data buffers.
     */
    public void dispose() {
        if (null != incompleteBuffer) {
            incompleteBuffer.release();
        }
    }

    public static boolean isLineDelimiter(char c) {
        return c == '\r' || c == '\n';
    }

    /*Visible for testing*/ ByteBuf getIncompleteBuffer() {
        return incompleteBuffer;
    }
}
