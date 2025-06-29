package dev.firstdark.rpc.connection.unix;

import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;

import java.io.File;
import java.io.IOException;

public class JUnixBackend implements IUnixBackend {

    private AFUNIXSocket socket;

    /**
     * Open a connection with the backend
     *
     * @param path The Pipe (or socket) path to open
     * @throws IOException Thrown when a connection error occurs
     */
    @Override
    public void openPipe(String path) throws IOException {
        this.socket = AFUNIXSocket.newInstance();
        this.socket.connect(AFUNIXSocketAddress.of(new File(path)));
    }

    /**
     * Close the backend connection, closing the pipe
     *
     * @throws IOException Thrown when an error occurs
     */
    @Override
    public void closePipe() throws IOException {
        if (socket != null)
            socket.close();
    }

    /**
     * Write data to the backend connection
     *
     * @param bytes The bytes to be written
     * @throws IOException Thrown when an exception occurs
     */
    @Override
    public void write(byte[] bytes) throws IOException {
        if (socket == null || !socket.isConnected())
            return;

        socket.getOutputStream().write(bytes);
    }

    /**
     * Get the amount of data available for reading from the backend
     *
     * @return This will always return 0 when using NIO
     * @throws IOException Thrown when an error occurs
     */
    @Override
    public int getAvailable() throws IOException {
        if (socket == null || !socket.isConnected())
            return -1;

        return socket.getInputStream().available();
    }

    /**
     * Read data from the backend
     *
     * @param bytes The byte buffer to read to
     * @return The total number of bytes read
     * @throws IOException Thrown when an error occurs
     */
    @Override
    public int read(byte[] bytes) throws IOException {
        if (socket == null || !socket.isConnected())
            return -1;

        return socket.getInputStream().read(bytes);
    }

    /**
     * Check if the backend implementation is connected or not
     *
     * @return True if connected
     */
    @Override
    public boolean isConnected() {
        return socket != null && socket.isConnected();
    }
}
