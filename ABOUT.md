# About chitchat
## Client

The client split into two main parts: a thread that gets the user's messages and another thread that pings the server occasionally for message updates. The first thread is just a wrapper around a BufferedReader that passes it over to a packet encoder. The second thread pings the server every second or so to ask if any new messages have arrived.

## Server
The server has a list of recent messages, which is updated anytime a message is recieved from a client. If a message update is requested, it bundles up the messages and sends them to the client.

## Encoder
The encoder uses netty.io ByteBufs to write create packets. Generally, the packets follow the following pattern:
`[packet id] [length of item] [item] [length of item] [item] ...`

## Decoder
The decoder verifies the packet id and then decodes the packets by reading lengths and items until the ByteBuf is consumed.
