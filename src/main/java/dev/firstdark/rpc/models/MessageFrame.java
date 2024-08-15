package dev.firstdark.rpc.models;

import dev.firstdark.rpc.enums.OpCode;
import lombok.Getter;
import lombok.Setter;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * @author HypherionSA
 * Represents an RPC packet in its byte/string form
 * // TODO Clean this shit up
 */
@Getter
public class MessageFrame {

    // Message and header buffers
    byte[] headerBuffer;
    byte[] messageBuffer;

    // Packet contents
    @Setter
    private OpCode opCode;
    private int length;
    private String message;

    /**
     * Create a new, empty message packet
     */
    public MessageFrame() {
        this.headerBuffer = new byte[8];
        this.messageBuffer = new byte[65535 - this.headerBuffer.length];
    }

    /**
     * Create a new message packet from existing data
     *
     * @param code The {@link OpCode} (type) of the packet
     * @param message The message (usually JSON string) to be sent
     */
    public MessageFrame(OpCode code, String message) {
        super();
        this.opCode = code;
        this.message = message;
    }

    /**
     * Retrieve the header from the RPC packet
     *
     * @return True if the header was processed
     */
    public boolean parseHeader() {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(this.headerBuffer);
            this.opCode = OpCode.values()[this.readInt(inputStream)];
            this.length = this.readInt(inputStream);
            return true;
        } catch (IOException ex) {
            System.out.println("Failed to parse header: " + ex.getMessage());
            return false;
        }
    }

    /**
     * Helper method to extract INT values from the packet bytes
     *
     * @param stream The packet stream that is being read
     * @return The int value that was extracted
     * @throws IOException Thrown when a parsing error occurs
     */
    private int readInt(InputStream stream) throws IOException {
        int ch1 = stream.read();
        int ch2 = stream.read();
        int ch3 = stream.read();
        int ch4 = stream.read();

        if ((ch1 | ch2 | ch3 | ch4) < 0)
            throw new EOFException();

        return ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + ch1);
    }

    /**
     * Extract the message content from the packet data
     *
     * @return True if the message was extracted
     */
    public boolean parseMessage() {
        this.message = new String(Arrays.copyOfRange(this.messageBuffer, 0, this.length), StandardCharsets.UTF_8);
        return true;
    }

    /**
     * Convert the packet into a {@link ByteBuffer} so that it can be sent to the RPC
     *
     * @return The constructed {@link ByteBuffer} ready for sending
     */
    public ByteBuffer write() {
        byte[] d = message.getBytes(StandardCharsets.UTF_8);

        ByteBuffer writeStream = ByteBuffer.allocate(d.length + 8);
        writeStream.putInt(Integer.reverseBytes(opCode.ordinal()));
        writeStream.putInt(Integer.reverseBytes(d.length));
        writeStream.put(d);
        writeStream.rewind();

        return writeStream;
    }

}
