package io.reactivex.netty.examples.http.ws.messaging;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

/**
 * Messaging frame used by {@link MessagingClient} and {@link MessagingServer}. This frame has two information:
 *
 *<ul>
 <li>{@link MessageFrame.MessageType} for the message: This is the first byte in the frame data. </li>
 <li>Id for the message: This is a long after the first type byte.</li>
 </ul>
 */
public class MessageFrame extends BinaryWebSocketFrame {

    public enum MessageType {
        Message,
        Ack
    }

    private final MessageType type;
    private final long id;

    /**
     * Constructs a new frame, usually used for writing with the passed message type and id.
     *
     * @param type Type of the message.
     * @param id Id of the message.
     */
    public MessageFrame(MessageType type, long id) {
        this(type, id, UnpooledByteBufAllocator.DEFAULT);
    }

    /**
     * Constructs a new frame, usually used for writing with the passed message type and id.
     *
     * @param type Type of the message.
     * @param id Id of the message.
     * @param allocator An allocator for allocating the buffer for this frame.
     */
    public MessageFrame(MessageType type, long id, ByteBufAllocator allocator) {
        super(allocator.buffer().writeByte(type.ordinal()).writeLong(id));
        this.type = type;
        this.id = id;
    }

    /**
     * Creates a new frame from existing data, usually used for reading a frame of the wire.
     *
     * @param binaryData Data representing a message frame.
     *
     * @throws IllegalArgumentException If the message type is unknown.
     * @throws IndexOutOfBoundsException If the frame is malformed and does not contain the data in the format required.
     */
    public MessageFrame(ByteBuf binaryData) {
        super(binaryData);
        binaryData.markReaderIndex();
        int typeRead = binaryData.readByte();
        switch (typeRead) {
        case 0:
            type = MessageType.Message;
            break;
        case 1:
            type = MessageType.Ack;
            break;
        default:
            throw new IllegalArgumentException("Unexpected message type: " + typeRead);
        }

        id = binaryData.readLong();
        binaryData.resetReaderIndex();
    }

    public long getId() {
        return id;
    }

    public MessageType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "MessageFrame{" + "id=" + id + ", type=" + type + '}';
    }
}
