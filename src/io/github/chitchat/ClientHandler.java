package io.github.chitchat;

import java.util.ArrayList;
import java.util.Dictionary;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelHandler.Sharable;

@Sharable
public class ClientHandler extends ChannelInboundHandlerAdapter {
    private PacketDecoder decoder = new PacketDecoder();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception{
        // Decode the response as a list of ByteBufs
        ArrayList<ByteBuf> responses = decoder.decodeUpdateResponse((ByteBuf) msg);
        ArrayList<String> processedResponses = new ArrayList<>();

        // Converted each of the messages into a formatted string and store it in a list
        for (ByteBuf buf : responses) {
            Dictionary<String, String> decoded = decoder.decodeMessage(buf);
            processedResponses.add(String.format("[%s] %s", decoded.get("username"), decoded.get("msg")));
        }

        // Writing the entire list at once to decrease the chance that the client
        // tries to read the mostRecentMessages list mid-write
        Client.mostRecentMessages = processedResponses;
        
        // Tell the client to display the messages
        Client.displayMessages();
        
        super.channelRead(ctx, msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        super.exceptionCaught(ctx, cause);
    }
}
