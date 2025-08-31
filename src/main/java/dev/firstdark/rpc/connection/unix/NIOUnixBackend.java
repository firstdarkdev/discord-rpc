package dev.firstdark.rpc.connection.unix;

import java.io.IOException;
//#if modernjava
//$$ import java.net.UnixDomainSocketAddress;
//#endif
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class NIOUnixBackend implements IUnixBackend {

    private SocketChannel channel;

    /**
     * Open a connection with the backend
     *
     * @param path The Pipe (or socket) path to open
     * @throws IOException Thrown when a connection error occurs
     */
    @Override
    public void openPipe(String path) throws IOException {
        //#if modernjava
        //$$ this.channel = SocketChannel.open(UnixDomainSocketAddress.of(path));
        //#else
        this.channel = null;
        //#endif
    }

    /**
     * Close the backend connection, closing the pipe
     *
     * @throws IOException Thrown when an error occurs
     */
    @Override
    public void closePipe() throws IOException {
        if (this.channel == null)
            return;

        this.channel.close();
    }

    /**
     * Write data to the backend connection
     *
     * @param bytes The bytes to be written
     * @throws IOException Thrown when an exception occurs
     */
    @Override
    public void write(byte[] bytes) throws IOException {
        if (this.channel == null || !this.channel.isConnected())
            return;

        this.channel.write(ByteBuffer.wrap(bytes));
    }

    /**
     * Get the amount of data available for reading from the backend
     *
     * @return This will always return 0 when using NIO
     * @throws IOException Thrown when an error occurs
     */
    @Override
    public int getAvailable() throws IOException {
        if (this.channel == null || !this.channel.isConnected())
            return -1;

        Selector selector = Selector.open();
        try {
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_READ);
            return selector.selectNow();
        } finally {
            selector.close();
            channel.configureBlocking(true);
        }
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
        if (this.channel == null || !this.channel.isConnected())
            return -1;

        return this.channel.read(ByteBuffer.wrap(bytes));
    }

    /**
     * Check if the backend implementation is connected or not
     *
     * @return True if connected
     */
    @Override
    public boolean isConnected() {
        return this.channel != null && this.channel.isConnected();
    }
}
