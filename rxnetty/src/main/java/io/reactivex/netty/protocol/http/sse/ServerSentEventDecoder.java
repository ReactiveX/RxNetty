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
package io.reactivex.netty.protocol.http.sse;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufProcessor;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.List;

/**
 * A decoder to decode <a href="http://www.w3.org/TR/eventsource/">Server sent events</a> into {@link ServerSentEvent}
 */
public class ServerSentEventDecoder extends ByteToMessageDecoder {

    private static final Logger logger = LoggerFactory.getLogger(ServerSentEventDecoder.class);

    public static final int DEFAULT_MAX_FIELD_LENGTH = 100;

    private static final char[] EVENT_ID_FIELD_NAME = "event".toCharArray();
    private static final char[] DATA_FIELD_NAME = "data".toCharArray();
    private static final char[] ID_FIELD_NAME = "id".toCharArray();

    protected static final ByteBufProcessor SKIP_TILL_LINE_DELIMITER_PROCESSOR = new ByteBufProcessor() {
        @Override
        public boolean process(byte value) throws Exception {
            return !isLineDelimiter((char) value);
        }
    };

    protected static final ByteBufProcessor SKIP_LINE_DELIMITERS_AND_SPACES_PROCESSOR = new ByteBufProcessor() {
        @Override
        public boolean process(byte value) throws Exception {
            return isLineDelimiter((char) value) || (char) value == ' ';
        }
    };

    protected static final ByteBufProcessor SKIP_COLON_AND_WHITE_SPACE_PROCESSOR = new ByteBufProcessor() {
        @Override
        public boolean process(byte value) throws Exception {
            char valueChar = (char) value;
            return valueChar == ':' || valueChar == ' ';
        }
    };

    protected static final ByteBufProcessor SCAN_COLON_PROCESSOR = new ByteBufProcessor() {
        @Override
        public boolean process(byte value) throws Exception {
            return (char) value != ':';
        }
    };

    protected static final ByteBufProcessor SCAN_EOL_PROCESSOR = new ByteBufProcessor() {
        @Override
        public boolean process(byte value) throws Exception {
            return !isLineDelimiter((char) value);
        }
    };

    private static Charset sseEncodingCharset;

    static {
        try {
            sseEncodingCharset = Charset.forName("UTF-8");
        } catch (Exception e) {
            logger.error("UTF-8 charset not available. Since SSE only contains UTF-8 data, we can not read SSE data.");
            sseEncodingCharset = null;
        }
    }

    private enum State {
        SkipColonAndWhiteSpaces,// Skip colon and all whitespaces after reading field name.
        SkipLineDelimitersAndSpaces,// Skip all line delimiters after field value end.
        DiscardTillEOL,// On recieving an illegal/unidentified field, ignore everything till EOL.
        ReadFieldName, // Read till a colon to get the name of the field.
        ReadFieldValue // Read value till the line delimiter.
    }

    /**
     * Release of these buffers happens in the following ways:
     *
     * 1) If this was a data buffer, it is released when ServerSentEvent is released.
     * 2) If this was an eventId buffer, it is released when next Id arrives or when the connection
     *     is closed.
     * 3) If this was an eventType buffer, it is released when next type arrives or when the connection
     *     is closed.
     */
    private ByteBuf lastEventId;
    private ByteBuf lastEventType;
    private ByteBuf incompleteData; // Can be field value of name, according to the current state.

    private ServerSentEvent.Type currentFieldType;

    private State state = State.ReadFieldName;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

        if (null == sseEncodingCharset) {
            throw new IllegalArgumentException("Can not read SSE data as UTF-8 charset is not available.");
        }

        while (in.isReadable()) {

            final int readerIndexAtStart = in.readerIndex();

            switch (state) {
                case SkipColonAndWhiteSpaces:
                    if (skipColonAndWhiteSpaces(in)) {
                        state = State.ReadFieldValue;
                    }
                    break;
                case SkipLineDelimitersAndSpaces:
                    if (skipLineDelimiters(in)) {
                        state = State.ReadFieldName;
                    }
                    break;
                case DiscardTillEOL:
                    if(skipTillEOL(in)) {
                        state = State.SkipLineDelimitersAndSpaces;
                    }
                    break;
                case ReadFieldName:
                    final int indexOfColon = scanAndFindColon(in);

                    if (-1 == indexOfColon) { // No colon found
                        // Accumulate data into the field name buffer.
                        if (null == incompleteData) {
                            incompleteData = ctx.alloc().buffer();
                        }
                        // accumulate into incomplete data buffer to be used when the full data arrives.
                        incompleteData.writeBytes(in);
                    } else {
                        int fieldNameLengthInTheCurrentBuffer = indexOfColon - readerIndexAtStart;

                        ByteBuf fieldNameBuffer;
                        if (null != incompleteData) {
                            // Read the remaining data into the temporary buffer
                            in.readBytes(incompleteData, fieldNameLengthInTheCurrentBuffer);
                            fieldNameBuffer = incompleteData;
                            incompleteData = null;
                        } else {
                            // Consume the data from the input buffer.
                            fieldNameBuffer = ctx.alloc().buffer(fieldNameLengthInTheCurrentBuffer,
                                                                 fieldNameLengthInTheCurrentBuffer);
                            in.readBytes(fieldNameBuffer, fieldNameLengthInTheCurrentBuffer);
                        }

                        state = State.SkipColonAndWhiteSpaces; // We have read the field name, next we should skip colon & WS.
                        try {
                            currentFieldType = readCurrentFieldTypeFromBuffer(fieldNameBuffer);
                        } finally {
                            if (null == currentFieldType) {
                                state = State.DiscardTillEOL; // Ignore this event completely.
                            }
                            fieldNameBuffer.release();
                        }
                    }
                    break;
                case ReadFieldValue:

                    final int endOfLineStartIndex = scanAndFindEndOfLine(in);


                    if (-1 == endOfLineStartIndex) { // End of line not found, accumulate data into a temporary buffer.
                        if (null == incompleteData) {
                            incompleteData = ctx.alloc().buffer(in.readableBytes());
                        }
                        // accumulate into incomplete data buffer to be used when the full data arrives.
                        incompleteData.writeBytes(in);
                    } else { // Read the data till end of line into the value buffer.
                        final int bytesAvailableInThisIteration = endOfLineStartIndex - readerIndexAtStart;
                        if (null == incompleteData) {
                            incompleteData = ctx.alloc().buffer(bytesAvailableInThisIteration,
                                                                bytesAvailableInThisIteration);
                        }
                        incompleteData.writeBytes(in, bytesAvailableInThisIteration);

                        switch (currentFieldType) {
                            case Data:
                                if (incompleteData.isReadable()) {
                                    out.add(ServerSentEvent.withEventIdAndType(lastEventId, lastEventType,
                                                                               incompleteData));
                                } else {
                                    incompleteData.release();
                                }
                                break;
                            case Id:
                                if (incompleteData.isReadable()) {
                                    lastEventId = incompleteData;
                                } else {
                                    incompleteData.release();
                                    lastEventId = null;
                                }
                                break;
                            case EventType:
                                if (incompleteData.isReadable()) {
                                    lastEventType = incompleteData;
                                } else {
                                    incompleteData.release();
                                    lastEventType = null;
                                }
                                break;
                        }
                        /**
                         * Since all data is read, reset the incomplete data to null. Release of this buffer happens in
                         * the following ways
                         * 1) If this was a data buffer, it is released when ServerSentEvent is released.
                         * 2) If this was an eventId buffer, it is released when next Id arrives or when the connection
                         *     is closed.
                         * 3) If this was an eventType buffer, it is released when next type arrives or when the connection
                         *     is closed.
                         */
                        incompleteData = null;
                        state = State.SkipLineDelimitersAndSpaces; // Skip line delimiters after reading a field value completely.
                    }
                    break;
            }
        }

    }

    private static ServerSentEvent.Type readCurrentFieldTypeFromBuffer(final ByteBuf fieldNameBuffer) {
        /**
         * This code tries to eliminate the need of creating a string from the ByteBuf as the field names are very
         * constrained. The algorithm is as follows:
         *
         * -- Scan the bytes in the buffer.
         * -- If the first byte matches the expected field names then use the matching field name char array to verify
         * the rest of the field name.
         * -- If the first byte does not match, reject the field name.
         * -- After the first byte, exact match the rest of the field name with the expected field name, byte by byte.
         * -- If the name does not exactly match the expected value, then reject the field name.
         */
        ServerSentEvent.Type toReturn = ServerSentEvent.Type.Data;
        int readableBytes = fieldNameBuffer.readableBytes();
        final int readerIndexAtStart = fieldNameBuffer.readerIndex();
        char[] fieldNameToVerify = DATA_FIELD_NAME;
        boolean verified = false;
        int actualFieldNameIndexToCheck = 0; // Starts with 1 as the first char is validated by equality.
        for (int i = readerIndexAtStart; i < readableBytes; i++) {
            final char charAtI = (char) fieldNameBuffer.getByte(i);

            if (i == readerIndexAtStart) {
                switch (charAtI) { // See which among the known field names this buffer belongs.
                    case 'e':
                        fieldNameToVerify = EVENT_ID_FIELD_NAME;
                        toReturn = ServerSentEvent.Type.EventType;
                        break;
                    case 'd':
                        fieldNameToVerify = DATA_FIELD_NAME;
                        toReturn = ServerSentEvent.Type.Data;
                        break;
                    case 'i':
                        fieldNameToVerify = ID_FIELD_NAME;
                        toReturn = ServerSentEvent.Type.Id;
                        break;
                    default:
                        return null;
                }
            } else {
                if (++actualFieldNameIndexToCheck >= fieldNameToVerify.length || charAtI != fieldNameToVerify[actualFieldNameIndexToCheck]) {
                    // If the character does not match or the buffer is bigger than the expected name, then discard.
                    verified = false;
                    break;
                } else {
                    // Verified till now. If all characters are matching then this stays as verified, else changed to false.
                    verified = true;
                }
            }
        }

        if (verified) {
            return toReturn;
        } else {
            return null;
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        if (null != lastEventId) {
            lastEventId.release();
        }
        if (null != lastEventType) {
            lastEventType.release();
        }
        if (null != incompleteData) {
            incompleteData.release();
        }
    }

    protected static int scanAndFindColon(ByteBuf byteBuf) {
        return byteBuf.forEachByte(SCAN_COLON_PROCESSOR);
    }

    protected static int scanAndFindEndOfLine(ByteBuf byteBuf) {
        return byteBuf.forEachByte(SCAN_EOL_PROCESSOR);
    }

    protected static boolean skipLineDelimiters(ByteBuf byteBuf) {
        return skipTillMatching(byteBuf, SKIP_LINE_DELIMITERS_AND_SPACES_PROCESSOR);
    }

    protected static boolean skipColonAndWhiteSpaces(ByteBuf byteBuf) {
        return skipTillMatching(byteBuf, SKIP_COLON_AND_WHITE_SPACE_PROCESSOR);
    }

    private static boolean skipTillEOL(ByteBuf in) {
        return skipTillMatching(in, SKIP_TILL_LINE_DELIMITER_PROCESSOR);
    }

    protected static boolean skipTillMatching(ByteBuf byteBuf, ByteBufProcessor processor) {
        final int lastIndexProcessed = byteBuf.forEachByte(processor);
        if (-1 == lastIndexProcessed) {
            byteBuf.readerIndex(byteBuf.readerIndex() + byteBuf.readableBytes()); // If all the remaining bytes are to be ignored, discard the buffer.
        } else {
            byteBuf.readerIndex(lastIndexProcessed);
        }

        return -1 != lastIndexProcessed;
    }

    protected static boolean isLineDelimiter(char c) {
        return c == '\r' || c == '\n';
    }
}