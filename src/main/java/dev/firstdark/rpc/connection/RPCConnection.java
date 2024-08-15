package dev.firstdark.rpc.connection;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.firstdark.rpc.DiscordRpc;
import dev.firstdark.rpc.enums.ErrorCode;
import dev.firstdark.rpc.enums.OpCode;
import dev.firstdark.rpc.enums.RPCState;
import dev.firstdark.rpc.exceptions.NoDiscordClientException;
import dev.firstdark.rpc.exceptions.UnsupportedOsType;
import dev.firstdark.rpc.handlers.Callbacks;
import dev.firstdark.rpc.models.MessageFrame;
import dev.firstdark.rpc.models.User;
import lombok.Getter;
import lombok.Setter;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author HypherionSA
 * Main RPC connection controller class.
 * This handles opening and closing connections, as well as serializing data and creating the JSON
 * structures to be sent to and from the IPC pipe
 */
public class RPCConnection {

    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    /**
     * The RPC OS backend currently in use
     */
    @Getter
    private final BaseConnection baseConnection;

    /**
     * Callback that is called when the RPC connection is opened and ready
     */
    @Setter
    private Callbacks.Connected connectedCallback;

    /**
     * Callback that is called when the RPC connection is closed
     */
    @Setter
    private Callbacks.Disconnected disconnectedCallback;

    private final String appId;
    private ErrorCode lastErrorCode;
    private String lastErrorMessage;
    private RPCState state;
    private final Lock writeLock;

    /**
     * Internal method, to set up the RPC api and default values.
     * Use {@link RPCConnection#create(String, DiscordRpc)} to create a new instance
     *
     * @param applicationId The Discord Application ID
     * @param rpc The initialized {@link DiscordRpc} client
     * @throws UnsupportedOsType Thrown when the current OS is not supported
     */
    private RPCConnection(String applicationId, DiscordRpc rpc) throws UnsupportedOsType {
        this.baseConnection = BaseConnection.createConnection(rpc);
        this.state = RPCState.DISCONNECTED;

        this.connectedCallback = null;
        this.disconnectedCallback = null;

        this.appId = applicationId;
        this.lastErrorCode = ErrorCode.SUCCESS;
        this.lastErrorMessage = null;

        this.writeLock = new ReentrantLock();
    }

    /**
     * Create a new instance of the RPC Connection Controller
     *
     * @param applicationId The Discord Application ID
     * @param rpc The initialized {@link DiscordRpc} client
     * @return A new initialized instance of the RPC Connection controller
     * @throws UnsupportedOsType Thrown when the current OS is not supported
     */
    public static RPCConnection create(String applicationId, DiscordRpc rpc) throws UnsupportedOsType {
        return new RPCConnection(applicationId, rpc);
    }

    /**
     * Close an RPC connection
     *
     * @param connection The connection to be closed
     */
    public static void destroy(RPCConnection connection) {
        connection.close();
    }

    /**
     * Check if the RPC connection is connected and ready to be used
     *
     * @return True if connected
     */
    public boolean isOpen() {
        return this.state == RPCState.CONNECTED && baseConnection.isOpen();
    }

    /**
     * Construct the JSON data for the RPC handshake packet
     *
     * @return The JSON string with needed data
     */
    private String writeHandshake() {
        JsonObject data = new JsonObject();
        data.add("v", new JsonPrimitive(1));
        data.add("client_id", new JsonPrimitive(this.appId));

        return data.toString();
    }

    /**
     * Try to open an RPC connection
     *
     * @throws NoDiscordClientException No valid discord install was found
     */
    public void open() throws NoDiscordClientException {
        if (this.state == RPCState.CONNECTED)
            return;

        if (this.state == RPCState.DISCONNECTED && !this.baseConnection.open())
            return;

        // We received the handshake packet from discord, so we need to process it
        if (this.state == RPCState.SENT_HANDSHAKE) {
            JsonObject data = new JsonObject();

            if (this.read(data, true)) {
                String cmd = data.has("cmd") && !data.get("cmd").isJsonNull() ? data.get("cmd").getAsString() : null;
                String evt = data.has("evt") && !data.get("evt").isJsonNull() ? data.get("evt").getAsString() : null;

                // Check if the RPC is ready and dispatch the ready event
                if (cmd != null && evt != null && cmd.equals("DISPATCH") && evt.equals("READY")) {
                    this.state = RPCState.CONNECTED;

                    // Construct the user class from the returned data
                    JsonObject userData = data.get("data").getAsJsonObject().get("user").getAsJsonObject();
                    User user = GSON.fromJson(userData, User.class);

                    if (connectedCallback != null)
                        this.connectedCallback.accept(user);
                }
            }

            return;
        }

        // Connection is not yet open, so we send our handshake packet
        MessageFrame messageFrame = new MessageFrame(OpCode.HANDSHAKE, this.writeHandshake());
        boolean success;

        this.writeLock.lock();

        try {
            success = this.baseConnection.write(messageFrame.write().array());
        } finally {
            this.writeLock.unlock();
        }

        if (success)
            this.state = RPCState.SENT_HANDSHAKE;
        else
            this.close();
    }

    /**
     * Shut down the active RPC connection
     */
    private void close() {
        if (this.disconnectedCallback != null && (this.state == RPCState.CONNECTED || this.state == RPCState.SENT_HANDSHAKE))
            this.disconnectedCallback.accept(this.lastErrorCode, this.lastErrorMessage);

        BaseConnection.destroyConnection(this.baseConnection);
        this.state = RPCState.DISCONNECTED;
    }

    /**
     * Send a data packet to the IPC pipe
     *
     * @param bytes The ByteArray to be sent
     * @return True if sent
     */
    public boolean write(byte[] bytes) {
        MessageFrame messageFrame = new MessageFrame(OpCode.FRAME, new String(bytes, StandardCharsets.UTF_8));

        boolean success;
        this.writeLock.lock();

        try {
            success = this.baseConnection.write(messageFrame.write().array());
        } finally {
            this.writeLock.unlock();
        }

        if (!success) {
            this.close();
            return false;
        }

        return true;
    }

    /**
     * Convert a data packet to a JSON object, for later use
     *
     * @param jsonObject The JSON object that will be filled with data
     * @param wait Wait for the packet to be fully received before processing
     * @return True if successful
     */
    public boolean read(JsonObject jsonObject, boolean wait) {
        if (this.state != RPCState.CONNECTED && this.state != RPCState.SENT_HANDSHAKE)
            return false;

        MessageFrame messageFrame = new MessageFrame();

        while (true) {
            // Process the OpCode header
            boolean didRead = this.baseConnection.read(messageFrame.getHeaderBuffer(), messageFrame.getHeaderBuffer().length, wait);
            if (!didRead || !messageFrame.parseHeader()) {
                if (!this.baseConnection.isOpen()) {
                    this.lastErrorCode = ErrorCode.PIPE_CLOSED;
                    this.lastErrorMessage = "Pipe Closed";
                    this.close();
                }

                return false;
            }

            // Read the JSON data from the packet
            if (messageFrame.getLength() > 0) {
                didRead = this.baseConnection.read(messageFrame.getMessageBuffer(), messageFrame.getLength(), true);

                if (!didRead || !messageFrame.parseMessage()) {
                    this.lastErrorCode = ErrorCode.READ_CORRUPT;
                    this.lastErrorMessage = "Partial data in frame";
                    this.close();

                    return false;
                }
            }

            // Check what OpCode was sent to us
            switch (messageFrame.getOpCode()) {
                // Connection terminated, so we need to close our client
                case CLOSE:
                    JsonObject object = GSON.fromJson(messageFrame.getMessage(), JsonObject.class);
                    object.entrySet().forEach(entry -> jsonObject.add(entry.getKey(), entry.getValue()));

                    int error = object.has("code") && !object.get("code").isJsonNull() ? object.get("code").getAsInt() : 0;
                    this.lastErrorCode = error >= ErrorCode.values().length ? ErrorCode.UNKNOWN : ErrorCode.values()[error];
                    this.lastErrorMessage = object.has("message") && !object.get("message").isJsonNull() ? object.get("message").getAsString() : "";
                    this.close();
                    return false;

                // Generic update update
                case FRAME:
                    object = GSON.fromJson(messageFrame.getMessage(), JsonObject.class);
                    object.entrySet().forEach(entry -> jsonObject.add(entry.getKey(), entry.getValue()));
                    return true;

                case PING:
                    messageFrame.setOpCode(OpCode.PONG);

                    boolean success;
                    this.writeLock.lock();

                    try {
                        success = this.baseConnection.write(messageFrame.write().array());
                    } finally {
                        this.writeLock.unlock();
                    }

                    if (success)
                        this.close();

                    break;

                case PONG:
                    break;

                case HANDSHAKE:
                default:
                    this.lastErrorCode = ErrorCode.READ_CORRUPT;
                    this.lastErrorMessage = "Bad IPC Frame";
                    this.close();
                    return false;
            }
        }
    }
}
