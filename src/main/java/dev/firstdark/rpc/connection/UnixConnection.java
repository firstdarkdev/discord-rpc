package dev.firstdark.rpc.connection;

import dev.firstdark.rpc.DiscordRpc;
import dev.firstdark.rpc.exceptions.NoDiscordClientException;
import jnr.unixsocket.UnixSocketAddress;
import jnr.unixsocket.UnixSocketChannel;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author HypherionSA
 * Linux/MacOs IPC Client implementation
 */
class UnixConnection extends BaseConnection {

    // Linux/Mac uses sockets to communicate with Discord
    private UnixSocketChannel unixSocket;
    private boolean isOpened;

    /**
     * Create a new instance of a UNIX IPC pipe
     *
     * @param rpc The initialized {@link DiscordRpc} client
     */
    UnixConnection(DiscordRpc rpc) {
        super(rpc);
        this.unixSocket = null;
        this.isOpened = false;
    }

    /**
     * Check if the IPC connection is open and ready to be used
     *
     * @return True if ready
     */
    @Override
    boolean isOpen() {
        return isOpened;
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

        for (int i = 0; i < 10; i++) {
            try {
                UnixSocketAddress address = new UnixSocketAddress(String.format(pipeName, i));
                this.unixSocket = UnixSocketChannel.open(address);
                this.unixSocket.finishConnect();
                this.isOpened = true;
                getRpc().printDebug("Connected to IPC pipe %s", String.format(pipeName, i));
                return true;
            } catch (Exception e) {
                getRpc().printDebug("Failed to connect to pipe %s", e);
            }
        }

        throw new NoDiscordClientException();
    }

    /**
     * Close the open pipe connection, not allowing any new communications
     */
    @Override
    void close() {
        if (!this.isOpened)
            return;

        try {
            this.unixSocket.close();
        } catch (IOException e) {
            getRpc().printDebug("Failed to close connection", e);
        }

        this.unixSocket = null;
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
        if (!this.isOpened)
            return false;

        try {
            this.unixSocket.write(ByteBuffer.wrap(bytes));
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

        if (!isOpened)
            return false;

        try {
            if (!wait) {
                long available = this.unixSocket.socket().getInputStream().available();

                if (available < length)
                    return false;
            }

            ByteBuffer buffer = ByteBuffer.allocate(length);
            int read = this.unixSocket.read(buffer);
            if (read != length)
                throw new IOException("Read less data than supplied. Expected: " + length + ". Got: " + read);

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
}
