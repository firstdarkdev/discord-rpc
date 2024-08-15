package dev.firstdark.rpc.connection;

import dev.firstdark.rpc.DiscordRpc;
import dev.firstdark.rpc.exceptions.NoDiscordClientException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * @author HypherionSA
 * Windows IPC Client implementation
 */
class WindowsConnection extends BaseConnection {

    // Windows uses a random file, instead of sockets
    private RandomAccessFile pipe;
    private boolean opened;

    /**
     * Create a new instance of a Windows IPC pipe
     *
     * @param rpc The initialized {@link DiscordRpc} client
     */
    WindowsConnection(DiscordRpc rpc) {
        super(rpc);
        this.pipe = null;
        this.opened = false;
    }

    /**
     * Check if the IPC connection is open and ready to be used
     *
     * @return True if ready
     */
    @Override
    boolean isOpen() {
        return this.opened;
    }

    /**
     * Try to find the first IPC pipe available and connect to it (if any)
     *
     * @return The IPC pipe that is in use
     * @throws NoDiscordClientException Thrown when no valid discord client is found
     */
    @Override
    boolean open() throws NoDiscordClientException {
        String pipeName = "\\\\?\\pipe\\discord-ipc-%s";

        if (this.isOpen())
            throw new IllegalStateException("Connection is already opened");

        for (int i = 0; i < 10; i++) {
            try {
                this.pipe = new RandomAccessFile(String.format(pipeName, i), "rw");
                this.opened = true;
                getRpc().printDebug("Connected to IPC Pipe %s", String.format(pipeName, i));
                return true;
            } catch (FileNotFoundException e) {
                getRpc().printDebug("Failed to connect to pipe %s", e);
            } catch (SecurityException sec) {
                getRpc().getLogger().error("Failed to open RPC Connection, with error Access Denied. Is Discord running in Administrator mode?");
            }
        }

        throw new NoDiscordClientException();
    }

    /**
     * Close the open pipe connection, not allowing any new communications
     */
    @Override
    void close() {
        if (!this.isOpen())
            return;

        try {
            this.pipe.close();
        } catch (Exception e) {
            getRpc().printDebug("Failed to close pipe %s", e);
        }

        this.pipe = null;
        this.opened = false;

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
            this.pipe.write(bytes);
            return true;
        } catch (Exception ignored) {
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

        if (!this.isOpen())
            return false;

        try {
            if (!wait) {
                long available = this.pipe.length() - this.pipe.getFilePointer();
                if (available < length)
                    return false;
            }

            int read = this.pipe.read(bytes, 0, length);

            if (read != length)
                throw new IOException("Read less data than supplied. Expected: " + length + ". Got: " + read);

            return true;

        } catch (IOException e) {
            getRpc().printDebug("Failed to read packet %s", e);
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
        // TODO Implement Register
    }

    /**
     * Register a steam game with Discord
     *
     * @param applicationId The Discord Application ID
     * @param steamId The ID of the steam game to register
     */
    @Override
    public void registerSteamGame(String applicationId, String steamId) {
        // TODO Implement Steam Register
    }
}
