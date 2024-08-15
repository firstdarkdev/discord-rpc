package dev.firstdark.rpc.connection;

import dev.firstdark.rpc.DiscordRpc;
import dev.firstdark.rpc.enums.OSType;
import dev.firstdark.rpc.exceptions.NoDiscordClientException;
import dev.firstdark.rpc.exceptions.UnsupportedOsType;
import dev.firstdark.rpc.utils.OSDetector;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author HypherionSA
 * Base RPC connection class. Specific implementations is handled by the OS specific classes
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseConnection {

    private final DiscordRpc rpc;

    /**
     * Create a new instance of an RPC Pipe
     *
     * @param rpc The initialized {@link DiscordRpc} client
     * @return The Connection wrapper of the detected os
     * @throws UnsupportedOsType An unsupported OS was detected
     */
    static BaseConnection createConnection(DiscordRpc rpc) throws UnsupportedOsType {
        OSType osType = OSDetector.INSTANCE.detectOs();

        if (osType.isWindows())
            return new WindowsConnection(rpc);

        if (osType.isLinux())
            return new UnixConnection(rpc);

        if (osType.isMac())
            return new MacOsConnection(rpc);

        throw new UnsupportedOsType(osType);
    }

    /**
     * Close and destroy and existing IPC connection
     *
     * @param connection The connection to close and destroy
     */
    static void destroyConnection(BaseConnection connection) {
        if (connection == null)
            return;

        connection.close();
    }

    /**
     * Check if the current IPC pipe is still open and ready
     *
     * @return True if open
     */
    abstract boolean isOpen();

    /**
     * Open, or try to open a new IPC connection
     *
     * @return True if opened
     */
    abstract boolean open() throws NoDiscordClientException;

    /**
     * Close the current IPC connection.
     * When a connection is closed, it will need to be reopened
     */
    abstract void close();

    /**
     * Convert data to bytes for sending over the IPC pipe
     *
     * @param bytes The ByteArray of data to send
     * @return True on success
     */
    abstract boolean write(byte[] bytes);

    /**
     * Read a packet received by the IPC socket/pipe
     *
     * @param bytes The bytes received
     * @param length The length of the data to be read
     * @param wait Wait for the data to be fully available
     * @return True on success
     */
    abstract boolean read(byte[] bytes, int length, boolean wait);

    /**
     * Register an application as a Discord application
     *
     * @param applicationId The Discord Application ID
     * @param command The command used to launch the application
     */
    public abstract void register(String applicationId, String command);

    /**
     * Register a steam game with Discord
     *
     * @param applicationId The Discord Application ID
     * @param steamId The ID of the steam game to register
     */
    public abstract void registerSteamGame(String applicationId, String steamId);

}
