## Discord RPC

---

A pure Java implementation of the now defunct [discord-rpc sdk](https://github.com/discord/discord-rpc).

This library has the same features as the original DLL versions, but with notable changes:

- Pure java implementation, without the need for JNA or JNI (except for older java versions, where junixsocket uses it)
- Apple Silicon support


### Features

---

- Setting Rich Presence
- Listen for Join, Spectate and Join-Requests
- Specify the Activity type (Watching, Playing, Competing, etc)
- Automatic reconnects when switching user accounts
- 100% pure java
- Supports Java 8-21
- Specifically designed for use in Minecraft mods
- Automatic reconnecting and user switching support

### Getting Started

---

![badge](https://maven.firstdark.dev/api/badge/latest/releases/dev/firstdark/discordrpc/discord-rpc?color=40c14a&name=Latest)

First, add our maven repository

```groovy
maven {
    name "FDD Maven"
    url "https://maven.firstdark.dev/releases"
}
```

Next, add the required dependency:

```groovy
// For Java 16+
implementation "dev.firstdark.discordrpc:discord-rpc:VERSION"

// For Java older than 16
// implementation "dev.firstdark.discordrpc:discord-rpc:VERSION:legacy"
```

Note:

`legacy` uses JUnixSockets to handle the discord communication. The Java 16+ jar uses java NIO to communicate with the sockets

### Example

---

```java
DiscordRpc rpc = new DiscordRpc();
rpc.setDebugMode(true);

RPCEventHandler handler = new RPCEventHandler() {
    @Override
    public void ready(User user) {
        System.out.println("Ready");
        DiscordRichPresence presence = DiscordRichPresence.builder()
                .details("Hello World")
                .largeImageKey("gear")
                .activityType(ActivityType.WATCHING)
                .button(DiscordRichPresence.RPCButton.of("Test", "https://google.com"))
                .build();

        rpc.updatePresence(presence);
        System.out.println(user.getUsername());
    }

    @Override
    public void disconnected(ErrorCode errorCode, String message) {
        System.out.println("Disconnected " + errorCode + " - " + message);
    }

    @Override
    public void errored(ErrorCode errorCode, String message) {
        System.out.println("Errored " + errorCode + " - " + message);
    }
};

rpc.init("1000773209924317265", handler, false);
```

---

This library and code is licensed under the MIT license, same as the original discord SDK.

If you need more help, visit our [discord](https://discord.firstdark.dev)