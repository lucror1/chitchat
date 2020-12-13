// Based on io.netty.example.telnet.TelnetClient
// https://github.com/netty/netty/blob/4.1/example/src/main/java/io/netty/example/telnet/TelnetClient.java
package io.github.chitchat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class Client {
    // Basic Java stuff
    public static String username;
    public static String host;
    public static int port;
    public static boolean shouldRun;

    // Most recent message read and it's random id
    public static ArrayList<String> mostRecentMessages;
    public static int mostRecentId;

    // The BufferedReader is static so that the message displaying routine can grab half-typed messages
    // That way, they won't get deleted if the screen clears while typing
    public static BufferedReader in;

    // My classes
    public static ClientHandler clientHandler = new ClientHandler();

    public static void main(String[] args) throws Exception {
        // Assigns the default server location
        Client.host = "localhost";
        Client.port = 8080;
        Client.shouldRun = true;
        // Setting mostRecentId to an impossible value to ensure we get an update response
        // the first time we request one
        Client.mostRecentId = -1;

        // If arguments are provided, change the default location
        if (args.length == 0) {
            System.out.println("Usage: ./runClient <username> [host] [port]");
            System.exit(-1);
        }
        if(args.length == 1) {
            Client.username = args[0];
        }

        if (args.length == 2) {
            Client.username = args[0];
            Client.host = args[1];
        }

        if (args.length == 3) {
            Client.username = args[0];
            Client.host = args[1];
            try {
                Client.port = Integer.parseInt(args[2]);
            }
            catch (NumberFormatException ex) {
                System.out.println("Usage: ./runClient <username> [host] [port]");
                System.exit(-1);
            }
        }
        if (args.length > 3) {
            System.out.println("Usage: ./runClient <username> [host] [port]");
            System.exit(-1);
        }

        // Create the user input thread and network thread
        Thread userMessageThread = new Thread(new UserMessageHandler());
        Thread networkMessageThread = new Thread(new NetworkMessageHandler());

        userMessageThread.start();
        networkMessageThread.start();
    }

    public static void displayMessages() {
        
        clearScreen();
        for (String msg : Client.mostRecentMessages) {
            System.out.println(msg);
        }
    }

    private static void clearScreen() {
        String os = System.getProperty("os.name");
        
        // Windows is annoying and doesn't have a character sequence to clear the scree
        // so I have to create a new process to deal with it
        if(os.toLowerCase().contains("windows")) {
            try{
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            }
            catch(Exception ex) {
                // If clearing the screen fails, print a bunch of newlines to mimic the effect
                System.out.println("\n".repeat(50));
            }
        }
        // Everything else has a nice easy solution though
        else {
            System.out.print("\033[H\033[2J");
        }
    }
}

class UserMessageHandler implements Runnable {
    private PacketEncoder encoder;
    private EventLoopGroup workerGroup;
    private ChannelFuture lastWriteFuture;

    public void run() {
        workerGroup = new NioEventLoopGroup();

        encoder = new PacketEncoder();
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        while(Client.shouldRun) {
            try {
                // Check if the user has entered any new messages
                if(in.ready()) {
                    // Read all the buffered messages
                    while(in.ready()) {
                        // Get the message and encode it
                        String msg = in.readLine();

                        // Exit if exit command is issued
                        if (msg.equals("/exit")) {
                            stop();
                            Client.shouldRun = false;
                        }
                        else if (msg.length() != 0){
                            // Send the message over the network
                            sendMessage(msg);
                            Client.displayMessages();
                            // Mimic the client properly displaying the message
                            System.out.println(String.format("[%s] %s", Client.username, msg));
                        }
                    }
                }
            }
            catch (IOException ex) {
                System.out.println("Error reading from stdin");
            }
        }
    }

    public void sendMessage(String msg) {
        ByteBuf byteMsg = encoder.encodeMessage(Client.username, msg);

        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup);
            b.channel(NioSocketChannel.class);
            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.handler(new ChannelInitializer<SocketChannel>(){
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(Client.clientHandler);
                }
            });

            Channel channel = b.connect(Client.host, Client.port).sync().channel();
            
            lastWriteFuture = channel.writeAndFlush(byteMsg);
        }
        catch (InterruptedException ex) {
            System.out.println("Error connecting to server");
        }
    }

    public void stop() {
        try {
            if (lastWriteFuture != null) {
                lastWriteFuture.sync();
            }
        }
        catch (InterruptedException ex) {
            System.out.println("Error shutting down UserMessageHandler");
        }
        finally {
            workerGroup.shutdownGracefully();
        }
    }
}

class NetworkMessageHandler implements Runnable {
    private PacketEncoder encoder;
    private EventLoopGroup workerGroup;
    private ChannelFuture lastWriteFuture;

    public void run() {
        encoder = new PacketEncoder();
        workerGroup = new NioEventLoopGroup();

        while(Client.shouldRun) {
            Channel channel = connect();
            ByteBuf msg = encoder.encodeUpdateRequest(Client.mostRecentId);
            lastWriteFuture = channel.writeAndFlush(msg);

            try{
                Thread.sleep(1000);
            }
            catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
        stop();
    }

    private Channel connect() {
        Channel channel;
        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup);
            b.channel(NioSocketChannel.class);
            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.handler(new ChannelInitializer<SocketChannel>(){
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(Client.clientHandler);
                }
            });

            channel = b.connect(Client.host, Client.port).sync().channel();
        }
        catch (InterruptedException ex) {
            channel = null;
        }
        return channel;
    }

    public void stop() {
        try {
            if (lastWriteFuture != null) {
                lastWriteFuture.sync();
            }
        }
        catch (InterruptedException ex) {
            System.out.println("Error shutting down NetworkMessageHandler");
        }
        finally {
            workerGroup.shutdownGracefully();
        }
    }
}