package io.github.chitchat;

import java.nio.charset.Charset;
import java.util.ArrayList;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class PacketEncoder {

    public ByteBuf encodeMessage(String username, String msg) {
        // Get size of the username and message
        int userByteCount = username.length();  // ints are 4 bytes
        int msgBytecount = msg.length();

        // Allocate enough space in the buffer
        // For some reason it seems to automatically allocate at least 64 bytes
        // unless the username and message are only a few characters
        ByteBuf message = Unpooled.buffer(9 + userByteCount + msgBytecount);

        // Write packet type as a single byte
        message.writeByte(0);

        // Write the username and it's size
        message.writeInt(userByteCount);
        message.writeCharSequence(username, Charset.defaultCharset());

        // Write the message and it's size
        message.writeInt(msgBytecount);
        message.writeCharSequence(msg, Charset.defaultCharset());

        return message;
    }

    public ByteBuf encodeUpdateRequest(int id) {
        ByteBuf request = Unpooled.buffer(5);

        // Write the packet type
        request.writeByte(1);
        request.writeInt(id);

        return request;
    }

    public ByteBuf encodeUpdateResponse(ArrayList<ByteBuf> msgs, int id) {
        // 256 is just a generic multiplier to estimate the size of each msg
        ByteBuf response = Unpooled.buffer(256 * msgs.size());

        // Write packet type
        response.writeByte(2);

        // Write the new id
        response.writeInt(id);

        // Write each message's size and the message itself
        // We need a long because each message can be very long
        for(ByteBuf msg:msgs) {
            response.writeLong(msg.readableBytes());
            response.writeBytes(msg);
        }

        return response;
    }
}
