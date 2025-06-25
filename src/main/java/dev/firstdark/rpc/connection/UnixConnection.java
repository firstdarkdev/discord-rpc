package dev.firstdark.rpc.connection;

import dev.firstdark.rpc.DiscordRpc;
import dev.firstdark.rpc.connection.unix.IUnixBackend;
//#if modernjava
//$$ import dev.firstdark.rpc.connection.unix.NIOUnixBackend;
//#else
import dev.firstdark.rpc.connection.unix.JUnixBackend;
//#endif
import dev.firstdark.rpc.exceptions.NoDiscordClientException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author HypherionSA
 * Linux/MacOs IPC Client implementation
 */
class UnixConnection extends BaseConnection {

    // Linux/Mac uses sockets to communicate with Discord
    private IUnixBackend unixBackend;
    private boolean isOpened;

    /**
     * Create a new instance of a UNIX IPC pipe
     *
     * @param rpc The initialized {@link DiscordRpc} client
     */
    UnixConnection(DiscordRpc rpc) {
        super(rpc);
        //#if modernjava
        //$$ this.unixBackend = new NIOUnixBackend();
        //#else
        this.unixBackend = new JUnixBackend();
        //#endif
        this.isOpened = false;
    }

    /**
     * Check if the IPC connection is open and ready to be used
     *
     * @return True if ready
     */
    @Override
    boolean isOpen() {
        return this.unixBackend != null && isOpened;
    }

    /**
     * Try to find the first IPC pipe available and connect to it (if any)
     *
     * @return The IPC pipe that is in use
     * @throws NoDiscordClientException Thrown when no valid discord client is found
     */
    @Override
    boolean open() throws NoDiscordClientException {
        String pipeName = this.getTempPath() + "/discord-ipc-%s";

        if (this.isOpen())
            throw new IllegalStateException("Connection is already opened");

        if (this.tryOpenConnection(pipeName))
            return true;

        String nextPath = getAdditionalPaths();

        if (nextPath != null && !nextPath.isEmpty() && this.tryOpenConnection(nextPath + "/discord-ipc-%s"))
            return true;

        throw new NoDiscordClientException();
    }

    /**
     * Helper method to try and open an RPC connection
     *
     * @param pipeName The pipe base path to try and open
     * @return True if opened
     */
    private boolean tryOpenConnection(String pipeName) {
        for (int i = 0; i < 10; i++) {
            try {
                this.unixBackend.openPipe(String.format(pipeName, i));
                this.isOpened = true;
                getRpc().printDebug("Connected to IPC pipe %s", String.format(pipeName, i));
                return true;
            } catch (Exception e) {
                getRpc().printDebug("Failed to connect to pipe %s", String.format(pipeName, i), e);
            }
        }

        return false;
    }

    /**
     * Close the open pipe connection, not allowing any new communications
     */
    @Override
    void close() {
        if (!this.isOpen())
            return;

        try {
            this.unixBackend.closePipe();
        } catch (IOException e) {
            getRpc().printDebug("Failed to close connection", e);
        }

        this.unixBackend = null;
        this.isOpened = false;
    }

    /**
     * Send a packet to the IPC pipe
     *
     * @param bytes The ByteArray of data to send
     * @return True if successful
     */
    @Override
    boolean write(byte[] bytes) {
        if (!this.isOpen())
            return false;

        try {
            this.unixBackend.write(bytes);
            return true;
        } catch (Exception e) {
            getRpc().printDebug("Failed to write packet %s", e);
            return false;
        }
    }

    /**
     * Read a packet from the IPC pipe
     *
     * @param bytes The bytes received
     * @param length The length of the data to be read
     * @param wait Wait for the data to be fully available
     * @return True if successful
     */
    @Override
    boolean read(byte[] bytes, int length, boolean wait) {
        if (bytes == null || bytes.length == 0)
            return bytes != null;

        if (!isOpen())
            return false;

        try {
            if (!wait) {
                long available = this.unixBackend.getAvailable();

                if (available < length)
                    return false;
            }

            byte[] buf = new byte[length];
            int read = this.unixBackend.read(buf);
            if (read != length)
                throw new IOException("Read less data than supplied. Expected: " + length + ". Got: " + read);

            ByteBuffer buffer = ByteBuffer.wrap(buf);
            buffer.rewind();
            buffer.get(bytes, 0, length);
            return true;
        } catch (Exception e) {
            getRpc().getLogger().error("Failed to read packet %s", e);
            this.close();
            return false;
        }
    }

    /**
     * Register an application as a Discord application
     *
     * @param applicationId The Discord Application ID
     * @param command The command used to launch the application
     */
    @Override
    public void register(String applicationId, String command) {
        String home = System.getenv("HOME");

        if (home == null)
            throw new RuntimeException("Unable to find user HOME directory");

        if (command == null) {
            try {
                command = Files.readSymbolicLink(Paths.get("/proc/self/exe")).toString();
            } catch (Exception ex) {
                throw new RuntimeException("Unable to get current exe path from /proc/self/exe", ex);
            }
        }

        String desktopFile =
                "[Desktop Entry]\n" +
                        "Name=Game " + applicationId + "\n" +
                        "Exec=" + command + " %%u\n" +
                        "Type=Application\n" +
                        "NoDisplay=true\n" +
                        "Categories=Discord;Games;\n" +
                        "MimeType=x-scheme-handler/discord-" + applicationId + ";\n";

        String desktopFileName = "/discord-" + applicationId + ".desktop";
        String desktopFilePath = home + "/.local";

        if (!this.mkdir(desktopFilePath))
            throw new RuntimeException("Failed to create directory '" + desktopFilePath + "'");

        desktopFilePath += "/share";

        if (!this.mkdir(desktopFilePath))
            throw new RuntimeException("Failed to create directory '" + desktopFilePath + "'");

        desktopFilePath += "/applications";

        if (!this.mkdir(desktopFilePath))
            throw new RuntimeException("Failed to create directory '" + desktopFilePath + "'");

        desktopFilePath += desktopFileName;

        try (FileWriter fileWriter = new FileWriter(desktopFilePath)) {
            fileWriter.write(desktopFile);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to write desktop info into '" + desktopFilePath + "'");
        }

        String xdgMimeCommand = "xdg-mime default discord-" + applicationId + ".desktop x-scheme-handler/discord-" + applicationId;

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(xdgMimeCommand.split(" "));
            processBuilder.environment();
            int result = processBuilder.start().waitFor();
            if (result < 0)
                throw new Exception("xdg-mime returned " + result);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to register mime handler", ex);
        }
    }

    /**
     * Register a steam game with Discord
     *
     * @param applicationId The Discord Application ID
     * @param steamId The ID of the steam game to register
     */
    @Override
    public void registerSteamGame(String applicationId, String steamId) {
        this.register(applicationId, "xdg-open steam://rungameid/" + steamId);
    }

    /**
     * Get a list of potential temporary paths used by the Operating system
     *
     * @return The first path that exists
     */
    private String getTempPath() {
        String temp = System.getenv("XDG_RUNTIME_DIR");
        temp = temp != null ? temp : System.getenv("TMPDIR");
        temp = temp != null ? temp : System.getenv("TMP");
        temp = temp != null ? temp : System.getenv("TEMP");
        temp = temp != null ? temp : "/tmp";
        return temp;
    }

    /**
     * Get additional path for Snap and Flatpak versions of discord
     *
     * @return Path if found, or null if not
     */
    private String getAdditionalPaths() {
        String[] unixFolderPaths = {"/snap.discord", "/app/com.discordapp.Discord"};
        String path = getTempPath();

        for (String s : unixFolderPaths) {
            File f = new File(path, s);
            if (f.exists() && f.isDirectory() && f.list() != null && f.list().length > 0)
                return f.getAbsolutePath();
        }

        return null;
    }

    boolean mkdir(String path) {
        File file = new File(path);
        return file.exists() && file.isDirectory() || file.mkdir();
    }
}
