**1.0.6**

- [BUG] Don't exclude needed metadata from META-INF on legacy jar

**1.0.5**

- [BUG] Fix legacy jar requiring java 16 to work

**1.0.4**

- [FEAT] Add support for new RPC activity details
    - Added support for changing the RPC name on the presence itself
    - Added support for LargeImage urls
    - Added support for SmallImage urls
    - Added support for Details url
    - Added support for State url
    - Added support for StatusDisplayType

**1.0.3**

- [BUG] Fix resource leaks on Unix platforms - rageandengage

**1.0.2**

- [BUG] Fix RPC not working on Windows with Java versions newer than 22

**1.0.1**

- [BUG] Fixed Windows Access Denied errors not being caught
- [BUG] Fixed Snap/FlatPak discord locations not being scanned on linux
- [BUG] Fixed Linux Backend not detecting user switching (Modern Jave)
- [BUG] Fixed Linux Backend ignoring a lot of Discord events (Modern Java)
- [BUG] Fixed Auto Reconnect not working
- [FEAT] Added a Dummy RPCEventHandler class to remove the need to implement DiscordEventHandler

**1.0.0.**

- Initial Release