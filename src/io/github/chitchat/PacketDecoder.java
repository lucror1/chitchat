package io.github.chitchat;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;

import io.netty.buffer.ByteBuf;

public class PacketDecoder {
    public Dictionary<String, String> decodeMessage(ByteBuf msg) throws IllegalArgumentException{
        Dictionary<String, String> msgInfo = new Hashtable<>();

        // Verify that this is a valid message packet
        if (msg.readByte() != 0) {
            throw new IllegalArgumentException();
        }

        // Get the length of the username and read it
        int userByteCount = msg.readInt();
        String username = msg.readCharSequence(userByteCount, Charset.defaultCharset()).toString();
        msgInfo.put("username", username);

        // Get the length of the message and read it
        int msgByteCount = msg.readInt();
        String message = msg.readCharSequence(msgByteCount, Charset.defaultCharset()).toString();
        msgInfo.put("msg", message);

        return msgInfo;
    }

    public long decodeUpdateRequest(ByteBuf msg) throws IllegalArgumentException {
        if(msg.readByte() != 1) {
            throw new IllegalArgumentException();
        }

        return msg.readLong();
    }

    public ArrayList<ByteBuf> decodeUpdateResponse(ByteBuf msg) throws IllegalArgumentException{
        if(msg.readByte() != 2) {
            throw new IllegalArgumentException();
        }

        Client.mostRecentId = msg.readInt();

        ArrayList<ByteBuf> msgs = new ArrayList<>();
        long msgSize;

        while(msg.isReadable()) {
            // Get each msg size and add it to the ArrayList
            msgSize = msg.readLong();
            msgs.add(msg.readBytes((int) msgSize));
        }

        return msgs;
    }
}
