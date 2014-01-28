package io.reactivex.netty.protocol.text;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.util.List;

/**
 * A decoder that breaks an incoming {@link ByteBuf} into a list of strings delimited by a new line as specified by
 * {@link #isLineDelimiter(char)}
 */
class StringLineDecoder extends ReplayingDecoder<StringLineDecoder.State> {

    public enum State {
        NEW_LINE,
        END_OF_LINE
    }

    public StringLineDecoder() {
        super(State.NEW_LINE);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        switch (state()) {
        case NEW_LINE:
            String line = readFullLine(in);

            // Immediately checkpoint the progress so that replaying decoder
            // will not start over from the beginning of the line when buffer overflows
            checkpoint();

            out.add(line);
            break;
        case END_OF_LINE:
            skipLineDelimiters(in);
            break;
        }
    }

    private String readFullLine(ByteBuf in) {
        StringBuilder line = new StringBuilder();

        for (;;) {
            char c = (char) in.readByte();
            if (isLineDelimiter(c)) {
                checkpoint(State.END_OF_LINE);
                return line.toString();
            }

            line.append(c);
        }
    }

    private void skipLineDelimiters(ByteBuf in) {
        for (;;) {
            char c = (char) in.readByte();
            if (isLineDelimiter(c)) {
                continue;
            }

            // Leave the reader index at the first letter of the next line, if any
            in.readerIndex(in.readerIndex() - 1);
            checkpoint(State.NEW_LINE);
            break;
        }
    }

    public static boolean isLineDelimiter(char c) {
        return c == '\r' || c == '\n';
    }
}
