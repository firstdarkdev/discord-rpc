package dev.firstdark.rpc.connection.unix;

import java.io.IOException;

/***
 * @author HypherionSA
 * Helper interface to implement "backend" support for Unix systems.
 * Reason: Forge and NeoForge has a stupid SecureJarHandler system,
 * that crashes whenever mods use the same packages, without relocation. Since JUnix cannot
 * be relocated, we default to built in JAVA 16 NIO system, when the game is using Java 16+.
 */
public interface IUnixBackend {

    /**
     * Open a connection with the backend
     *
     * @param path The Pipe (or socket) path to open
     * @throws IOException Thrown when a connection error occurs
     */
    void openPipe(String path) throws IOException;

    /**
     * Close the backend connection, closing the pipe
     *
     * @throws IOException Thrown when an error occurs
     */
    void closePipe() throws IOException;

    /**
     * Write data to the backend connection
     *
     * @param bytes The bytes to be written
     * @throws IOException Thrown when an exception occurs
     */
    void write(byte[] bytes) throws IOException;

    /**
     * Get the amount of data available for reading from the backend
     *
     * @return This will always return 0 when using NIO
     * @throws IOException Thrown when an error occurs
     */
    int getAvailable() throws IOException;

    /**
     * Read data from the backend
     *
     * @param bytes The byte buffer to read to
     * @return The total number of bytes read
     * @throws IOException Thrown when an error occurs
     */
    int read(byte[] bytes) throws IOException;

    /**
     * Check if the backend implementation is connected or not
     *
     * @return True if connected
     */
    boolean isConnected();

}
