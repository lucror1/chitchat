# chitchat

chitchat is a simple text-based chat application that utilizes the netty.io libarary

## Mac/Linux
### Installation
Use [git](https://git-scm.com/) to install chitchat.

```bash
git clone https://github.com/pmwarner/chitchat.git
cd chitchat
bash compile
```

### Server Usage

```bash
bash runServer
```
To run the server on a different port (default 8080), use the following instead:
```bash
bash runServer <port>
```

### Client Usage
```bash
bash runClient <username> \[<host> <port>]
```
Host defaults to `localhost` and port defaults to `8080`.

## Windows
Instructions will (probably) be added at a later date. Until then, either figure it out yourself or use the Windows Subsystem for Linux.

## License
[Apache 2.0 License](https://github.com/pmwarner/chitchat/blob/main/LICENSE)
