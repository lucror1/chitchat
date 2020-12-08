package io.github.chitchat;

import java.util.ArrayList;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ServerHandler extends ChannelInboundHandlerAdapter {
    private static final int maxMessageBufferSize = 50;
    private static ArrayList<Byte[]> messageList = new ArrayList<>(maxMessageBufferSize);
    private static int id;

    private PacketEncoder encoder = new PacketEncoder();

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        switch(getPacketId(buf)) {
            case 0:
                handleMessagePacket(buf);
                break;
            case 1:
                ctx.writeAndFlush(handleUpdatePacket(buf));
                break;
        }
    }

    private int getPacketId(ByteBuf buf) {
        return buf.getByte(0);
    }

    private void handleMessagePacket (ByteBuf buf) {
        // Generate new random packet id so the new message log will be sent to people
        id = (int)(Math.random() * Integer.MAX_VALUE);

        // Convert buf to byte[]
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);

        // Convert byte[] to Byte[] for storage in message buffer
        Byte[] convertedBytes = new Byte[bytes.length];
        for(int i = 0; i < bytes.length; i++) {
            convertedBytes[i] = bytes[i];
        }
        
        // Add the array to the message buffer
        messageList.add(convertedBytes);

        // If the message list is too big, removing old messages
        while(messageList.size() > maxMessageBufferSize) {
            messageList.remove(0);
        }
    }

    private ByteBuf handleUpdatePacket(ByteBuf msg) {
        int msgId = msg.getInt(1);
        
        // Check if the client has outdated messages
        if (msgId != id) {
            // If it does, package an update
            ArrayList<ByteBuf> convertedMessages = new ArrayList<>();

            // Converts the list of Byte[]s to a list of ByteBufs
            for(Byte[] b : messageList) {
                // Converts the messageList of Byte[] to byte[]
                byte[] temp = new byte[b.length];
                for(int i = 0; i < b.length; i++) {
                    temp[i] = Byte.valueOf(b[i]);
                }

                convertedMessages.add(Unpooled.wrappedBuffer(temp));
            }

            // Encode the message and return it
            return encoder.encodeUpdateResponse(convertedMessages, id);
        }
        // If the client doesn't need an update, send nothing
        return Unpooled.EMPTY_BUFFER;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        super.exceptionCaught(ctx, cause);
    }
}
